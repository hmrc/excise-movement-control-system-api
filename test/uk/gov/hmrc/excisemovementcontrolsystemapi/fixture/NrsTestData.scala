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

package uk.gov.hmrc.excisemovementcontrolsystemapi.fixture

import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialStrength, User}
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ItmpAddress, ItmpName, MdtpInformation, Name}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.NonRepudiationIdentityRetrievals

import java.time.{LocalDate, ZoneId, ZonedDateTime}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.IdentityData

import java.lang.String.format
import java.math.BigInteger
import java.security.MessageDigest.getInstance

trait NrsTestData {

  val testAffinityGroup: AffinityGroup     = AffinityGroup.Organisation
  val testProviderId: String               = "testProviderID"
  val testProviderType: String             = "GovernmentGateway"
  val testCredentials: Credentials         = Credentials(testProviderId, testProviderType)
  val testInternalid                       = "INT-123-456-789"
  val testExternalId                       = "testExternalId"
  val testAgentCode                        = "testAgentCode"
  val testConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L200
  val testSautr                            = "testSautr"
  val testNino                             = "NB686868C"
  val testDate: LocalDate                  = LocalDate.of(2017, 1, 1)
  val testDateTime: ZonedDateTime          = ZonedDateTime.of(2022, 7, 1, 12, 34, 37, 0, ZoneId.of("Z"))
  val testDateTimeString: String           = "2022-07-01T12:34:37Z" // <-- this is an actual date string taken from production

  val testAuthName: Name =
    uk.gov.hmrc.auth.core.retrieve.Name(Some("testFirstName"), Some("testLastName"))

  val testEmail: String                    = "testEmail"
  val testErn: String                      = "GB123456789"
  val testAuthToken: String                = "testAuthToken"
  val testUserHeaders: Map[String, String] = Map("testKey" -> "testValue")
  val testSearchKeys: Map[String, String]  = Map("ern" -> testErn)

  val testAgentInformation: AgentInformation =
    AgentInformation(Some("testAgentId"), Some("testAgentCode"), Some("testAgentFriendlyName"))
  val testGroupIdentifier                    = "testGroupIdentifier"
  val testCredentialRole: User.type          = User
  val testMdtpInformation: MdtpInformation   = MdtpInformation("testDeviceId", "testSessionId")

  val testItmpName: ItmpName =
    ItmpName(Some("testGivenName"), Some("testMiddleName"), Some("testFamilyName"))

  val testItmpAddress: ItmpAddress =
    ItmpAddress(Some("testLine1"), None, None, None, None, Some("testPostcode"), None, None)

  val testCredentialStrength: String = CredentialStrength.strong

  implicit class RetrievalCombiner[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }
  val testAuthRetrievals: NonRepudiationIdentityRetrievals =
    Some(testAffinityGroup) ~
      Some(testInternalid) ~
      Some(testExternalId) ~
      Some(testAgentCode) ~
      Some(testCredentials) ~
      testConfidenceLevel ~
      Some(testNino) ~
      Some(testSautr) ~
      Some(testAuthName) ~
      Some(testEmail) ~
      testAgentInformation ~
      Some(testGroupIdentifier) ~
      Some(testCredentialRole) ~
      Some(testMdtpInformation) ~
      Some(testItmpName) ~
      Some(testItmpAddress) ~
      Some(testCredentialStrength)

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

  val testNrsIdentityData: IdentityData = IdentityData(
    Some(testInternalid),
    Some(testExternalId),
    Some(testAgentCode),
    Some(testCredentials),
    testConfidenceLevel,
    Some(testNino),
    Some(testSautr),
    Some(testAuthName),
    Some(testEmail),
    testAgentInformation,
    Some(testGroupIdentifier),
    Some(testCredentialRole),
    Some(testMdtpInformation),
    Some(testItmpName),
    Some(testItmpAddress),
    Some(testAffinityGroup),
    Some(testCredentialStrength)
  )

  def sha256Hash(text: String): String =
    format(
      "%064x",
      new BigInteger(
        1,
        getInstance("SHA-256")
          .digest(text.getBytes("UTF-8"))
      )
    )
}
