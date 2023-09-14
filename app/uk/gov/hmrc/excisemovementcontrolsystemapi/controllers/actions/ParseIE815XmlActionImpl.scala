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
import play.api.Logging
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
import scala.util.{Failure, Success, Try}

import play.api.mvc.AnyContentAsXml

class ParseIE815XmlActionImpl @Inject()
(
  xmlParser: XmlParser,
  cc: ControllerComponents
)(implicit val executionContext: ExecutionContext) extends BackendController(cc) with ParseIE815XmlAction with Logging {


  override def refine[A](request: AuthorizedRequest[A]): Future[Either[Result, AuthorizedIE815Request[A]]] = {

    println(s"XML received => : ${request.body}")

      request.body match {
        case body: NodeSeq if body.nonEmpty =>
          println("is NodeSeq")
          Future.successful(Right(AuthorizedIE815Request(request, null, "request.internalId")))
          parseXml(body, request)
          // TODO: We should be dealing with NodeSeq, remove this hack
        case body: AnyContentAsXml if body.xml.nonEmpty =>
          println("is AnyContentAsXml")
          parseXml(body.xml, request)
        case _ =>
          logger.error("Not valid XML or XML is empty")
          Future.successful(Left(BadRequest("Not valid XML or XML is empty")))
      }
  }

  def parseXml[A](xmlBody: NodeSeq, request: AuthorizedRequest[A]) : Future[Either[Result, AuthorizedIE815Request[A]]] = {

    Try(xmlParser.fromXml(xmlBody)) match {
      case Success(value) => Future.successful(Right(AuthorizedIE815Request(request, value, request.internalId)))
      case Failure(exception) =>
        logger.error(s"Not valid IE815 message: ${exception.getMessage}", exception)
        Future.successful(Left(BadRequest(s"Not valid IE815 message: ${exception.getMessage}")))
    }
  }
}

@ImplementedBy(classOf[ParseIE815XmlActionImpl])
trait ParseIE815XmlAction extends ActionRefiner[AuthorizedRequest, AuthorizedIE815Request] {

  def refine[A](request: AuthorizedRequest[A]): Future[Either[Result, AuthorizedIE815Request[A]]]
}
