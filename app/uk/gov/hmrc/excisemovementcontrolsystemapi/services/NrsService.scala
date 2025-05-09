/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import play.api.{Configuration, Logging}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.UnexpectedResponseException
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NRSWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.nonRepudiationIdentityRetrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils._
import uk.gov.hmrc.http.HttpErrorFunctions.{is4xx, is5xx}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.lock.{MongoLockRepository, ScheduledLockService}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Failed, PermanentlyFailed}
import uk.gov.hmrc.mongo.workitem.WorkItem

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent._

@Singleton
class NrsService @Inject() (
  override val authConnector: AuthConnector,
  nrsConnectorNew: NrsConnector,
  nrsWorkItemRepository: NRSWorkItemRepository,
  dateTimeService: DateTimeService,
  emcsUtils: EmcsUtils,
  nrsEventIdMapper: NrsEventIdMapper,
  mongoLockRepository: MongoLockRepository,
  timestampSupport: TimestampSupport,
  actorSystem: ActorSystem,
  configuration: Configuration
)(implicit ec: ExecutionContext)
    extends AuthorisedFunctions
    with Logging {

  private val lockId: String                    = "nrs-lock"
  private val nrsThrottleDuration               = configuration.get[FiniteDuration]("microservice.services.nrs.nrs-throttle-duration")
  private val lockServiceTTL                    = configuration.get[FiniteDuration]("microservice.services.nrs.lock-service-ttl")
  private val lockService: ScheduledLockService = ScheduledLockService(
    mongoLockRepository,
    lockId = lockId,
    timestampSupport,
    lockServiceTTL
  )

  def makeWorkItemAndQueue(
    request: ParsedXmlRequest[_],
    authorisedErn: String
  )(implicit headerCarrier: HeaderCarrier): Future[Done] = {

    val payload        = request.body.toString
    val userHeaderData = request.headersAsMap
    val message        = request.ieMessage
    val exciseNumber   = authorisedErn
    val notableEventId = nrsEventIdMapper.mapMessageToEventId(message)

    for {
      identityData  <- retrieveIdentityData()
      userAuthToken  = retrieveUserAuthToken()
      metaData       = NrsMetadata.create(
                         payload,
                         emcsUtils,
                         notableEventId,
                         identityData,
                         dateTimeService.timestamp().toString,
                         userAuthToken,
                         userHeaderData,
                         exciseNumber
                       )
      encodedPayload = emcsUtils.encode(payload)
      _             <- nrsWorkItemRepository.pushNew(NrsSubmissionWorkItem(NrsPayload(encodedPayload, metaData)))
    } yield Done
  }

  def submitNrs(workItem: WorkItem[NrsSubmissionWorkItem])(implicit hc: HeaderCarrier): Future[Done] =
    nrsConnectorNew
      .sendToNrs(workItem.item.payload)
      .flatMap { _ =>
        nrsWorkItemRepository
          .completeAndDelete(workItem.id)
          .map(_ => Done)
      }
      .recoverWith {
        case ex: UnexpectedResponseException if is4xx(ex.status) =>
          logger.error(
            s"NRS call failed permanently with status ${ex.status} - marking workitem ${workItem.id} as PermanentlyFailed",
            ex
          )
          nrsWorkItemRepository
            .complete(workItem.id, PermanentlyFailed)
            .map(_ => Done)
        case ex: UnexpectedResponseException if is5xx(ex.status) =>
          logger.warn(
            s"NRS call failed with status ${ex.status} - marking workitem ${workItem.id} as Failed for retry",
            ex
          )
          nrsWorkItemRepository
            .complete(workItem.id, Failed)
            .map(_ => Done)
        case ex: UnexpectedResponseException                     =>
          logger.error(
            s"NRS call failed permanently with status ${ex.status} - marking workitem ${workItem.id} as PermanentlyFailed",
            ex
          )
          nrsWorkItemRepository
            .complete(workItem.id, PermanentlyFailed)
            .map(_ => Done)
      }

  private def retrieveIdentityData()(implicit hc: HeaderCarrier): Future[IdentityData] =
    authorised().retrieve(nonRepudiationIdentityRetrievals) {
      case affinityGroup ~ internalId ~
          externalId ~ agentCode ~
          credentials ~ confidenceLevel ~
          nino ~ saUtr ~
          name ~
          email ~ agentInfo ~
          groupId ~ credentialRole ~
          mdtpInfo ~ itmpName ~
          itmpAddress ~
          credentialStrength =>
        Future.successful(
          IdentityData(
            internalId = internalId,
            externalId = externalId,
            agentCode = agentCode,
            optionalCredentials = credentials,
            confidenceLevel = confidenceLevel,
            nino = nino,
            saUtr = saUtr,
            optionalName = name,
            email = email,
            agentInformation = agentInfo,
            groupIdentifier = groupId,
            credentialRole = credentialRole,
            mdtpInformation = mdtpInfo,
            optionalItmpName = itmpName,
            optionalItmpAddress = itmpAddress,
            affinityGroup = affinityGroup,
            credentialStrength = credentialStrength
          )
        )
    }

  private def retrieveUserAuthToken()(implicit hc: HeaderCarrier): String =
    hc.authorization match {
      case Some(Authorization(authToken)) => authToken
      case _                              =>
        logger.warn("[NrsService] - No auth token available for NRS")
        throw new InternalServerException("No auth token available for NRS")
    }

  def processAllWithLock()(implicit hc: HeaderCarrier): Future[Done] =
    lockService
      .withLock {
        processAll()
      }
      .map {
        _.getOrElse {
          logger.info(
            s"Could not acquire lock on nrsWorkItemRepository"
          )
          Done
        }
      }

  private def processAll()(implicit hc: HeaderCarrier): Future[Done] =
    processSingleNrs()
      .flatMap {
        case true  => processAll()
        case false => Future.successful(Done)
      }

  def processSingleNrs()(implicit hc: HeaderCarrier): Future[Boolean] = {

    val now = dateTimeService.timestamp()

    nrsWorkItemRepository
      .pullOutstanding(now, now)
      .flatMap {
        case Some(wi) => submitNrsThrottled(wi).map(_ => true)
        case None     => Future.successful(false)
      }
  }

  private def submitNrsThrottled(workItem: WorkItem[NrsSubmissionWorkItem])(implicit hc: HeaderCarrier): Future[Done] =
    takeAtLeastXTime(submitNrs(workItem), nrsThrottleDuration)

  private def takeAtLeastXTime[A](f: => Future[A], duration: FiniteDuration): Future[A] = {
    val promise             = Promise[Unit]()
    lazy val succeedPromise = promise.success(())
    actorSystem.scheduler.scheduleOnce(duration)(succeedPromise)
    for {
      v <- f
      _ <- promise.future
    } yield v
  }

}

object NrsService {

  type NonRepudiationIdentityRetrievals =
    (Option[AffinityGroup] ~ Option[String]
      ~ Option[String] ~ Option[String]
      ~ Option[Credentials] ~ ConfidenceLevel
      ~ Option[String] ~ Option[String]
      ~ Option[Name]
      ~ Option[String] ~ AgentInformation
      ~ Option[String] ~ Option[CredentialRole]
      ~ Option[MdtpInformation] ~ Option[ItmpName]
      ~ Option[ItmpAddress]
      ~ Option[String])

  val nonRepudiationIdentityRetrievals: Retrieval[NonRepudiationIdentityRetrievals] =
    Retrievals.affinityGroup and Retrievals.internalId and
      Retrievals.externalId and Retrievals.agentCode and
      Retrievals.credentials and Retrievals.confidenceLevel and
      Retrievals.nino and Retrievals.saUtr and
      Retrievals.name and
      Retrievals.email and Retrievals.agentInformation and
      Retrievals.groupIdentifier and Retrievals.credentialRole and
      Retrievals.mdtpInformation and Retrievals.itmpName and
      Retrievals.itmpAddress and
      Retrievals.credentialStrength
}
