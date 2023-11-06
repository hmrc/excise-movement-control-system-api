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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.{NotFound, Ok}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}


class ValidateLRNActionSpec
  extends PlaySpec
    with EitherValues
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val emcsUtils: EmcsUtils = mock[EmcsUtils]
  private val currentDateTime = LocalDateTime.of(2023, 10, 18, 15, 33, 33)
  private val ieMessage = mock[IEMessage]
  private val movementService = mock[MovementService]
  private val sut = new ValidateLRNImpl(movementService, emcsUtils, stubMessagesControllerComponents())

  def block(authRequest: ValidatedXmlRequest[_]) =
    Future.successful(Ok)

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(ieMessage.consigneeId).thenReturn(Some("GBWK002181023"))
    when(emcsUtils.getCurrentDateTime).thenReturn(currentDateTime)
  }

  "ValidateLRNActionSpec" should {
    "return a request when valid LRN/ERN combo in database" in {

      when(movementService.getMovementMessagesByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Some(Movement("lrn", "consignorId", None))))

      val erns = Set("GBWK002281023", "GBWK002181023", "GBWK002281022")
      val request = ValidatedXmlRequest(ParsedXmlRequest(EnrolmentRequest(FakeRequest(), erns, "123"),
        ieMessage, erns, "123"), erns)

      val block = (actual: ValidatedXmlRequest[_]) => {
        actual mustBe request
        Future.successful(Ok)
      }

      val result = await(sut.apply("lrn").invokeBlock(request, block))

      result mustBe Ok
    }

    "an error" when {
      "LRN/ERN combo is not in the db" in {
        when(movementService.getMovementMessagesByLRNAndERNIn(any, any))
          .thenReturn(Future.successful(None))

        when(ieMessage.consigneeId).thenReturn(Some("12356"))
        val request = ValidatedXmlRequest(ParsedXmlRequest(EnrolmentRequest(FakeRequest(), Set("12356"), "123"),
          ieMessage, Set("12356"), "123"), Set("12356"))

        val result = await(sut.apply("lrn").invokeBlock(request, block))

        result mustBe NotFound(Json.toJson(ErrorResponse(
          currentDateTime,
          "Local reference number not found",
          "Local reference number lrn is not found within the data for ERNs 12356"))
        )
      }

      "DB error occurs" in {
        when(movementService.getMovementMessagesByLRNAndERNIn(any, any))
          .thenReturn(Future.failed(new RuntimeException("error")))
        when(ieMessage.consigneeId).thenReturn(Some("12356"))

        val request = ValidatedXmlRequest(ParsedXmlRequest(EnrolmentRequest(FakeRequest(), Set("12356"), "123"),
          ieMessage, Set("12356"), "123"), Set("12356"))

        intercept[RuntimeException] {
          await(sut.apply("lrn").invokeBlock(request, block))
        }.getMessage mustBe "error"

      }
    }
  }
}
