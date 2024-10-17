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
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseJsonAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.{ParsedPreValidateTraderETDSRequest, ParsedPreValidateTraderRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response._
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PreValidateTraderService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreValidateTraderController @Inject() (
  authAction: AuthAction,
  parseJsonAction: ParseJsonAction,
  preValidateTraderService: PreValidateTraderService,
  cc: ControllerComponents,
  appConfig: AppConfig
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
          case Right(response) => processETDSResponse(response, parsedRequest)
          case Left(error)     => error
        }
      case Left(result)                                                      => Future.successful(result)
    }

  private def processETDSResponse(
    response: PreValidateTraderETDSResponse,
    parsedRequest: ParsedPreValidateTraderETDSRequest[JsValue]
  ): Result = response match {
    case response200: ExciseTraderValidationETDSResponse =>
      val convertedResponseBody = convertETDSResponse(response200, parsedRequest)
      Ok(Json.toJson(convertedResponseBody))

    case response400: PreValidateTraderETDS400ErrorMessageResponse =>
      BadRequest(Json.toJson(response400))

    case response500: PreValidateTraderETDS500ErrorMessageResponse =>
      InternalServerError(Json.toJson(response500))
  }

  private def convertETDSResponse(
    response: ExciseTraderValidationETDSResponse,
    parsedRequest: ParsedPreValidateTraderETDSRequest[JsValue]
  ): PreValidateTraderMessageResponse = {
    val entityGroup = parsedRequest.json.entityGroup.getOrElse(
      "N/A"
    ) //Do we want to make this non optional in ETDS to allign with current spec? not needed but keeps us pretty
    val validTrader = response.validationResult == "Pass"
    val traderType  = determineTraderType(response.exciseId, validTrader)

    val errorCode = response.failDetails.flatMap(_.errorCode).map(_.toString)
    val errorText = response.failDetails.flatMap(_.errorText)

    val validateProductAuthResponse = createValidateProductAuthResponse(response, validTrader)

    PreValidateTraderMessageResponse(
      validationTimeStamp = response.processingDateTime,
      exciseRegistrationNumber = response.exciseId,
      entityGroup = entityGroup,
      validTrader = validTrader,
      errorCode = errorCode,
      errorText = errorText,
      traderType = traderType,
      validateProductAuthorisationResponse = validateProductAuthResponse
    )
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

  def determineTraderType(exciseId: String, validTrader: Boolean): Option[String] =
    if (validTrader) { //need to check we only populate this if we have a valid trader
      exciseId.substring(2, 4) match {
        case "WK" => Some("1") // 1 = Warehouse Keeper
        case "??" => Some("2") // 2 = Tax Warehouse
        case "??" => Some("3") // 3 = Registered Consignor
        case "??" => Some("4") // 4 = Registered Consignee
        case "??" => Some("5") // 5 = Temporary Registered Consignee
        case "??" => Some("6") // 6 = Temporary Registered Authorisation
        case _    => Some("7") // 7 = Other
      }
    } else {
      None
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
