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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, InternalServerError}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.PreValidateTraderConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.request.ParsedPreValidateTraderRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.preValidateTrader.response.{ExciseTraderResponse, ExciseTraderValidationResponse, PreValidateTraderEISResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PreValidateTraderService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.TestUtils._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class PreValidateTraderServiceSpec extends PlaySpec with BeforeAndAfterEach with EitherValues {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector = mock[PreValidateTraderConnector]
  private val dateTimeService = mock[DateTimeService]

  private val now = Instant.now
  when(dateTimeService.timestamp()).thenReturn(now)

  private val preValidateTraderService = new PreValidateTraderService(connector, dateTimeService)

  private val validRequest = ParsedPreValidateTraderRequest(
    EnrolmentRequest(FakeRequest(), Set("ern"), "id"),
    getPreValidateTraderRequest
  )

  private val getBadlyFormattedEISResponse = PreValidateTraderEISResponse(
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
          validateProductAuthorisationResponse = None
        ),
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


  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector)
  }


  "submitMessage" should {

    "return in the API response format if the Connector returns the EIS success response" in {

      when(connector.submitMessage(any, any)(any)).thenReturn(Future.successful(Right(getPreValidateTraderSuccessEISResponse)))

      val result = await(preValidateTraderService.submitMessage(validRequest))

      result.value mustBe getPreValidateTraderSuccessResponse

    }

    "return in the API response format if the Connector returns the EIS business error response" in {

      when(connector.submitMessage(any, any)(any)).thenReturn(Future.successful(Right(getPreValidateTraderErrorEISResponse)))

      val result = await(preValidateTraderService.submitMessage(validRequest))

      result.value mustBe getPreValidateTraderErrorResponse

    }

    "return an error result if EIS has returned an unexpected response" in {
      //E.g. we expect the ExciseTraderResponse array to only have one item in

      when(connector.submitMessage(any, any)(any)).thenReturn(Future.successful(Right(getBadlyFormattedEISResponse)))

      val result = await(preValidateTraderService.submitMessage(validRequest))

      result.left.value mustBe InternalServerError(Json.toJson(
        ErrorResponse(
          now,
          "PreValidateTrader Error",
          "Failed to parse preValidateTrader response"
        )
      ))

    }

    "return an error result if the Connector does" in {

      when(connector.submitMessage(any, any)(any)).thenReturn(Future.successful(Left(BadRequest("broken"))))

      val result = await(preValidateTraderService.submitMessage(validRequest))

      result.left.value mustBe BadRequest("broken")

    }

  }

}
