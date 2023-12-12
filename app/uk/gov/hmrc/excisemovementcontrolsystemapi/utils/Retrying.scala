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

import play.api.Logging
import play.api.http.Status
import play.api.libs.concurrent.Futures
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait Retrying extends Logging {

  implicit val ec: ExecutionContext
  implicit val futures: Futures

  def retry(
    delays: Seq[FiniteDuration],
    retryCondition: Try[HttpResponse] => Boolean,
    url: String
  )(block: Int => Future[HttpResponse]): Future[HttpResponse] = {

    def loop(attemptNumber: Int, delays: Seq[FiniteDuration]): Future[HttpResponse] = {
      def retryIfPossible(result: Try[HttpResponse]): Future[HttpResponse] =
        delays match {
          case Nil => Future.fromTry(result)
          case delay :: Nil => futures.delay(delay).flatMap {_ => Future.fromTry(result)}
          case delay :: tail =>
            if (retryCondition(result)) {
              val message = result.fold(e => s"error occurred ${e.getMessage}", r =>  s" with status ${r.status.toString}")
              logger.warn(s"[Retrying] - EMCS_API_RETRY retrying: url $url, $message")
              futures.delay(delay).flatMap {_ => loop(attemptNumber + 1, tail)}
            } else Future.fromTry(result)
        }

      block(attemptNumber).transformWith { result: Try[HttpResponse] =>
        result match {
          case Success(response) if Status.isSuccessful(response.status) => Future.successful(response)
          case s: Success[HttpResponse] => retryIfPossible(s)
          case f @ Failure(ex) =>
             logger.error(s"[Retrying] - EMCS_API_RETRY error when retrying: url $url with message ${ex.getMessage}", ex)
            retryIfPossible(f)
        }
      }
    }

    loop(0, delays)
  }
}
