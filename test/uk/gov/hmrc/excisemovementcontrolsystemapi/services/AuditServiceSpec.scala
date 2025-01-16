/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.NonEmptySeq
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsObject
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeXmlParsers
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.{GetMovementsAuditInfo, GetMovementsParametersAuditInfo, GetMovementsResponseAuditInfo, GetSpecificMovementAuditInfo, GetSpecificMovementRequestAuditInfo, MessageSubmittedDetails, UserDetails}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE704Message, IE815Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, NodeSeq}

class AuditServiceSpec extends PlaySpec with TestXml with BeforeAndAfterEach with FakeXmlParsers {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier    = HeaderCarrier()

  val auditConnector: AuditConnector = mock[AuditConnector]
  val appConfig: AppConfig           = mock[AppConfig]

  val testMovement: Movement = Movement("id", None, "lrn", "consignorId", None, None, Instant.now, Seq.empty[Message])
  val testErns: Set[String]  = Set("123", "456")

  private def createRequest(headers: Seq[(String, String)], body: Elem = IE815): ParsedXmlRequest[NodeSeq] =
    ParsedXmlRequest[NodeSeq](
      EnrolmentRequest[NodeSeq](
        FakeRequest()
          .withHeaders(FakeHeaders(headers))
          .withBody(body),
        testErns,
        UserDetails("", "")
      ),
      IE815Message.createFromXml(body),
      testErns,
      UserDetails("", "")
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(auditConnector, appConfig)

    when(appConfig.newAuditingEnabled).thenReturn(true)
    when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(AuditResult.Success))
  }

  "auditMessage" should {

    "silently returns right on error with log" in {
      when(auditConnector.sendExtendedEvent(any)(any, any))
        .thenReturn(Future.successful(AuditResult.Failure("test", None)))

      val service = new AuditService(auditConnector, appConfig)
      val result  = service.auditMessage(IE815Message.createFromXml(IE815))

      await(result.value) equals Right(())
    }

    "return Right(())) on success" in {

      val service = new AuditService(auditConnector, appConfig)
      val result  = service.auditMessage(IE815Message.createFromXml(IE815))

      await(result.value) equals Right(())
    }
  }

  val service = new AuditService(auditConnector, appConfig)

  "messageSubmittedNoMovement" should {

    val request = createRequest(Seq.empty[(String, String)])

    "post an event if newAuditing feature switch is true" in {

      val message = IE815Message.createFromXml(IE815)

      val expectedMessageSubmittedDetails = MessageSubmittedDetails(
        "IE815",
        "DraftMovement",
        "LRNQA20230909022221",
        None,
        None,
        "GBWK002281023",
        Some("GBWKQOZ8OVLYR"),
        true,
        "6de1b822562c43fb9220d236e487c920",
        Some("correlationId"),
        UserDetails("", ""),
        NonEmptySeq.of("123", "456"),
        message.toJsObject
      )

      service.messageSubmittedNoMovement(message, true, Some("correlationId"), request)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("MessageSubmitted"), eqTo(expectedMessageSubmittedDetails))(eqTo(hc), any, any)
    }

    "post no event if newAuditing feature switch is false" in {

      when(appConfig.newAuditingEnabled).thenReturn(false)

      service.messageSubmittedNoMovement(IE815Message.createFromXml(IE815), true, Some("correlationId"), request)

      verify(auditConnector, times(0)).sendExplicitAudit(any[String], any[JsObject])(any, any)
    }

  }

  "messageSubmitted" should {
    "there is no movement to be audited" should {
      val request = createRequest(Seq.empty[(String, String)])
      "post an event if newAuditing feature switch is true" in {

        val message    = IE704Message.createFromXml(IE704)
        val movementId = "4bf36235-4816-464a-b3f3-71dbd3a30095"

        val movement = Movement(
          movementId,
          Some("boxId"),
          "lrn",
          "consignorId",
          Some("consigneeId"),
          Some("arc1"),
          Instant.now(),
          Seq.empty
        )

        val expectedMessageSubmittedDetails = MessageSubmittedDetails(
          "IE704",
          "Refused",
          "lrn",
          Some("arc1"),
          Some("4bf36235-4816-464a-b3f3-71dbd3a30095"),
          "consignorId",
          Some("consigneeId"),
          true,
          "XI000001",
          Some("correlationId"),
          UserDetails("", ""),
          NonEmptySeq.of("123", "456"),
          message.toJsObject
        )

        //TODO: Need to change this to actual correlationId from header
        service.messageSubmitted(message, movement, true, Some("correlationId"), request)

        verify(auditConnector, times(1))
          .sendExplicitAudit(eqTo("MessageSubmitted"), eqTo(expectedMessageSubmittedDetails))(any, any, any)

      }

      "post no event if newAuditing feature switch is false" in {
        when(appConfig.newAuditingEnabled).thenReturn(false)

        service.messageSubmitted(IE815Message.createFromXml(IE815), testMovement, true, Some(""), request)

        verify(auditConnector, times(0)).sendExtendedEvent(any)(any, any)
      }
    }
  }

  "getInformationForGetMovements" should {
    "post an event when user calls a getMovements" in {
      val request          = GetMovementsParametersAuditInfo(None, None, None, None, None)
      val response         = GetMovementsResponseAuditInfo(5)
      val userDetails      = UserDetails("gatewayId", "groupIdentifier")
      val authExciseNumber = NonEmptySeq("ern1", Seq("ern2", "ern3"))

      val expectedDetails = GetMovementsAuditInfo(
        request = request,
        response = response,
        userDetails = userDetails,
        authExciseNumber = authExciseNumber
      )

      service.getInformationForGetMovements(request, response, userDetails, authExciseNumber)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("GetInformation"), eqTo(expectedDetails))(any, any, any)
    }

  }

  "getInformationForGetSpecificMovement" should {
    "post an event when user calls a GetSpecificMovement" in {
      val uuid             = UUID.randomUUID().toString
      val request          = GetSpecificMovementRequestAuditInfo(uuid)
      val userDetails      = UserDetails("gatewayId", "groupIdentifier")
      val authExciseNumber = NonEmptySeq("ern1", Seq("ern2", "ern3"))

      val expectedDetails = GetSpecificMovementAuditInfo(
        request = request,
        userDetails = userDetails,
        authExciseNumber = authExciseNumber
      )

      service.getInformationForGetSpecificMovement(request, userDetails, authExciseNumber)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("GetInformation"), eqTo(expectedDetails))(any, any, any)
    }
  }
}
