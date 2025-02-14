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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import org.bson.types.ObjectId
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem

import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Instant

object TestUtils {

  def createWorkItem(
    ern: String,
    fastPollRetries: Int = 0,
    availableAt: Instant,
    receivedAt: Instant,
    updatedAt: Instant,
    status: ProcessingStatus = ToDo,
    failureCount: Int = 0
  ): WorkItem[ExciseNumberWorkItem] =
    WorkItem(
      id = new ObjectId(),
      receivedAt = receivedAt,
      updatedAt = updatedAt,
      availableAt = availableAt,
      status = status,
      failureCount = failureCount,
      item = ExciseNumberWorkItem(ern, fastPollRetries)
    )

  def getPreValidateTraderSuccessEISResponse: PreValidateTraderEISResponse = PreValidateTraderEISResponse(
    ExciseTraderValidationResponse(
      validationTimestamp = "2021-12-17T09:31:123Z",
      exciseTraderResponse = Array(
        ExciseTraderResponse(
          exciseRegistrationNumber = "GBWK002281023WK",
          entityGroup = "UK Record",
          validTrader = true,
          traderType = Some("1"),
          validateProductAuthorisationResponse = Some(
            ValidateProductAuthorisationResponse(valid = true)
          )
        )
      )
    )
  )

  def getPreValidateTraderETDSMessageResponseAllFail: ExciseTraderValidationETDSResponse =
    ExciseTraderValidationETDSResponse(
      processingDateTime = "2021-12-17T09:31:123Z",
      exciseId = "GBWK000000000",
      traderType = "Not Found",
      validationResult = "Fail",
      failDetails = Option(
        ETDSFailDetails(
          validTrader = false,
          errorCode = Some(6),
          errorText = Some("Not Found"),
          validateProductAuthorisationResponse = Option(
            ValidateProductAuthorisationETDSResponse(productError =
              Seq(
                ProductErrorETDS(
                  exciseProductCode = "S200",
                  errorCode = 1,
                  errorText = "Unrecognised EPC"
                )
              )
            )
          )
        )
      )
    )

  def getPreValidateTraderETDSMessageResponseTraderFail: ExciseTraderValidationETDSResponse =
    ExciseTraderValidationETDSResponse(
      processingDateTime = "2021-12-17T09:31:123Z",
      exciseId = "GBWK000000000",
      traderType = "Not Found",
      validationResult = "Fail",
      failDetails = Option(
        ETDSFailDetails(
          validTrader = false,
          errorCode = Some(6),
          errorText = Some("Not Found"),
          validateProductAuthorisationResponse = None
        )
      )
    )

  def getPreValidateTraderETDSMessageResponseProductsFail: ExciseTraderValidationETDSResponse =
    ExciseTraderValidationETDSResponse(
      processingDateTime = "2021-12-17T09:31:123Z",
      exciseId = "GBWK000000000",
      traderType = "Authorised Warehouse Keeper",
      validationResult = "Pass",
      failDetails = Option(
        ETDSFailDetails(
          validTrader = true,
          errorCode = None,
          errorText = None,
          validateProductAuthorisationResponse = Option(
            ValidateProductAuthorisationETDSResponse(productError =
              Seq(
                ProductErrorETDS(
                  exciseProductCode = "S200",
                  errorCode = 1,
                  errorText = "Unrecognised EPC"
                )
              )
            )
          )
        )
      )
    )

  def getPreValidateTraderErrorEISResponseNoProductResponse: PreValidateTraderEISResponse =
    PreValidateTraderEISResponse(
      ExciseTraderValidationResponse(
        validationTimestamp = "2021-12-17T09:31:123Z",
        exciseTraderResponse = Array(
          ExciseTraderResponse(
            validTrader = false,
            exciseRegistrationNumber = "GBWK000000000",
            traderType = None,
            entityGroup = "UK Record",
            errorCode = Some("6"),
            errorText = Some("Not Found"),
            validateProductAuthorisationResponse = None
          )
        )
      )
    )

