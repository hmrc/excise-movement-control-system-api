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
import play.api.Logging
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnectorNew
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnectorNew.UnexpectedResponseException
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NRSWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.nonRepudiationIdentityRetrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils, NrsEventIdMapper}
import uk.gov.hmrc.http.HttpErrorFunctions.{is4xx, is5xx}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Failed, PermanentlyFailed, Succeeded}
import uk.gov.hmrc.mongo.workitem.WorkItem

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsServiceNew @Inject() (
  override val authConnector: AuthConnector,
  nrsConnectorNew: NrsConnectorNew,
  nrsWorkItemRepository: NRSWorkItemRepository,
  dateTimeService: DateTimeService,
  emcsUtils: EmcsUtils,
  nrsEventIdMapper: NrsEventIdMapper,
  correlationIdService: CorrelationIdService
)(implicit ec: ExecutionContext)
    extends AuthorisedFunctions
    with Logging {

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
      userAuthToken  = retrieveUserAuthToken(headerCarrier)
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

  def submitNrs(workItem: WorkItem[NrsSubmissionWorkItem])(implicit hc: HeaderCarrier): Future[Done] = {
    //needs to call connector, and handle marking the workitems. IN PROGRESS

    nrsConnectorNew.sendToNrs(workItem.item.payload, correlationIdService.generateCorrelationId())
      .flatMap( _ =>
        nrsWorkItemRepository.complete(workItem.id, Succeeded)
          .map(_ => Done)
      )
      .recoverWith {
        case UnexpectedResponseException(status, _) if is4xx(status) =>
          nrsWorkItemRepository.complete(workItem.id, PermanentlyFailed)
            .map(_ => Done)
        case UnexpectedResponseException(status, _) if is5xx(status) =>
          nrsWorkItemRepository.complete(workItem.id, Failed)
            .map(_ => Done)
      }
  }

  private def retrieveIdentityData()(implicit headerCarrier: HeaderCarrier): Future[IdentityData] =
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

  private def retrieveUserAuthToken(hc: HeaderCarrier): String =
    hc.authorization match {
      case Some(Authorization(authToken)) => authToken
      case _                              =>
        logger.warn("[NrsService] - No auth token available for NRS")
        throw new InternalServerException("No auth token available for NRS")
    }
}
