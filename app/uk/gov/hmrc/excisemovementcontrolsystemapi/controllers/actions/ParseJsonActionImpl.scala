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
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.{ActionRefiner, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.{ExciseTraderETDSRequest, ParsedPreValidateTraderETDSRequest, ParsedPreValidateTraderRequest, PreValidateTraderRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, preValidateTrader}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ParseJsonActionImpl @Inject() (
  dateTimeService: DateTimeService,
  cc: ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends BackendController(cc)
    with ParseJsonAction
    with Logging {

  override def refineDynamic[A](
    request: EnrolmentRequest[A],
    etdsEnabled: Boolean
  ): Future[Either[Result, Either[ParsedPreValidateTraderRequest[A], ParsedPreValidateTraderETDSRequest[A]]]] =
    if (etdsEnabled) {
      refineETDS(request).map {
        case Right(etdsRequest) => Right(Right(etdsRequest))
        case Left(error)        => Left(error)
      }
    } else {
      refine(request).map {
        case Right(traderRequest) => Right(Left(traderRequest))
        case Left(error)          => Left(error)
      }
    }

  override def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedPreValidateTraderRequest[A]]] =
    request.body match {
      case body: JsValue => parseJson(body, request)
      case _             =>
        logger.warn("Not valid Json")
        Future.successful(Left(BadRequest(Json.toJson(handleError("Json error", "Not valid Json or Json is empty")))))
    }

  override def refineETDS[A](
    request: EnrolmentRequest[A]
  ): Future[Either[Result, ParsedPreValidateTraderETDSRequest[A]]] =
    request.body match {
      case body: JsValue => parseETDSJson(body, request)
      case _             =>
        logger.warn("Not valid Json")
        Future.successful(Left(BadRequest(Json.toJson(handleError("Json error", "Not valid Json or Json is empty")))))
    }

  def parseJson[A](
    jsonBody: JsValue,
    request: EnrolmentRequest[A]
  ): Future[Either[Result, ParsedPreValidateTraderRequest[A]]] =
    Try(jsonBody.as[PreValidateTraderRequest]) match {
      case Success(value) =>
        Future.successful(Right(preValidateTrader.request.ParsedPreValidateTraderRequest(request, value)))

      case Failure(exception: JsResultException) =>
        logger.warn(s"Not valid Pre Validate Trader message: ${exception.getMessage}", exception)
        Future.successful(
          Left(
            BadRequest(
              Json.toJson(
                handleError(
                  s"Not valid PreValidateTrader message",
                  s"Error parsing Json: ${exception.errors.toString()}"
                )
              )
            )
          )
        )

      case Failure(exception) =>
        logger.warn(s"Not valid Pre Validate Trader message: ${exception.getMessage}", exception)
        Future.successful(
          Left(BadRequest(Json.toJson(handleError(s"Not valid PreValidateTrader message", exception.getMessage))))
        )
    }

  def parseETDSJson[A](
    jsonBody: JsValue,
    request: EnrolmentRequest[A]
  ): Future[Either[Result, ParsedPreValidateTraderETDSRequest[A]]] =
    Try(jsonBody.as[ExciseTraderETDSRequest]) match {
      case Success(value) =>
        Future.successful(Right(preValidateTrader.request.ParsedPreValidateTraderETDSRequest(request, value)))

      case Failure(exception: JsResultException) =>
        logger.warn(s"Not valid ETDS Pre Validate Trader message: ${exception.getMessage}", exception)
        Future.successful(
          Left(
            BadRequest(
              Json.toJson(
                handleError(
                  s"Not valid ETDS PreValidateTrader message",
                  s"Error parsing Json: ${exception.errors.toString()}"
                )
              )
            )
          )
        )

      case Failure(exception) =>
        logger.warn(s"Not valid Pre Validate Trader message: ${exception.getMessage}", exception)
        Future.successful(
          Left(BadRequest(Json.toJson(handleError(s"Not valid PreValidateTrader message", exception.getMessage))))
        )
    }

  private def handleError(
    message: String,
    debugMessage: String
  ): ErrorResponse =
    ErrorResponse(dateTimeService.timestamp(), message, debugMessage)
}

@ImplementedBy(classOf[ParseJsonActionImpl])
trait ParseJsonAction extends ActionRefiner[EnrolmentRequest, ParsedPreValidateTraderRequest] {
  def refine[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedPreValidateTraderRequest[A]]]
  def refineETDS[A](request: EnrolmentRequest[A]): Future[Either[Result, ParsedPreValidateTraderETDSRequest[A]]]
  def refineDynamic[A](
    request: EnrolmentRequest[A],
    etdsEnabled: Boolean
  ): Future[Either[Result, Either[ParsedPreValidateTraderRequest[A], ParsedPreValidateTraderETDSRequest[A]]]]
}
