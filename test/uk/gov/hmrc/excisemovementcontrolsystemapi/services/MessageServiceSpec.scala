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

import generated.NewMessagesDataResponse
import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageReceiptConnector, ShowNewMessagesConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.NewMessagesXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.Future
import scala.xml.Elem

class MessageServiceSpec extends PlaySpec
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneAppPerSuite
  with NewMessagesXml
  with BeforeAndAfterEach {

  private val movementRepository = mock[MovementRepository]
  private val ernRetrievalRepository = mock[ErnRetrievalRepository]
  private val showNewMessagesConnector = mock[ShowNewMessagesConnector]
  private val messageReceiptConnector = mock[MessageReceiptConnector]
  private val dateTimeService = mock[DateTimeService]
  private val messageService = app.injector.instanceOf[MessageService]
  private val ieMessageFactory = app.injector.instanceOf[IEMessageFactory]
  private val utils = new EmcsUtils
  private val now = Instant.now
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MovementRepository].toInstance(movementRepository),
      bind[ErnRetrievalRepository].toInstance(ernRetrievalRepository),
      bind[ShowNewMessagesConnector].toInstance(showNewMessagesConnector),
      bind[MessageReceiptConnector].toInstance(messageReceiptConnector),
      bind[DateTimeService].toInstance(dateTimeService),
    ).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset[Any](
      movementRepository,
      ernRetrievalRepository,
      showNewMessagesConnector,
      messageReceiptConnector,
      dateTimeService
    )
  }

  private def noNewMessagesResponse(ern: String) =
    eisResponse(ern, emptyNewMessageDataXml)

  private def someNewMessagesResponse(ern: String) =
    eisResponse(ern, newMessageXmlWithIE704)

  private def eisResponse(ern: String, xml: Elem) =
    EISConsumptionResponse(
      now,
      ern,
      utils.encode(xml.toString())
    )


  "updateMessages" when {
    "there is a movement but we have never retrieved anything" when {
      "we try to retrieve messages but there are none" should {
        "update last retrieval time for ern" in {
          val ern = "testErn"
          val movement = Movement(None, "LRN", "Consignor", None)
          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.updateMovement(any)).thenReturn(Future.successful(Some(movement)))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(showNewMessagesConnector.get(any)(any)).thenReturn(Future.successful(Right(noNewMessagesResponse(ern))))

          messageService.updateMessages(ern).futureValue

          verify(showNewMessagesConnector).get(eqTo(ern))(any)
          verify(movementRepository, never()).getAllBy(any)
          verify(movementRepository, never()).updateMovement(any)
          verify(ernRetrievalRepository).save(eqTo(ern))
        }
      }
      "we try to retrieve messages and there are some" should {
        "add messages to the movement" in {
          val ern = "testErn"
          val movement = Movement(None, "token", "Consignor", None)
          val eisResponse = someNewMessagesResponse(ern)
          val decodedMessage = utils.decode(eisResponse.message)
          val newMessageDataResponse = scalaxb.fromXML[NewMessagesDataResponse](scala.xml.XML.loadString(decodedMessage))
          val IE704Message = ieMessageFactory.createIEMessage(newMessageDataResponse.Messages.messagesoption.head)
          val expectedMovement = movement.copy(messages = Seq(Message(utils.encode(IE704Message.toXml.toString()), "IE704", "messageId-4", eisResponse.dateTime)))

          when(dateTimeService.timestamp()).thenReturn(now)
          when(movementRepository.getAllBy(any)).thenReturn(Future.successful(Seq(movement)))
          when(movementRepository.updateMovement(any)).thenReturn(Future.successful(Some(expectedMovement)))
          when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
          when(ernRetrievalRepository.save(any)).thenReturn(Future.successful(Done))
          when(showNewMessagesConnector.get(any)(any)).thenReturn(Future.successful(Right(eisResponse)))

          messageService.updateMessages(ern).futureValue

          verify(showNewMessagesConnector).get(eqTo(ern))(any)
          verify(movementRepository).getAllBy(eqTo(ern))
          verify(movementRepository).updateMovement(eqTo(expectedMovement))
          verify(ernRetrievalRepository).save(eqTo(ern))
        }
      }
    }
  }
}
