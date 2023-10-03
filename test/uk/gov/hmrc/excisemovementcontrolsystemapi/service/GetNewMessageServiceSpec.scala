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
import org.mockito.MockitoSugar.{verify, when}
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.ShowNewMessagesConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ShowNewMessageResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.GetNewMessageServiceImpl
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class GetNewMessageServiceSpec extends PlaySpec with EitherValues{

  protected implicit val ec: ExecutionContext = ExecutionContext.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector = mock[ShowNewMessagesConnector]
  private val sut = new GetNewMessageServiceImpl(connector)

  "getNewMessages" should {
    "get messages for an excise number" in {
      val dateTime = LocalDateTime.of(2023, 3, 4, 5, 6, 7)
      when(connector.get(any)(any)).thenReturn(Future.successful(Right(ShowNewMessageResponse(dateTime, "123", "any message"))))

      val result = await(sut.getNewMessages("123"))

      verify(connector).get(eqTo("123"))(any)
      result mustBe Right(ShowNewMessageResponse(dateTime, "123", "any message"))
    }

    "return an error" in {
      when(connector.get(any)(any)).thenReturn(Future.successful(Left(InternalServerError("error"))))

      val result = await(sut.getNewMessages("123"))

      result mustBe Left(InternalServerError("error"))
    }

    "acknowledge the new messages" in {
      val dateTime = LocalDateTime.of(2023, 3, 4, 5, 6, 7)
      when(connector.get(any)(any)).thenReturn(Future.successful(Right(ShowNewMessageResponse(dateTime, "123", "any message"))))

      val result = await(sut.getNewMessages("123"))

      result mustBe Right(ShowNewMessageResponse(dateTime, "123", "any message"))
    }
  }
}
