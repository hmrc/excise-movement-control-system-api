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

import dispatch.Future
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{MessageTypes, MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ExciseNumberRepository, MovementMessageRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumber, Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{DateTimeService, ExciseNumberService, MovementMessageService, ShowNewMessageParser}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext

class ExciseNumberServiceSpec extends PlaySpec
  with BeforeAndAfterEach
  with EitherValues {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockExciseNumberRepository = mock[ExciseNumberRepository]

  private val exciseNumberService = new ExciseNumberService(mockExciseNumberRepository)

  private val lrn = "123"
  private val consignorId = "ABC"
  private val consigneeId = "ABC123"
  private val timeService = mock[DateTimeService]
  private val now = Instant.parse("2018-11-30T18:35:24.00Z")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockExciseNumberRepository)
    when(timeService.now).thenReturn(now)
  }

  "saveExciseNumber" should {
    "save a ExciseNumber" in {
      when(mockExciseNumberRepository.save(any))
        .thenReturn(Future.successful(true))

      val result = await(exciseNumberService.saveExciseNumber(ExciseNumber("123","lrn", now)))

      result mustBe Right(ExciseNumber("123","lrn", now))
    }

    "throw an error" in {
      when(mockExciseNumberRepository.save(any))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(exciseNumberService.saveExciseNumber(ExciseNumber("123","lrn", now)))

      result.left.value mustBe MongoError("error")
    }
  }

}
