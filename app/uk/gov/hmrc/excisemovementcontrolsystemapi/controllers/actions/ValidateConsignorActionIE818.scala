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
import play.api.mvc.Results.{BadRequest, Forbidden}
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{DataRequestIE818, ParsedXmlRequestIE818}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.MovementMessageIE818

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ValidateConsignorActionIE818Impl @Inject()(implicit val executionContext: ExecutionContext, implicit val emcsUtils: EmcsUtils)
  extends ValidateConsignorActionIE818
    with Logging {
  override def refine[A](request: ParsedXmlRequestIE818[A]): Future[Either[Result, DataRequestIE818[A]]] = {

    (for {
      consigneeIdOption <- request.ie818Message.Body.AcceptedOrRejectedReportOfReceiptExport.ConsigneeTrader
      traderId <- consigneeIdOption.Traderid
      isValidConsignee = request.erns.contains(traderId)
    } yield (traderId, isValidConsignee)) match {
      case Some(a) if a._1.nonEmpty && a._2 =>
        Future.successful(Right(DataRequestIE818(
          request,
          MovementMessageIE818(a._1),
          request.erns,
          request.internalId)))
      case Some(a) if a._1.nonEmpty && !a._2 =>
        logger.error("[ValidateErnAction] - Invalid Excise Number")
        Future.successful(
          Left(
            Forbidden(Json.toJson(ErrorResponse(
              emcsUtils.getCurrentDateTime,
              "ERN validation error",
              "Excise number in message does not match authenticated excise number"
            ))))
        )
      case _ => Future.successful(
        Left(BadRequest(Json.toJson(ErrorResponse(
          emcsUtils.getCurrentDateTime,
          "ERN validation error",
          "Consignee ID should be supplied in the message"
        ))))
      )

    }

  }
}

@ImplementedBy(classOf[ValidateConsignorActionIE818Impl])
trait ValidateConsignorActionIE818 extends ActionRefiner[ParsedXmlRequestIE818, DataRequestIE818] {
  def refine[A](request: ParsedXmlRequestIE818[A]): Future[Either[Result, DataRequestIE818[A]]]
}
