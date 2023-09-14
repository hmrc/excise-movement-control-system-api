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
import generated.IE815Type
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, Reads}
import play.api.mvc.Results.BadRequest
import play.api.mvc.{Action, ActionRefiner, BodyParser, ControllerComponents, Result}
import play.mvc.BodyParser.Text
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{AuthorizedIE815Request, AuthorizedRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.XmlParser
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class ParseIE815XmlActionImpl @Inject()
(
  xmlParser: XmlParser,
  cc: ControllerComponents
)(implicit val executionContext: ExecutionContext) extends BackendController(cc) with ParseIE815XmlAction {


  override def refine[A](request: AuthorizedRequest[A]): Future[Either[Result, AuthorizedIE815Request[A]]] = {

    request.body match {
      case body: NodeSeq if body.nonEmpty =>
        Future.successful(Right(AuthorizedIE815Request(request, xmlParser.fromXml(body), "123")))
      //todo: Could we have plain String body?
      case body: String =>
        println("is string")
        Future.successful(Left(Ok("")))
        //todo: Could we have Json body?
      case body: JsValue =>
        println("is json")
        Future.successful(Left(Ok("")))
      case _ => Future.successful(Left(BadRequest("error")))
    }
  }
}

@ImplementedBy(classOf[ParseIE815XmlActionImpl])
trait ParseIE815XmlAction extends ActionRefiner[AuthorizedRequest, AuthorizedIE815Request] {
}
