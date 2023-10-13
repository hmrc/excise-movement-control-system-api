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
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{DataRequest, DataRequestIE818, ParsedXmlRequest, ParsedXmlRequestIE818}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{MovementMessage, MovementMessageIE818}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ValidateConsignorActionIE818Impl @Inject()(implicit val executionContext: ExecutionContext)
  extends ValidateConsignorActionIE818
    with Logging {
  override def refine[A](request: ParsedXmlRequestIE818[A]): Future[Either[Result, DataRequestIE818[A]]] = {

    val consigneeIdOption = request.ie818Message.Body.AcceptedOrRejectedReportOfReceiptExport.ConsigneeTrader

    if (consigneeIdOption.isEmpty) {
      //TODO something
    }

    //TODO if traderid not defined
    val consigneeId = consigneeIdOption.get.Traderid.get

    if(request.erns.contains(consigneeId)) {
      Future.successful(Right(DataRequestIE818(
        request,
        MovementMessageIE818(Some(consigneeId)),
        request.internalId))
      )
    }
    else {
      logger.error("[ValidateErnAction] - Invalid Excise Number")
      Future.successful(Left(Forbidden("Invalid Excise Number")))
    }
  }
}

@ImplementedBy(classOf[ValidateConsignorActionIE818Impl])
trait ValidateConsignorActionIE818 extends ActionRefiner[ParsedXmlRequestIE818, DataRequestIE818]{
  def refine[A](request: ParsedXmlRequestIE818[A]): Future[Either[Result, DataRequestIE818[A]]]
}
