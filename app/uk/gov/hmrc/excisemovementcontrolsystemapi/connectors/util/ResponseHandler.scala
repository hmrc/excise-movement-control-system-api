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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.util

import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.HttpResponse

import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

trait ResponseHandler {

  def extractIfSuccessful[T](
    response: HttpResponse
  )(implicit reads: Reads[T], tt: TypeTag[T]): Either[HttpResponse, T] =
    if (is2xx(response.status)) Right(jsonAs[T](response.body))
    else Left(response)

  def jsonAs[T](body: String)(implicit reads: Reads[T], tt: TypeTag[T]): T =
    Try(Json.parse(body).as[T]) match {
      case Success(obj)       => obj
      case Failure(exception) =>
        throw new RuntimeException(s"Response body could not be read as type ${typeOf[T]}", exception)
    }

  def removeControlDocumentReferences(errorMsg: Option[String]): Option[String] =
    errorMsg.map(x => x.replaceAll("/con:[^/]*(?=/)", ""))
}
