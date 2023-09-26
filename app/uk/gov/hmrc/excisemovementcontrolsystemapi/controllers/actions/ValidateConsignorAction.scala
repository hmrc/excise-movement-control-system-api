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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{DataRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.MovementMessage

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ValidateConsignorActionImpl @Inject()(implicit val executionContext: ExecutionContext)
  extends ValidateConsignorAction
    with Logging {
  override def refine[A](request: ParsedXmlRequest[A]): Future[Either[Result, DataRequest[A]]] = {

    val consignorId = request.ie815Message.Body.SubmittedDraftOfEADESAD.ConsignorTrader.TraderExciseNumber

    if(request.request.erns.contains(consignorId)) {
      val consigneeId = request.ie815Message.Body.SubmittedDraftOfEADESAD.ConsigneeTrader.flatMap(_.Traderid)
      val localRefNumber = request.ie815Message.Body.SubmittedDraftOfEADESAD.EadEsadDraft.LocalReferenceNumber
      Future.successful(Right(DataRequest(
        request,
        MovementMessage(localRefNumber, consignorId, consigneeId),
        request.internalId))
      )
    }
    else {
      logger.error("[ValidateErnAction] - Invalid Excise Number")
      Future.successful(Left(Forbidden("Invalid Excise Number")))
    }
  }
}

@ImplementedBy(classOf[ValidateConsignorActionImpl])
trait ValidateConsignorAction extends ActionRefiner[ParsedXmlRequest, DataRequest ]{
  def refine[A](request: ParsedXmlRequest[A]): Future[Either[Result, DataRequest[A]]]
}
