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

package uk.gov.hmrc.excisemovementcontrolsystemapi.service

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageReceiptConnector, ShowNewMessagesConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageReceiptResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{GetNewMessageServiceImpl, NewMessageParserService}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class GetNewMessageServiceSpec
  extends PlaySpec
    with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = ExecutionContext.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val dateTime = Instant.parse("2023-03-04T05:06:07Z")
  private val showNewMessageConnector = mock[ShowNewMessagesConnector]
  private val messageReceiptConnector = mock[MessageReceiptConnector]
  private val showNewMessageParser = mock[NewMessageParserService]
  private val sut = new GetNewMessageServiceImpl(showNewMessageConnector, messageReceiptConnector, showNewMessageParser)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(showNewMessageConnector, messageReceiptConnector)
  }

  "getNewMessages" should {
    "get messages for an excise number" in {
      when(showNewMessageParser.countOfMessagesAvailable(any)).thenReturn(10)
      when(showNewMessageConnector.get(any)(any))
        .thenReturn(Future.successful(Right(EISConsumptionResponse(dateTime, "123", "any message"))))
      when(messageReceiptConnector.put(any)(any))
        .thenReturn(Future.successful(Right(MessageReceiptResponse(dateTime, "123", 10))))

      val result = await(sut.getNewMessagesAndAcknowledge("123"))

      verify(showNewMessageConnector).get(eqTo("123"))(any)
      result mustBe Some((EISConsumptionResponse(dateTime, "123", "any message"), 10))

      withClue("acknowledge the messages") {
        verify(messageReceiptConnector).put(eqTo("123"))(any)
      }
    }

    "not call the message-receipt api if no new message found" in {
      when(showNewMessageParser.countOfMessagesAvailable(any)).thenReturn(0)
      when(showNewMessageConnector.get(any)(any))
        .thenReturn(Future.successful(Right(EISConsumptionResponse(dateTime, "123", ""))))
      when(messageReceiptConnector.put(any)(any))
        .thenReturn(Future.successful(Right(MessageReceiptResponse(dateTime, "123", 10))))

      await(sut.getNewMessagesAndAcknowledge("123"))

      withClue("acknowledge the messages") {
        verifyZeroInteractions(messageReceiptConnector)
      }
    }

    "return the response and zero message count if no messages returned" in {
      when(showNewMessageParser.countOfMessagesAvailable(any)).thenReturn(0)
      val consumptionResponse = EISConsumptionResponse(dateTime, "123", "")
      when(showNewMessageConnector.get(any)(any))
        .thenReturn(Future.successful(Right(consumptionResponse)))
      when(messageReceiptConnector.put(any)(any))
        .thenReturn(Future.successful(Right(MessageReceiptResponse(dateTime, "123", 10))))

      val result = await(sut.getNewMessagesAndAcknowledge("123"))

      result mustBe Some((consumptionResponse, 0))

    }

    "return message if message-receipt fails" in {
      when(showNewMessageParser.countOfMessagesAvailable(any)).thenReturn(7)
      when(showNewMessageConnector.get(any)(any))
        .thenReturn(Future.successful(Right(EISConsumptionResponse(dateTime, "123", "any message"))))
      when(messageReceiptConnector.put(any)(any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val result = await(sut.getNewMessagesAndAcknowledge("123"))

      result mustBe Some((EISConsumptionResponse(dateTime, "123", "any message"), 7))
    }

    "return No messages" when {
      "show-new-message api return an error" in {
        when(showNewMessageConnector.get(any)(any))
          .thenReturn(Future.successful(Left(InternalServerError("error"))))
        when(messageReceiptConnector.put(any)(any))
          .thenReturn(Future.successful(Right(MessageReceiptResponse(dateTime, "123", 10))))

        val result = await(sut.getNewMessagesAndAcknowledge("123"))

        result mustBe None

        withClue("message receipt API should not be called") {
          verifyZeroInteractions(messageReceiptConnector)
        }
      }
    }

    "return empty response and message count as 0 when message-receipt fails and there are no messages" in {
      when(showNewMessageParser.countOfMessagesAvailable(any)).thenReturn(0)
      val eisResponse = EISConsumptionResponse(dateTime, "123", "any message")
      when(showNewMessageConnector.get(any)(any))
        .thenReturn(Future.successful(Right(eisResponse)))
      when(messageReceiptConnector.put(any)(any))
        .thenReturn(Future.successful(Left(BadRequest("error"))))

      val result = await(sut.getNewMessagesAndAcknowledge("123"))

      result mustBe Some((eisResponse, 0))
    }

  }
}

