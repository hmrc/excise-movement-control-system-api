package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.test.{FakeHeaders, FakeRequest}

import java.util.UUID

class CorrelationIdServiceSpec extends PlaySpec with BeforeAndAfterEach {

  val correlationIdService: CorrelationIdService = new CorrelationIdService()

  "guaranteeCorrelationId" should {
    "generate new correlationId when none is present" in {
      val headers = Seq.empty[(String, String)]

      val request = FakeRequest()
        .withHeaders(FakeHeaders(headers))

      val result = correlationIdService.guaranteeCorrelationId(request)

      val correlationId = result.headers.get(HttpHeader.xCorrelationId)

      correlationId.isDefined mustBe true
    }
    "use already existing correlationId" in {
      val testCorrelationId = UUID.randomUUID().toString
      val headers           = Seq(HttpHeader.xCorrelationId -> testCorrelationId)

      val request = FakeRequest()
        .withHeaders(FakeHeaders(headers))

      val result = correlationIdService.guaranteeCorrelationId(request)

      val correlationId = result.headers.get(HttpHeader.xCorrelationId)

      correlationId.isDefined mustBe true
      correlationId.get mustBe testCorrelationId
    }
  }
}
