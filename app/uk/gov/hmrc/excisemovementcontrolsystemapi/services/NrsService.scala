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

import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel, CredentialRole}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ValidatedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{IdentityData, NonRepudiationSubmissionAccepted, NrsMetadata, NrsPayload}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.nonRepudiationIdentityRetrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, ErnsMapper, NrsEventIdMapper}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class NrsService @Inject()
(
  authConnector: AuthConnector,
  nrsConnector: NrsConnector,
  dateTimeService: DateTimeService,
  emcsUtils: EmcsUtils,
  ernsMapper: ErnsMapper,
  nrsEventIdMapper: NrsEventIdMapper
)(implicit ec: ExecutionContext) extends Logging {

  def submitNrs(
    request: ValidatedXmlRequest[_],
  )(implicit headerCarrier: HeaderCarrier): Future[Either[Int, NonRepudiationSubmissionAccepted]] = {

    val payload = request.body.toString
    val userHeaderData = request.headersAsMap
    val message = request.message
    val exciseNumber = ernsMapper.getSingleErnFromMessage(message, request.validErns)
    val notableEventId = nrsEventIdMapper.mapMessageToEventId(message)

    (for {
      identityData <- retrieveIdentityData()
      userAuthToken = retrieveUserAuthToken(headerCarrier)
      metaData = NrsMetadata.create(payload, emcsUtils,notableEventId, identityData,dateTimeService.nowUtc.toString,
        userAuthToken, userHeaderData, exciseNumber)
      encodedPayload = emcsUtils.encode(payload)
      nrsPayload = NrsPayload(encodedPayload, metaData)
      retrievedNrsResponse <- nrsConnector.sendToNrs(nrsPayload)
    } yield retrievedNrsResponse)
      .recover {
        case NonFatal(e) =>
          //Todo: Catching the error and not throwing here for the moment. But do we want to throw here?
          logger.error(s"[NrsService] - Error when submitting to Non repudiation system (NRS) with message: ${e.getMessage}", e)
          Left(INTERNAL_SERVER_ERROR)
      }

  }

  private def retrieveIdentityData()(implicit headerCarrier: HeaderCarrier): Future[IdentityData] =
    authConnector.authorise(EmptyPredicate, nonRepudiationIdentityRetrievals).map {
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
        IdentityData(internalId = internalId,
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
    }

  def retrieveUserAuthToken(hc: HeaderCarrier): String =
    hc.authorization match {
      case Some(Authorization(authToken)) => authToken
      case _                              => throw new InternalServerException("No auth token available for NRS")
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
