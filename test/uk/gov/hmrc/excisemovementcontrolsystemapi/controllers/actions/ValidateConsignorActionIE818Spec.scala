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

import generated.{IE815Type, IE818Type}
import org.scalatest.EitherValues
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Forbidden
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth._

import scala.concurrent.ExecutionContext


class ValidateConsignorActionIE818Spec extends PlaySpec with TestXml with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val sut = new ValidateConsignorActionIE818Impl()

  "ValidateConsignorActionIE818Spec" should {
    "return a request" in {

      val ie818Obj = scalaxb.fromXML[IE818Type](IE818)
      val erns = Set("GBWK002281023", "GBWK002181023", "GBWK002281022")
      val authorizedRequest = EnrolmentRequest(FakeRequest(), erns, "123")
      val request = ParsedXmlRequestIE818(authorizedRequest, ie818Obj, erns, "123");

      val result = await(sut.refine(request))

      val dataRequest = result.toOption.get
      dataRequest.internalId mustBe "123"
      dataRequest.movementMessage.consigneeId mustBe Some("GBWK002281023")
    }

    "an error" when {
      "ern does not must tye consignorId" in {
        val ie818Obj = scalaxb.fromXML[IE818Type](IE818)
        val authorizedRequest = EnrolmentRequest(FakeRequest(), Set("12356"), "123")
        val request = ParsedXmlRequestIE818(authorizedRequest, ie818Obj, Set("12356"), "123");

        val result = await(sut.refine(request))

        result mustBe Left(Forbidden("Invalid Excise Number"))
      }
    }
  }
}