  def getPreValidateTraderErrorEISResponse: PreValidateTraderEISResponse = PreValidateTraderEISResponse(
    ExciseTraderValidationResponse(
      validationTimestamp = "2021-12-17T09:31:123Z",
      exciseTraderResponse = Array(
        ExciseTraderResponse(
          validTrader = false,
          exciseRegistrationNumber = "GBWK000000000",
          traderType = None,
          entityGroup = "UK Record",
          errorCode = Some("6"),
          errorText = Some("Not Found"),
          validateProductAuthorisationResponse = Some(
            ValidateProductAuthorisationResponse(
              valid = false,
              productError = Some(
                Seq(
                  ProductError(
                    errorCode = "1",
                    errorText = "Unrecognised EPC",
                    exciseProductCode = "S200"
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  def getExciseTraderValidationETDSResponse: ExciseTraderValidationETDSResponse =
    ExciseTraderValidationETDSResponse(
      processingDateTime = "2021-12-17T09:31:123Z",
      exciseId = "GBWK002281023WK",
      traderType = "Authorised Warehouse Keeper",
      validationResult = "Pass",
      failDetails = Option(
        ETDSFailDetails(
          validTrader = true,
          errorCode = None,
          errorText = None,
          validateProductAuthorisationResponse = None
        )
      )
    )

  def getPreValidateTraderSuccessResponse: PreValidateTraderMessageResponse = PreValidateTraderMessageResponse(
    validationTimeStamp = "2021-12-17T09:31:123Z",
    exciseRegistrationNumber = "GBWK002281023WK",
    entityGroup = "UK Record",
    validTrader = true,
    traderType = Some("1"),
    validateProductAuthorisationResponse = Some(ValidateProductAuthorisationResponse(valid = true))
  )

  def getPreValidateTraderErrorResponseAllFail: PreValidateTraderMessageResponse =
    PreValidateTraderMessageResponse(
      validationTimeStamp = "2021-12-17T09:31:123Z",
      validTrader = false,
      exciseRegistrationNumber = "GBWK000000000",
      traderType = None,
      entityGroup = "UK Record",
      errorCode = Some("6"),
      errorText = Some("Not Found"),
      validateProductAuthorisationResponse = None
    )

  def getPreValidateTraderErrorResponse: PreValidateTraderMessageResponse =
    PreValidateTraderMessageResponse(
      validationTimeStamp = "2021-12-17T09:31:123Z",
      validTrader = false,
      exciseRegistrationNumber = "GBWK000000000",
      traderType = None,
      entityGroup = "UK Record",
      errorCode = Some("6"),
      errorText = Some("Not Found"),
      validateProductAuthorisationResponse = None
    )

  def getPreValidateTraderProductErrorResponse: PreValidateTraderMessageResponse =
    PreValidateTraderMessageResponse(
      validationTimeStamp = "2021-12-17T09:31:123Z",
      validTrader = true,
      exciseRegistrationNumber = "GBWK000000000",
      traderType = Some("1"),
      entityGroup = "UK Record",
      errorCode = None,
      errorText = None,
      validateProductAuthorisationResponse = Some(
        ValidateProductAuthorisationResponse(
          valid = false,
          productError = Some(
            Seq(
              ProductError(
                errorCode = "1",
                errorText = "Unrecognised EPC",
                exciseProductCode = "S200"
              )
            )
          )
        )
      )
    )

  def getPreValidateTraderRequest: PreValidateTraderRequest = PreValidateTraderRequest(
    ExciseTraderValidationRequest(
      ExciseTraderRequest(
        "GBWK002281023",
        "UK Record",
        Seq(ValidateProductAuthorisationRequest(ExciseProductCode("W200")))
      )
    )
  )

  def getPreValidateTraderETDSRequest: ExciseTraderETDSRequest = ExciseTraderETDSRequest(
    "GBWK002281023",
    "UK Record",
    Some(Seq(ValidateProductAuthorisationETDSRequest("W200")))
  )
}
