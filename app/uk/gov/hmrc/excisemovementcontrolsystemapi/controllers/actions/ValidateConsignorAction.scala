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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{DataRequest, ParsedXmlRequest, ParsedXmlRequestCopy}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ValidateConsignorActionImpl @Inject()(implicit val executionContext: ExecutionContext, implicit val emcsUtils: EmcsUtils)
  extends ValidateConsignorAction
    with Logging {
  override def refine[A](request: ParsedXmlRequestCopy[A]): Future[Either[Result, DataRequest[A]]] = {

    request.ieMessage.consignorId
    val consignorId = request.ieMessage.consignorId

    if (request.erns.contains(consignorId)) {
      val consigneeId = request.ieMessage.consigneeId
      //todo: some messages have no LRN. Should we throw an exception here?
      val localRefNumber = request.ieMessage.localReferenceNumber.getOrElse(throw new RuntimeException("Local reference number is null"))
      Future.successful(Right(DataRequest(
        request,
        Movement(localRefNumber, consignorId, consigneeId),
        request.internalId))
      )
    }
    else {
      logger.error("[ValidateErnAction] - Invalid Excise Number")
      Future.successful(
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
      )
    }
  }
}

@ImplementedBy(classOf[ValidateConsignorActionImpl])
trait ValidateConsignorAction extends ActionRefiner[ParsedXmlRequestCopy, DataRequest] {
  def refine[A](request: ParsedXmlRequestCopy[A]): Future[Either[Result, DataRequest[A]]]
}
