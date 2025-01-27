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
import play.api.mvc.AnyContent
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.MovementFilter
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeXmlParsers
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.{AuditEventFactory, GetMessagesAuditInfo, GetMessagesRequestAuditInfo, GetMessagesResponseAuditInfo, GetMovementsAuditInfo, GetMovementsParametersAuditInfo, GetMovementsResponseAuditInfo, GetSpecificMessageAuditInfo, GetSpecificMessageRequestAuditInfo, GetSpecificMessageResponseAuditInfo, GetSpecificMovementAuditInfo, GetSpecificMovementRequestAuditInfo, MessageAuditInfo, MessageSubmittedDetails, UserDetails}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{Consignee, IE704Message, IE815Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils
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
  val utils                          = new EmcsUtils
  val factory: AuditEventFactory     = new AuditEventFactory(utils, new IEMessageFactory)

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

  val erns: Seq[String]                              = Seq("ern")
  val userDetails: UserDetails                       = UserDetails("id", "id")
  val enrolmentRequest: EnrolmentRequest[AnyContent] =
    EnrolmentRequest(FakeRequest(), erns.toSet, userDetails)

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

      val service = new AuditService(auditConnector, appConfig, factory)
      val result  = service.auditMessage(IE815Message.createFromXml(IE815))

      await(result.value) equals Right(())
    }

    "return Right(())) on success" in {

      val service = new AuditService(auditConnector, appConfig, factory)
      val result  = service.auditMessage(IE815Message.createFromXml(IE815))

      await(result.value) equals Right(())
    }
  }

  val service = new AuditService(auditConnector, appConfig, factory)

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
        submittedToCore = true,
        "6de1b822562c43fb9220d236e487c920",
        Some("correlationId"),
        UserDetails("", ""),
        NonEmptySeq.of("123", "456"),
        message.toJsObject
      )

      service.messageSubmittedNoMovement(message, submittedToCore = true, Some("correlationId"), request)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("MessageSubmitted"), eqTo(expectedMessageSubmittedDetails))(eqTo(hc), any, any)
    }

    "post no event if newAuditing feature switch is false" in {

      when(appConfig.newAuditingEnabled).thenReturn(false)

      service.messageSubmittedNoMovement(
        IE815Message.createFromXml(IE815),
        submittedToCore = true,
        Some("correlationId"),
        request
      )

      verify(auditConnector, times(0)).sendExplicitAudit(any[String], any[JsObject])(any, any)
    }

  }

  "messageSubmitted" should {
    "there is no movement to be audited" should {
      val request = createRequest(Seq.empty[(String, String)])
      "post an event if newAuditing feature switch is true" in {
        val requestWithIE704 =
          ParsedXmlRequest[NodeSeq](
            EnrolmentRequest[NodeSeq](
              FakeRequest()
                .withHeaders(FakeHeaders(Seq.empty[(String, String)]))
                .withBody(IE704),
              testErns,
              UserDetails("", "")
            ),
            IE704Message.createFromXml(IE704),
            testErns,
            UserDetails("", "")
          )

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
          submittedToCore = true,
          "XI000001",
          Some("correlationId"),
          UserDetails("", ""),
          NonEmptySeq.of("123", "456"),
          message.toJsObject
        )

        //TODO: Need to change this to actual correlationId from header
        service.messageSubmitted(movement, submittedToCore = true, Some("correlationId"), requestWithIE704)

        verify(auditConnector, times(1))
          .sendExplicitAudit(eqTo("MessageSubmitted"), eqTo(expectedMessageSubmittedDetails))(any, any, any)

      }

      "post no event if newAuditing feature switch is false" in {
        when(appConfig.newAuditingEnabled).thenReturn(false)

        service.messageSubmitted(testMovement, submittedToCore = true, Some(""), request)

        verify(auditConnector, times(0)).sendExtendedEvent(any)(any, any)
      }
    }
  }

  "getInformationForGetMovements" should {
    "post an event when user calls a getMovements" in {
      val filter    = MovementFilter(None, None, None, None, None)
      val movements = Seq(testMovement)

      val expectedDetails = GetMovementsAuditInfo(
        request = GetMovementsParametersAuditInfo(None, None, None, None, None),
        response = GetMovementsResponseAuditInfo(1),
        userDetails = userDetails,
        authExciseNumber = NonEmptySeq(enrolmentRequest.erns.head, enrolmentRequest.erns.tail.toSeq)
      )

      service.getInformationForGetMovements(filter, movements, enrolmentRequest)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("GetInformation"), eqTo(expectedDetails))(any, any, any)
    }

  }

  "getInformationForGetSpecificMovement" should {
    "post an event when user calls a GetSpecificMovement" in {
      val movementId = UUID.randomUUID().toString
      val request    = GetSpecificMovementRequestAuditInfo(movementId)

      val expectedDetails = GetSpecificMovementAuditInfo(
        request = request,
        userDetails = userDetails,
        authExciseNumber = NonEmptySeq(enrolmentRequest.erns.head, enrolmentRequest.erns.tail.toSeq)
      )

      service.getInformationForGetSpecificMovement(movementId, enrolmentRequest)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("GetInformation"), eqTo(expectedDetails))(any, any, any)
    }
  }

  "getInformationForGetMessages" should {
    "post an event when user calls a getMessages" in {
      val movementId      = UUID.randomUUID().toString
      val messageId       = UUID.randomUUID().toString
      val messageCreateOn = Instant.now()

      val messages = Seq(
        Message(utils.encode(IE801.toString), "IE801", messageId, erns.head, Set.empty[String], messageCreateOn)
      )
      val movement =
        Movement(movementId, None, "lrn", erns.head, Some("consigneeId"), None, messageCreateOn, messages)

      val request          = GetMessagesRequestAuditInfo(movementId, None, Some(Consignee.name.toLowerCase))
      val messageAuditInfo =
        MessageAuditInfo(
          messageId,
          Some("PORTAL6de1b822562c43fb9220d236e487c920"),
          "IE801",
          "MovementGenerated",
          erns.head,
          messageCreateOn
        )
      val response         = GetMessagesResponseAuditInfo(1, Seq(messageAuditInfo), "lrn", None, erns.head, Some("consigneeId"))
      val authExciseNumber = NonEmptySeq(erns.head, Seq().empty)

      val expectedDetails = GetMessagesAuditInfo(
        request = request,
        response = response,
        userDetails = userDetails,
        authExciseNumber = authExciseNumber
      )

      service.getInformationForGetMessages(messages, movement, None, Some(Consignee.name.toLowerCase), enrolmentRequest)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("GetInformation"), eqTo(expectedDetails))(any, any, any)
    }
  }

  "getInformationForGetSpecificMessage" should {
    "post an event when user calls a getSpecificMessage" in {
      val movementId       = UUID.randomUUID().toString
      val messageId        = UUID.randomUUID().toString
      val encodedMessage   = utils.encode(IE801.toString)
      val message          = Message(encodedMessage, "IE801", messageId, "recipient", Set.empty[String], Instant.now)
      val movement         = Movement(
        movementId,
        None,
        "lrn",
        "consignorId",
        Some("consigneeId"),
        Some("arc"),
        Instant.now,
        Seq(message)
      )
      val request          = GetSpecificMessageRequestAuditInfo(movementId, messageId)
      val response         = GetSpecificMessageResponseAuditInfo(
        Some("PORTAL6de1b822562c43fb9220d236e487c920"),
        "IE801",
        "MovementGenerated",
        "lrn",
        Some("arc"),
        "consignorId",
        Some("consigneeId")
      )
      val authExciseNumber = NonEmptySeq(erns.head, Seq().empty)

      val expectedDetails = GetSpecificMessageAuditInfo(
        request = request,
        response = response,
        userDetails = userDetails,
        authExciseNumber = authExciseNumber
      )

      service.getInformationForGetSpecificMessage(movement, message, enrolmentRequest)

      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("GetInformation"), eqTo(expectedDetails))(any, any, any)

    }
  }

  "messageProcessingSuccess" should {
    "post a MessageProcessingSuccessAuditInfo event when called" in {
      service.messageProcessingSuccess(???)
      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("MessageProcessing"), eqTo(???))(any, any, any)
    }
  }

  "messageProcessingFailure" should {
    "post a MessageProcessingFailureAuditInfo event when called" in {
      service.messageProcessingFailure(???)
      verify(auditConnector, times(1))
        .sendExplicitAudit(eqTo("MessageProcessing"), eqTo(???))(any, any, any)
    }
  }
}
