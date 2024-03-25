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
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import scalaxb.ParserFailure
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.{IEMessageFactory, IEMessageFactoryException}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.ExecutionContext

class ParseXmlActionSpec
  extends PlaySpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val messageFactory = mock[IEMessageFactory]
  private val dateTimeService = mock[DateTimeService]
  private val message = mock[IEMessage]
  private val parserXmlAction = new ParseXmlActionImpl(
    messageFactory,
    dateTimeService,
    stubControllerComponents()
  )
  private val timestamp = Instant.parse("2023-05-11T01:01:01.987654Z")

  private val xmlStr =
    """<IE815>
      | <body></body>
      |</IE815>""".stripMargin

  private val scalaxbExceptionMessage: String = "Error while parsing <urn:SubmittedDraftOfEADESAD xmlns:urn=\\\"urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13\\\" xmlns:urn1=\\\"urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.13\\\" xmlns:soapenv=\\\"http://www.w3.org/2003/05/soap-envelope\\\" xmlns=\\\"http://www.hmrc.gov.uk/ChRIS/Service/Control\\\" xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\">\\n        <urn:Attributes>\\n          <urn:SubmissionMessageType>1</urn:SubmissionMessageType>\\n        </urn:Attributes>\\n        <urn:ConsigneeTrader language=\\\"en\\\">\\n          <urn:Traderid>GBWKQOZ8OVLYR</urn:Traderid>\\n          <urn:TraderName>WFlgUjfC</urn:TraderName>\\n          <urn:StreetName>xoL0NsNyDi</urn:StreetName>\\n          <urn:StreetNumber>67</urn:StreetNumber>\\n          <urn:Postcode>A1 1AA</urn:Postcode>\\n          <urn:City>l8WSaHS9</urn:City>\\n        </urn:ConsigneeTrader>\\n        <urn:ConsignorTrader language=\\\"en\\\">\\n          <urn:TraderExciseNumber>GBWKQOZ8OVLYS</urn:TraderExciseNumber>\\n          <urn:TraderName>Company PLC</urn:TraderName>\\n          <urn:StreetName>msfvZUL1Oe</urn:StreetName>\\n          <urn:StreetNumber>25</urn:StreetNumber>\\n          <urn:Postcode>A1 1AA</urn:Postcode>\\n          <urn:City>QDHwPa61</urn:City>\\n        </urn:ConsignorTrader>\\n        <urn:PlaceOfDispatchTrader language=\\\"en\\\">\\n          <urn:ReferenceOfTaxWarehouse>GB00DO459DMNX</urn:ReferenceOfTaxWarehouse>\\n          <urn:TraderName>2z0waekA</urn:TraderName>\\n          <urn:StreetName>MhO1XtDIVr</urn:StreetName>\\n          <urn:StreetNumber>25</urn:StreetNumber>\\n          <urn:Postcode>A1 1AA</urn:Postcode>\\n          <urn:City>zPCc6skm</urn:City>\\n        </urn:PlaceOfDispatchTrader>\\n        <urn:DeliveryPlaceTrader language=\\\"en\\\">\\n          <urn:Traderid>GB00AIP67RAO3</urn:Traderid>\\n          <urn:TraderName>BJpWdv2N</urn:TraderName>\\n          <urn:StreetName>C24vvUqCw6</urn:StreetName>\\n          <urn:StreetNumber>43</urn:StreetNumber>\\n          <urn:Postcode>A1 1AA</urn:Postcode>\\n          <urn:City>A9ZlElxP</urn:City>\\n        </urn:DeliveryPlaceTrader>\\n        <urn:CompetentAuthorityDispatchOffice>\\n          <urn:ReferenceNumber>GB004098</urn:ReferenceNumber>\\n        </urn:CompetentAuthorityDispatchOffice>\\n        <urn:FirstTransporterTrader language=\\\"en\\\">\\n          <urn:VatNumber>123798354</urn:VatNumber>\\n          <urn:TraderName>Mr Delivery place trader 4</urn:TraderName>\\n          <urn:StreetName>Delplace Avenue</urn:StreetName>\\n          <urn:StreetNumber>05</urn:StreetNumber>\\n          <urn:Postcode>FR5 4RN</urn:Postcode>\\n          <urn:City>Delville</urn:City>\\n        </urn:FirstTransporterTrader>\\n        <urn:DocumentCertificate>\\n          <urn:DocumentType>9</urn:DocumentType>\\n          <urn:DocumentReference>DPdQsYktZEJEESpc7b32Ig0U6B34XmHmfZU</urn:DocumentReference>\\n        </urn:DocumentCertificate>\\n        <urn:HeaderEadEsad>\\n          <urn:DestinationTypeCode>1</urn:DestinationTypeCode>\\n          <urn:JourneyTime>D07</urn:JourneyTime>\\n          <urn:TransportArrangement>1</urn:TransportArrangement>\\n        </urn:HeaderEadEsad>\\n        <urn:TransportMode>\\n          <urn:TransportModeCode>3</urn:TransportModeCode>\\n        </urn:TransportMode>\\n        <urn:MovementGuarantee>\\n          <urn:GuarantorTypeCode>1</urn:GuarantorTypeCode>\\n        </urn:MovementGuarantee>\\n        <urn:BodyEadEsad>\\n          <urn:BodyRecordUniqueReference>1</urn:BodyRecordUniqueReference>\\n          <urn:ExciseProductCode>B000</urn:ExciseProductCode>\\n          <urn:CnCode>22030001</urn:CnCode>\\n          <urn:Quantity>2000</urn:Quantity>\\n          <urn:GrossMass>20000</urn:GrossMass>\\n          <urn:NetMass>19999</urn:NetMass>\\n          <urn:AlcoholicStrengthByVolumeInPercentage>0.5</urn:AlcoholicStrengthByVolumeInPercentage>\\n          <urn:FiscalMarkUsedFlag>0</urn:FiscalMarkUsedFlag>\\n          <urn:Package>\\n            <urn:KindOfPackages>BA</urn:KindOfPackages>\\n            <urn:NumberOfPackages>2</urn:NumberOfPackages>\\n          </urn:Package>\\n        </urn:BodyEadEsad>\\n\\n        <urn:TransportDetails>\\n          <urn:TransportUnitCode>1</urn:TransportUnitCode>\\n          <urn:IdentityOfTransportUnits>100</urn:IdentityOfTransportUnits>\\n        </urn:TransportDetails>\\n      </urn:SubmittedDraftOfEADESAD>: parser error \\\"'{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}EadEsadDraft' expected but {urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}TransportDetails found\\\" while parsing /{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}IE815/{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}Body/{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}SubmittedDraftOfEADESAD/{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}Attributes{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}ConsigneeTrader{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}ConsignorTrader{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}PlaceOfDispatchTrader{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}DeliveryPlaceTrader{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}CompetentAuthorityDispatchOffice{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}FirstTransporterTrader{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}DocumentCertificate{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}HeaderEadEsad{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}TransportMode{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}MovementGuarantee{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}BodyEadEsad{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}TransportDetails\\n"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(messageFactory, dateTimeService, message)

    when(dateTimeService.timestamp()).thenReturn(timestamp)
    when(messageFactory.createFromXml(any, any)).thenReturn(message)
  }

  "refine" should {
    "return a ParsedXmlRequest" in {
      val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xml.XML.loadString(xmlStr)), Set("ern"), "123")

      val result = parserXmlAction.refine(enrolmentRequest).futureValue

      result mustBe Right(ParsedXmlRequest(enrolmentRequest, message, Set("ern"), "123"))
    }

    "return an error" when {
      "no xml format was sent" in {
        val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xmlStr), Set("ern"), "123")

        val result = parserXmlAction.refine(enrolmentRequest).futureValue

        val expectedError = ErrorResponse(timestamp, "XML error", "Not valid XML or XML is empty")
        result.left.value mustBe BadRequest(Json.toJson(expectedError))
      }

      "cannot create message from xml" when {

        "a parser error that can be simplified" in {
          when(messageFactory.createFromXml(any, any)).thenThrow(new ParserFailure(scalaxbExceptionMessage))
          val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xml.XML.loadString(xmlStr)), Set("ern"), "123")

          val result = parserXmlAction.refine(enrolmentRequest).futureValue

          val expectedError = ErrorResponse(timestamp, "Not valid IE815 message",
            "Parser error: \\\"'{urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}EadEsadDraft' expected but {urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13}TransportDetails found\\\"")


          result.left.value mustBe BadRequest(Json.toJson(expectedError))
        }

        "a parser error which isn't simplified" in {
          val parserError = "Error while parsing <urn:DateOfDispatch xmlns:urn=\"urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.13\" xmlns:urn1=\"urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.13\">lrnGBWKQOZ8OMCWS1</urn:DateOfDispatch>: java.lang.IllegalArgumentException: lrnGBWKQOZ8OMCWS1"

          when(messageFactory.createFromXml(any, any)).thenThrow(new ParserFailure(parserError))
          val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xml.XML.loadString(xmlStr)), Set("ern"), "123")

          val result = parserXmlAction.refine(enrolmentRequest).futureValue

          val expectedError = ErrorResponse(timestamp, "Not valid IE815 message", parserError)

          result.left.value mustBe BadRequest(Json.toJson(expectedError))
        }

        "IEMessageFactory exception" in {
          when(messageFactory.createFromXml(any, any)).thenThrow(new IEMessageFactoryException("Error doing message factory things"))
          val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xml.XML.loadString(xmlStr)), Set("ern"), "123")

          val result = parserXmlAction.refine(enrolmentRequest).futureValue

          val expectedError = ErrorResponse(timestamp, "Not valid IE815 message", "Error doing message factory things")
          result.left.value mustBe BadRequest(Json.toJson(expectedError))
        }

        "some generic exception" in {
          when(messageFactory.createFromXml(any, any)).thenThrow(new Exception(scalaxbExceptionMessage))
          val enrolmentRequest = EnrolmentRequest(FakeRequest().withBody(xml.XML.loadString(xmlStr)), Set("ern"), "123")

          val result = parserXmlAction.refine(enrolmentRequest).futureValue

          val expectedError = ErrorResponse(timestamp, "Not valid IE815 message", "Error occurred parsing message")
          result.left.value mustBe BadRequest(Json.toJson(expectedError))
        }

      }
    }
  }

}
