package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.UserDetails
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IE815Message
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader

import java.util.UUID
import scala.concurrent.ExecutionContext

class CorrelationIdActionSpec  extends PlaySpec
  with ScalaFutures
  with EitherValues
  with BeforeAndAfterEach
  with TestXml {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "refine" should {
    "return a ParsedXmlRequest with existing correlationId when one is provided" in {
      val xmlStr =
        """<IE815>
          | <body></body>
          |</IE815>""".stripMargin

      val testCorrelationId = "testCorrelationId"
      val headers           = Seq(HttpHeader.xCorrelationId -> testCorrelationId)

      val enrolmentRequest = EnrolmentRequest(
        FakeRequest()
          .withHeaders(FakeHeaders(headers))
          .withBody(xml.XML.loadString(xmlStr)), Set("ern"), UserDetails("123", "abc"))

      val message = IE815Message.createFromXml(IE815)

      val correlationIdAction = new CorrelationIdAction

      val inputRequest =
        ParsedXmlRequest(enrolmentRequest, message,  Set("ern"), UserDetails("123", "abc"))

      val result = correlationIdAction.transform(inputRequest).futureValue

      result mustBe inputRequest
    }
    "return a ParsedXmlRequest with new correlationId when none is provided" in {
      val xmlStr =
        """<IE815>
          | <body></body>
          |</IE815>""".stripMargin

      val testCorrelationId = "testCorrelationId"
      val headers           = Seq(HttpHeader.xCorrelationId -> testCorrelationId)

      val enrolmentRequest = EnrolmentRequest(
        FakeRequest()
          .withHeaders()
          .withBody(xml.XML.loadString(xmlStr)), Set("ern"), UserDetails("123", "abc"))

      val message = IE815Message.createFromXml(IE815)

      val correlationIdAction = new CorrelationIdAction

      val inputRequest =
        ParsedXmlRequest(enrolmentRequest, message,  Set("ern"), UserDetails("123", "abc"))

      val result = correlationIdAction.transform(inputRequest).futureValue

      val expectedRequest =
        ParsedXmlRequest(enrolmentRequest, message,  Set("ern"), UserDetails("123", "abc"))
      result mustBe ???
    }
  }
}
