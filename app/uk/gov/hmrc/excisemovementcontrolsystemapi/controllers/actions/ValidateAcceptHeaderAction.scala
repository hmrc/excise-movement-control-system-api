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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.Results.NotAcceptable
import play.api.mvc.{ActionFilter, Request, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidateAcceptHeaderAction @Inject() (datetimeService: DateTimeService)(implicit ec: ExecutionContext)
    extends ActionFilter[Request] {

  override val executionContext: ExecutionContext = ec

  override def filter[A](request: Request[A]): Future[Option[Result]] = Future.successful {

    val pattern = """^application/vnd[.]{1}hmrc[.]{1}1{1}[.]0[+]{1}xml$""".r

    request.headers.get(HeaderNames.ACCEPT) match {
      case Some(value) if pattern.matches(value) => None
      case _                                     =>
        Some(
          NotAcceptable(
            Json.toJson(
              ErrorResponse(
                datetimeService.timestamp(),
                "Invalid Accept header",
                "The accept header is missing or invalid"
              )
            )
          )
        )
    }
  }

}
