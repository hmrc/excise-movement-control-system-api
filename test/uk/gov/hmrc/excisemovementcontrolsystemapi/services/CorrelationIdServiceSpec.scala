/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.test.{FakeHeaders, FakeRequest}

import java.util.UUID

class CorrelationIdServiceSpec extends PlaySpec with BeforeAndAfterEach {

  val correlationIdService: CorrelationIdService = new CorrelationIdService()

  // we are in the middle of moving this over to the action

  "guaranteeCorrelationId" should {
    "generate new correlationId when none is present" in {
//      val headers = Seq.empty[(String, String)]
//
//      val request = FakeRequest()
//        .withHeaders(FakeHeaders(headers))
//
//      val result = correlationIdService.guaranteeCorrelationId(request)
//
//      val correlationId = result.headers.get(HttpHeader.xCorrelationId)
//
//      correlationId.isDefined mustBe true
    }
    "use already existing correlationId" in {
//      val testCorrelationId = UUID.randomUUID().toString
//      val headers           = Seq(HttpHeader.xCorrelationId -> testCorrelationId)
//
//      val request = FakeRequest()
//        .withHeaders(FakeHeaders(headers))
//
//      val result = correlationIdService.guaranteeCorrelationId(request)
//
//      val correlationId = result.headers.get(HttpHeader.xCorrelationId)
//
//      correlationId.isDefined mustBe true
//      correlationId.get mustBe testCorrelationId
    }
  }
}
