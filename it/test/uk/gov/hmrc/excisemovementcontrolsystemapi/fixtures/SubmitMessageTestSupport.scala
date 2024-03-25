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

package uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures

trait SubmitMessageTestSupport {

  val locationWithControlDoc = "/con:Control[1]/con:OperationRequest[1]/con:Parameters[1]/con:Parameter[1]/urn:IE815[1]/urn:Body[1]/urn:SubmittedDraftOfEADESAD[1]/urn:EadEsadDraft[1]/urn:DateOfDispatch[1]"
  val locationWithoutControlDoc = "/urn:IE815[1]/urn:Body[1]/urn:SubmittedDraftOfEADESAD[1]/urn:EadEsadDraft[1]/urn:DateOfDispatch[1]"

  def rimValidationErrorResponse(errorLocation: String): String = {
    s"""{
       |   "emcsCorrelationId": "correlationId",
       |   "message": ["Validation error(s) occurred"],
       |   "validatorResults":[
       |      {
       |        "errorCategory": "business",
       |        "errorType":8084,
       |        "errorReason":"The Date of Dispatch you entered is incorrect. It must be today or later. Please amend your entry and resubmit.",
       |        "errorLocation": "$errorLocation",
       |        "originalAttributeValue":"2023-12-05"
       |      }
       |   ]
       |}""".stripMargin
  }

  def validationErrorResponse(errorLocation: String, timestamp: String): String = {
    s"""{
       |   "dateTime": "$timestamp",
       |   "message": "Validation error",
       |   "debugMessage": "Validation error(s) occurred",
       |   "correlationId": "correlationId",
       |   "validatorResults":[
       |      {
       |        "errorCategory": "business",
       |        "errorType":8084,
       |        "errorReason":"The Date of Dispatch you entered is incorrect. It must be today or later. Please amend your entry and resubmit.",
       |        "errorLocation": "$errorLocation",
       |        "originalAttributeValue":"2023-12-05"
       |      }
       |   ]
       |}""".stripMargin
  }
}
