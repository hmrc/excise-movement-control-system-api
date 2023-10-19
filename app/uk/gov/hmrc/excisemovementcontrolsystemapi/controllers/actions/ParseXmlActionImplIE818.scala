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
import play.api.mvc.{ActionRefiner, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequestIE818}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.XmlParserIE818
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

class ParseIE818XmlActionImpl @Inject()
(
  xmlParser: XmlParserIE818,
  cc: ControllerComponents
)(implicit val executionContext: ExecutionContext, implicit val emcsUtils: EmcsUtils) extends BackendController(cc)
  with ParseIE818XmlAction
  with Logging {

  override def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedXmlRequestIE818[A]]] = {

    request.body match {
      case body: NodeSeq if body.nonEmpty => parseXml(body, request)
      case _ =>
        logger.error("Not valid XML or XML is empty")
        Future.successful(
          Left(
            BadRequest(
              Json.toJson(
                ErrorResponse(
                  emcsUtils.getCurrentDateTime,
                  "XML formatting error",
                  "Not valid XML or XML is empty",
                  emcsUtils.generateCorrelationId
                )
              )
            )
          )
        )
    }
  }

  def parseXml[A](xmlBody: NodeSeq, request: EnrolmentRequest[A]): Future[Either[Result, ParsedXmlRequestIE818[A]]] = {

    Try(xmlParser.fromXml(xmlBody)) match {
      case Success(value) => Future.successful(Right(ParsedXmlRequestIE818(request, value, request.erns, request.internalId)))
      case Failure(exception) =>
        logger.error(s"Not valid IE818 message: ${exception.getMessage}", exception)
        Future.successful(
          Left(
            BadRequest(
              Json.toJson(
                ErrorResponse(
                  emcsUtils.getCurrentDateTime,
                  "XML formatting error",
                  s"Not valid IE818 message: ${exception.getMessage}",
                  emcsUtils.generateCorrelationId
                )
              )
            )
          )
        )
    }
  }

}

@ImplementedBy(classOf[ParseIE818XmlActionImpl])
trait ParseIE818XmlAction extends ActionRefiner[EnrolmentRequest, ParsedXmlRequestIE818] {

  def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedXmlRequestIE818[A]]]
}
