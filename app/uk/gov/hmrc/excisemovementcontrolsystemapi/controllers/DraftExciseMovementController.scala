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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MovementMessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseIE815XmlAction, ValidateConsignorAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.AuthorizedIE815Request
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class DraftExciseMovementController @Inject()(
                                               authAction: AuthAction,
                                               xmlParser: ParseIE815XmlAction,
                                               consignorValidatorAction: ValidateConsignorAction,
                                               movementMessageConnector: MovementMessageConnector,
                                               cc: ControllerComponents
                                             )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def submit: Action[NodeSeq] =
    (authAction andThen xmlParser andThen consignorValidatorAction).async(parse.xml) { implicit request: AuthorizedIE815Request[NodeSeq] =>
      // todo integrate controller
      // movementMessageConnector.post("test-message", "IE815")
      Future.successful(Ok(Json.parse("""{"name": "mauro"}""")))
    }
}
