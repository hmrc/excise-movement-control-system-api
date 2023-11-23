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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{ParsedXmlRequest, ValidatedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.EmcsUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidateErnsActionImpl @Inject()(messageService: MessageService)(
  implicit val executionContext: ExecutionContext,
  implicit val emcsUtils: EmcsUtils
) extends ValidateErnsAction
  with Logging {
  override def refine[A](request: ParsedXmlRequest[A]): Future[Either[Result, ValidatedXmlRequest[A]]] = {

    val matchedErns: Future[Set[String]] =
      messageService.getErns(request.ieMessage).map(ernsFromMessage => request.erns.intersect(ernsFromMessage))

    matchedErns.map(erns => {
      if (erns.nonEmpty) {
        Right(ValidatedXmlRequest(request, erns))
      }
      else {
        logger.error("[ValidateErnAction] - Invalid Excise Number")
        Left(
          Forbidden(
            Json.toJson(
              ErrorResponse(
                emcsUtils.getCurrentDateTime,
                "ERN validation error",
                "Excise number in message does not match authenticated excise number"
              )
            )
          )
        )
      }
    })

  }
}

@ImplementedBy(classOf[ValidateErnsActionImpl])
trait ValidateErnsAction extends ActionRefiner[ParsedXmlRequest, ValidatedXmlRequest] {
  def refine[A](request: ParsedXmlRequest[A]): Future[Either[Result, ValidatedXmlRequest[A]]]
}