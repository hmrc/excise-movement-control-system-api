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

import dispatch.Future
import generated.IE818Type
import org.mockito.MockitoSugar.when
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.FORBIDDEN
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth._
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{EmcsUtils, ErrorResponse}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext


class ValidateConsignorActionIE818Spec extends PlaySpec with TestXml with EitherValues with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val eisUtils: EmcsUtils = mock[EmcsUtils]

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(eisUtils.getCurrentDateTime)
      .thenReturn(LocalDateTime.of(2023, 10, 18, 15, 33, 33))
  }

  val sut = new ValidateConsignorActionIE818Impl()

  "ValidateConsignorActionIE818Spec" should {
    "return a request" in {

      val ie818Obj = scalaxb.fromXML[IE818Type](IE818)
      val erns = Set("GBWK002281023", "GBWK002181023", "GBWK002281022")
      val authorizedRequest = EnrolmentRequest(FakeRequest(), erns, "123")
      val request = ParsedXmlRequestIE818(authorizedRequest, ie818Obj, erns, "123")

      val result = await(sut.refine(request))

      val dataRequest = result.toOption.get
      dataRequest.internalId mustBe "123"
      dataRequest.movementMessage.consigneeId mustBe "GBWK002281023"
    }

    "an error" when {
      "ern does not match the consignorId" in {
        val ie818Obj = scalaxb.fromXML[IE818Type](IE818)
        val authorizedRequest = EnrolmentRequest(FakeRequest(), Set("12356"), "123")
        val request = ParsedXmlRequestIE818(authorizedRequest, ie818Obj, Set("12356"), "123")

        val result = await(sut.refine(request))

        result match {
          case Left(error: Result) =>
            error.header.status mustBe FORBIDDEN
            val response = contentAsJson(Future.successful(error)).validate[ErrorResponse].asEither

            response.map {
              response =>
                response.message mustBe "ERN validation error"
                response.debugMessage mustBe "Excise number in message does not match authenticated excise number"
            }

          case _ => fail("Should have an error")
        }

      }
    }
  }
}
