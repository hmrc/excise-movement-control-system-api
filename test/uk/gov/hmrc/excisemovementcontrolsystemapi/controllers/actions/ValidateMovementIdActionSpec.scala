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
import play.api.mvc.Results
import play.api.mvc.Results.{NotFound, Ok}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{EmcsUtils, ErnsMapper}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}


class ValidateMovementIdActionSpec
  extends PlaySpec
    with EitherValues
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val emcsUtils: EmcsUtils = mock[EmcsUtils]
  private val currentDateTime = LocalDateTime.of(2023, 10, 18, 15, 33, 33)
  private val ieMessage = mock[IEMessage]
  private val movementService = mock[MovementService]
  private val ernsMapper = mock[ErnsMapper]
  private val sut = new ValidateMovementIdActionImpl(movementService, emcsUtils, ernsMapper, stubMessagesControllerComponents())

  def defaultBlock(authRequest: ValidatedXmlRequest[_]): Future[Results.Status] =
    Future.successful(Ok)

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(ieMessage.consigneeId).thenReturn(Some("GBWK002181023"))
    when(emcsUtils.getCurrentDateTime).thenReturn(currentDateTime)
    when(ernsMapper.getSingleErnFromMessage(any,any)).thenReturn("GBWK002281023")
  }

  "ValidateMovementIdAction" should {
    "return a request when id is in database and matches the ern" in {

      when(movementService.getMovementById(any))
        .thenReturn(Future.successful(Some(Movement("lrn", "consignorId", None))))

      val erns = Set("GBWK002281023", "GBWK002181023", "GBWK002281022")
      val request = ValidatedXmlRequest(
        ParsedXmlRequest(
          EnrolmentRequest(FakeRequest(), erns, "123"),
          ieMessage,
          erns, "123"
        ),
        erns)

      val checkResponseMatchesRequestBlock = (actual: ValidatedXmlRequest[_]) => {
        actual mustBe request
        Future.successful(Ok)
      }

      val result = await(sut.apply("insertUUIDhere").invokeBlock(request, checkResponseMatchesRequestBlock))

      result mustBe Ok
    }

    "return an error when id is not in database" in {

      when(movementService.getMovementById(any))
        .thenReturn(Future.successful(None))

      val erns = Set("GBWK002281023", "GBWK002181023", "GBWK002281022")
      val request = ValidatedXmlRequest(
        ParsedXmlRequest(
          EnrolmentRequest(FakeRequest(), erns, "123"),
          ieMessage,
          erns, "123"
        ),
        erns)

      val result = await(sut.apply("uuid1").invokeBlock(request, defaultBlock))

      result mustBe NotFound(Json.toJson(ErrorResponse(
        currentDateTime,
        "Movement not found",
        "Movement uuid1 is not found within the data"))
      )
    }

    "return an error when found in database but not matching any logged in ern" in {

      when(movementService.getMovementById(any))
        .thenReturn(Future.successful(Some(Movement("lrn", "GBWK002281029", Some("GBWK002281039")))))

      when(ernsMapper.getSingleErnFromMessage(any,any)).thenReturn("GBWK002281029")

      val erns = Set("GBWK002281023", "GBWK002181023", "GBWK002281022")
      val request = ValidatedXmlRequest(
        ParsedXmlRequest(
          EnrolmentRequest(FakeRequest(), erns, "123"),
          ieMessage,
          erns, "123"
        ),
        erns)

      val result = await(sut.apply("uuid1").invokeBlock(request, defaultBlock))

      result mustBe NotFound(Json.toJson(ErrorResponse(
        currentDateTime,
        "Movement not found",
        "Movement uuid1 is not found within the data for ERNs GBWK002281023/GBWK002181023/GBWK002281022"))
      )
    }

    //    "an error" when {
    //      "LRN/ERN combo is not in the db" in {
    //        when(movementService.getMovementByLRNAndERNIn(any, any))
    //          .thenReturn(Future.successful(None))
    //
    //        when(ieMessage.consigneeId).thenReturn(Some("12356"))
    //        val request = ValidatedXmlRequest(
    //          ParsedXmlRequest(
    //            EnrolmentRequest(FakeRequest(), Set("12356"), "123"),
    //            ieMessage,
    //            Set("12356"),
    //            "123"
    //          ),
    //          Set("12356"))
    //
    //        val result = await(sut.apply("lrn").invokeBlock(request, defaultBlock))
    //
    //        result mustBe NotFound(Json.toJson(ErrorResponse(
    //          currentDateTime,
    //          "Local reference number not found",
    //          "Local reference number lrn is not found within the data for ERNs 12356"))
    //        )
    //      }
    //
    //      "DB error occurs" in {
    //        when(movementService.getMovementByLRNAndERNIn(any, any))
    //          .thenReturn(Future.failed(new RuntimeException("error")))
    //        when(ieMessage.consigneeId).thenReturn(Some("12356"))
    //
    //        val request = ValidatedXmlRequest(
    //          ParsedXmlRequest(
    //            EnrolmentRequest(FakeRequest(), Set("12356"), "123"),
    //            ieMessage,
    //            Set("12356"),
    //            "123"
    //          ),
    //          Set("12356"))
    //
    //        intercept[RuntimeException] {
    //          await(sut.apply("lrn").invokeBlock(request, defaultBlock))
    //        }.getMessage mustBe "error"
    //
    //      }
    //    }
  }
}
