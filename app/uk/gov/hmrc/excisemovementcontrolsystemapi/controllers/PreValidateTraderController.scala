/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseJsonAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.{ParsedPreValidateTraderETDSRequest, ParsedPreValidateTraderRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response._
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{CorrelationIdService, PreValidateTraderService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TraderTypeInterpreter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreValidateTraderController @Inject() (
  authAction: AuthAction,
  parseJsonAction: ParseJsonAction,
  preValidateTraderService: PreValidateTraderService,
  cc: ControllerComponents,
  appConfig: AppConfig,
  correlationIdService: CorrelationIdService
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def submit: Action[JsValue] = authAction(parse.json).async { implicit authRequest: EnrolmentRequest[JsValue] =>
    if (appConfig.etdsPreValidateTraderEnabled) {
      handleETDSRequest()
    } else {
      handleLegacyRequest()
    }
  }

  private def handleETDSRequest()(implicit authRequest: EnrolmentRequest[JsValue]): Future[Result] =
    parseJsonAction.refineETDS(authRequest).flatMap {
      case Right(parsedRequest: ParsedPreValidateTraderETDSRequest[JsValue]) =>
        preValidateTraderService.submitETDSMessage(parsedRequest).map {
          case Right(response) => Ok(Json.toJson(convertETDSResponse(response, parsedRequest)))
          case Left(error)     => error
        }
      case Left(result)                                                      => Future.successful(result)
    }

  private def convertETDSResponse(
    response: ExciseTraderValidationETDSResponse,
    parsedRequest: ParsedPreValidateTraderETDSRequest[JsValue]
  ): PreValidateTraderMessageResponse = {

    val entityGroup = parsedRequest.json.entityGroup

    val validTrader = response.failDetails match {
      case Some(fd) => fd.validTrader
      case None     => true
    }

    val errorCode = response.failDetails.flatMap(_.errorCode).map(_.toString)
    val errorText = response.failDetails.flatMap(_.errorText)

    val validateProductAuthResponse = if (validTrader) {
      createValidateProductAuthResponse(response, validTrader)
    } else {
      None
    }

    PreValidateTraderMessageResponse(
      validationTimeStamp = response.processingDateTime,
      exciseRegistrationNumber = response.exciseId,
      entityGroup = entityGroup,
      validTrader = validTrader,
      errorCode = errorCode,
      errorText = errorText,
      traderType = determineTraderType(response.traderType, validTrader),
      validateProductAuthorisationResponse = validateProductAuthResponse
    )
  }

  def determineTraderType(traderTypeDescription: String, validTrader: Boolean): Option[String] =
    if (validTrader) {
      Some(TraderTypeInterpreter.fromTraderTypeDescription(traderTypeDescription))
    } else {
      None
    }

  private def createValidateProductAuthResponse(
    response: ExciseTraderValidationETDSResponse,
    isTraderValid: Boolean
  ): Option[ValidateProductAuthorisationResponse] = {
    val productErrors = for {
      failDetails <- response.failDetails.toSeq
      vpa         <- failDetails.validateProductAuthorisationResponse.toSeq
      pe          <- vpa.productError
    } yield ProductError(
      exciseProductCode = pe.exciseProductCode,
      errorCode = pe.errorCode.toString,
      errorText = pe.errorText
    )

    val productsValid = productErrors.isEmpty
    (productsValid, isTraderValid) match {
      case (true, false) => None
      case (_, _)        =>
        Some(
          ValidateProductAuthorisationResponse(
            valid = productsValid,
            productError = if (!productsValid) Some(productErrors) else None
          )
        )
    }

  }

  private def handleLegacyRequest()(implicit authRequest: EnrolmentRequest[JsValue]): Future[Result] =
    parseJsonAction.refine(authRequest).flatMap {
      case Right(parsedRequest: ParsedPreValidateTraderRequest[JsValue]) =>
        preValidateTraderService.submitMessage(parsedRequest).map {
          case Right(response) => Ok(Json.toJson(response))
          case Left(error)     => error
        }
      case Left(result)                                                  => Future.successful(result)
    }
}
