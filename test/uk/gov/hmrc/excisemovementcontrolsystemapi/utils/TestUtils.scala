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

  def createWorkItem(ern: String, fastPollRetries: Int = 0, availableAt: Instant,
                     receivedAt: Instant, updatedAt: Instant, status: ProcessingStatus = ToDo,
                     failureCount: Int = 0): WorkItem[ExciseNumberWorkItem] = {
    WorkItem(
      id = new ObjectId(),
      receivedAt = receivedAt,
      updatedAt = updatedAt,
      availableAt = availableAt,
      status = status,
      failureCount = failureCount,
      item = ExciseNumberWorkItem(ern, fastPollRetries)
    )
  }

  def getPreValidateTraderSuccessEISResponse: PreValidateTraderEISResponse = PreValidateTraderEISResponse(
    ExciseTraderValidationResponse(
      validationTimeStamp = "2021-12-17T09:31:123Z",
      exciseTraderResponse = Array(
        ExciseTraderResponse(
          exciseRegistrationNumber = "GBWK002281023",
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

  def getPreValidateTraderErrorEISResponse: PreValidateTraderEISResponse = PreValidateTraderEISResponse(
    ExciseTraderValidationResponse(
      validationTimeStamp = "2021-12-17T09:31:123Z",
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

  def getPreValidateTraderSuccessResponse: PreValidateTraderMessageResponse = PreValidateTraderMessageResponse(
    validationTimeStamp = "2021-12-17T09:31:123Z",
    exciseRegistrationNumber = "GBWK002281023",
    entityGroup = "UK Record",
    validTrader = true,
    traderType = Some("1"),
    validateProductAuthorisationResponse = Some(ValidateProductAuthorisationResponse(valid = true))
  )

  def getPreValidateTraderErrorResponse: PreValidateTraderMessageResponse = {
    PreValidateTraderMessageResponse(
      validationTimeStamp = "2021-12-17T09:31:123Z",
      validTrader = false,
      exciseRegistrationNumber = "GBWK000000000",
      traderType = None,
      entityGroup = "UK Record",
      errorCode = Some("6"),
      errorText = Some("Not Found"),
      validateProductAuthorisationResponse = Some(ValidateProductAuthorisationResponse(
        valid = false,
        productError = Some(Seq(ProductError(
          errorCode = "1",
          errorText = "Unrecognised EPC",
          exciseProductCode = "S200"
        )))
      ))
    )
  }

  def getPreValidateTraderRequest: PreValidateTraderRequest = PreValidateTraderRequest(
    ExciseTraderValidationRequest(
      ExciseTraderRequest(
        "GBWK002281023",
        "UK Record",
        Seq(ValidateProductAuthorisationRequest(ExciseProductCode("W200")))
      )
    )
  )
}
