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
import scalaxb.ParserFailure
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.{IEMessageFactory, IEMessageFactoryException}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

class ParseXmlActionImpl @Inject() (
  ieMessageFactory: IEMessageFactory,
  dateTimeService: DateTimeService,
  cc: ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends BackendController(cc)
    with ParseXmlAction
    with Logging {

  override def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedXmlRequest[A]]] =
    request.body match {
      case body: NodeSeq if body.nonEmpty => parseXml(body, request)
      case _                              =>
        logger.warn("Not valid XML or XML is empty")
        Future.successful(Left(BadRequest(Json.toJson(handleError("XML error", "Not valid XML or XML is empty")))))
    }

  def parseXml[A](
    xmlBody: NodeSeq,
    request: EnrolmentRequest[A]
  ): Future[Either[Result, ParsedXmlRequest[A]]] = {

    val messageType = xmlBody.head.label
    Try(ieMessageFactory.createFromXml(messageType, xmlBody)) match {
      case Success(value) =>
        Future.successful(Right(ParsedXmlRequest(request, value, request.erns, request.userDetails)))

      case Failure(exception: ParserFailure) =>
        logger.warn(s"[ParseXmlActionImpl] - Not valid $messageType")
        logger.debug(s"[ParseXmlActionImpl] - Not valid $messageType message: ${exception.getMessage}", exception)
        Future.successful(
          Left(
            BadRequest(
              Json.toJson(
                handleError(s"Not valid $messageType message", getUsefulPartOfScalaxbMessage(exception.getMessage))
              )
            )
          )
        )

      case Failure(exception: IEMessageFactoryException) =>
        logger.warn(s"[ParseXmlActionImpl] - Not valid $messageType message: ${exception.getMessage}", exception)
        // Happy to return this exception message as we control it
        Future.successful(
          Left(BadRequest(Json.toJson(handleError(s"Not valid $messageType message", exception.getMessage))))
        )

      case Failure(exception) =>
        logger.warn(s"[ParseXmlActionImpl] - Not valid $messageType message: ${exception.getMessage}", exception)
        // Don't return exception message as we don't know what is in it
        Future.successful(
          Left(
            BadRequest(Json.toJson(handleError(s"Not valid $messageType message", "Error occurred parsing message")))
          )
        )
    }
  }

  private def handleError(
    message: String,
    debugMessage: String
  ): ErrorResponse =
    ErrorResponse(dateTimeService.timestamp(), message, debugMessage)

  private def getUsefulPartOfScalaxbMessage(message: String): String = {
    val pattern = ": parser error(.+)while parsing".r

    val matches = pattern.findFirstMatchIn(message)

    matches match {
      case None    => message
      case Some(x) => s"Parser error: ${x.group(1).strip()}"
    }
  }
}

@ImplementedBy(classOf[ParseXmlActionImpl])
trait ParseXmlAction extends ActionRefiner[EnrolmentRequest, ParsedXmlRequest] {
  def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedXmlRequest[A]]]
}
