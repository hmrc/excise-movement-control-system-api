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
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.nonRepudiationIdentityRetrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils, NrsEventIdMapper}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsService @Inject()
(
  override val authConnector: AuthConnector,
  nrsConnector: NrsConnector,
  dateTimeService: DateTimeService,
  emcsUtils: EmcsUtils,
  nrsEventIdMapper: NrsEventIdMapper
)(implicit ec: ExecutionContext) extends AuthorisedFunctions with Logging {

  def submitNrs(
                 request: ParsedXmlRequest[_],
                 authorisedErn: String,
                 correlationId: String
               )(implicit headerCarrier: HeaderCarrier): Future[NonRepudiationSubmission] = {

    val payload = request.body.toString
    val userHeaderData = request.headersAsMap
    val message = request.ieMessage
    val exciseNumber = authorisedErn
    val notableEventId = nrsEventIdMapper.mapMessageToEventId(message)

    (for {
      identityData <- retrieveIdentityData()
      userAuthToken = retrieveUserAuthToken(headerCarrier)
      metaData = NrsMetadata.create(payload, emcsUtils, notableEventId, identityData, dateTimeService.timestamp().toString,
        userAuthToken, userHeaderData, exciseNumber)
      encodedPayload = emcsUtils.encode(payload)
      nrsPayload = NrsPayload(encodedPayload, metaData)
      retrievedNrsResponse <- nrsConnector.sendToNrs(nrsPayload, correlationId)
    } yield retrievedNrsResponse)
      .recover {
        case e: Exception =>
          logger.warn(s"[NrsService] - Error when submitting to Non repudiation system (NRS) with message: ${e.getMessage}", e)
          NonRepudiationSubmissionFailed(INTERNAL_SERVER_ERROR, e.getMessage)
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
        Future.successful(IdentityData(internalId = internalId,
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
        ))
    }

  private def retrieveUserAuthToken(hc: HeaderCarrier): String =
    hc.authorization match {
      case Some(Authorization(authToken)) => authToken
      case _ => throw new InternalServerException("No auth token available for NRS")
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
