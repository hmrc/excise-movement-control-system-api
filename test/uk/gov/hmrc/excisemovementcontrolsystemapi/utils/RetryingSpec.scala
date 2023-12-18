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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils


import akka.Done
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.concurrent.Futures
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class RetryingSpec extends PlaySpec with ScalaFutures with BeforeAndAfterEach{

  private val mockFutures =  mock[Futures]
  private val delays = List.fill(3)(0.millis)
  private var numOfRetried = 0

  class RetryingObj @Inject() extends Retrying{
    override implicit val ec: ExecutionContext = ExecutionContext.global
    override implicit val futures: Futures = mockFutures
  }

  private val retrying = new RetryingObj

  private val retryCondition: Try[HttpResponse] => Boolean = {
    case Success(value)  => Status.isClientError(value.status) || Status.isServerError(value.status)
    case Failure(_) => true

  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFutures)

    when(mockFutures.delay(any)) thenReturn Future.successful(Done)
    numOfRetried = 0
  }

  "retry" should {
    "process a block and return 200 with no retry" in {

      def block: Future[HttpResponse] = {
          Future.successful(HttpResponse(200, "Success"))
      }

      val result = retrying.retry(delays, retryCondition, "/url")(block).futureValue

      result.status mustBe OK
      numOfRetried mustBe 0
    }

    "retry 3 times" when {
      "always return a non 2xx status code" in {
        def block: Future[HttpResponse] = {
          Future.successful(HttpResponse(BAD_REQUEST, "failure"))
        }

        val result = await(retrying.retry(delays, retryCondition, "/url")(block))

        result.status mustBe BAD_REQUEST
      }

      "an exception occur continuously" in {
        def block: Future[HttpResponse] = {
            numOfRetried += 1
            Future.failed(new RuntimeException("error"))
        }

        the [RuntimeException] thrownBy {
          await(retrying.retry(delays, retryCondition, "/url")(block))
        } must have message "error"

        numOfRetried mustBe 3
      }
    }

    "stop retrying" when {
      "successful after an exception" in {
        def block: Future[HttpResponse] = {
          numOfRetried += 1
          numOfRetried match {
              case 1 => Future.successful(HttpResponse(BAD_REQUEST, "failure"))
              case 2 => Future.successful(HttpResponse(OK, "Success"))
          }
        }

        val result = await(retrying.retry(delays, retryCondition, "/url")(block))

        result.status mustBe OK
        numOfRetried mustBe 2
      }
    }
  }

}
