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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Futures.whenReady
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.Logger
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeXmlParsers
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, NodeSeq}

class AuditServiceSpec extends PlaySpec with TestXml with BeforeAndAfterEach with FakeXmlParsers {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier    = HeaderCarrier()

  val auditConnector = mock[AuditConnector]
  val appConfig      = mock[AppConfig]

  val testMovement = Movement("id", None, "lrn", "consignorId", None, None, Instant.now, Seq.empty[Message])
  val testErns     = Set("123", "456")

  private def createRequest(headers: Seq[(String, String)], body: Elem = IE815): ParsedXmlRequest[NodeSeq] =
    ParsedXmlRequest[NodeSeq](
      EnrolmentRequest[NodeSeq](
        FakeRequest()
          .withHeaders(FakeHeaders(headers))
          .withBody(body),
        testErns,
        ""
      ),
      IE815Message.createFromXml(body),
      testErns,
      ""
    )

  class Harness(auditConnector: AuditConnector, appConfig: AppConfig)
      extends AuditServiceImpl(auditConnector, appConfig) {
    def getLogger(): Logger = logger
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(auditConnector, appConfig)

    when(appConfig.newAuditingEnabled).thenReturn(true)
    when(auditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(AuditResult.Success))
  }
  //TODO: Can we add 'Future.failed' testing?
  "auditMessage" should {

    "silently returns right on error with log" in {
      when(auditConnector.sendExtendedEvent(any)(any, any))
        .thenReturn(Future.successful(AuditResult.Failure("test", None)))

      val sut    = new Harness(auditConnector, appConfig)
      val result = sut.auditMessage(IE815Message.createFromXml(IE815))

      await(result.value) equals Right(())
    }

    "return Right(())) on success" in {

      val sut    = new Harness(auditConnector, appConfig)
      val result = sut.auditMessage(IE815Message.createFromXml(IE815))

      await(result.value) equals Right(())
    }
  }

  "messageSubmitted" should {
    val request = createRequest(Seq.empty[(String, String)])
    "post an event if newAuditing feature switch is true" in {
      val sut    = new Harness(auditConnector, appConfig)
      val result = sut.messageSubmitted(IE815Message.createFromXml(IE815), testMovement, true, "", request)

      await(result.value)
      verify(auditConnector, times(1)).sendExtendedEvent(any)(any, any)
    }

    "silently returns right on error with log" in {
      when(auditConnector.sendExtendedEvent(any)(any, any))
        .thenReturn(Future.successful(AuditResult.Failure("test", None)))

      val sut    = new Harness(auditConnector, appConfig)
      val result = sut.messageSubmitted(IE815Message.createFromXml(IE815), testMovement, true, "", request)

      await(result.value) equals Right(())
    }

    "post no event if newAuditing feature switch is false" in {
      when(appConfig.newAuditingEnabled).thenReturn(false)
      val sut    = new Harness(auditConnector, appConfig)
      val result = sut.messageSubmitted(IE815Message.createFromXml(IE815), testMovement, true, "", request)

      await(result.value)
      verify(auditConnector, times(0)).sendExtendedEvent(any)(any, any)
    }
  }
}
