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

package uk.gov.hmrc.excisemovementcontrolsystemapi

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.{FORBIDDEN, OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InternalError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport


class DraftExciseMovementControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport with TestXml {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  override lazy val app: Application = {
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].to(authConnector),
      )
      .build()
  }

  "Draft Excise Movement" should {
    "return 200" in {
      withAuthorizedTrader

      println("Testing draft excise movement...")
      postRequest(validIE815XML).status mustBe OK
    }

    "return 403" ignore {
      withUnAuthorizedERN

      postRequest("").status mustBe FORBIDDEN
    }

    "return a 401" ignore {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      postRequest("").status mustBe  UNAUTHORIZED
    }
  }

  val validIE815XML = """<urn:IE815 xmlns:urn="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE815:V3.01" xmlns:urn1="urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:TMS:V3.01">
            |  <urn:Header>
            |    <urn1:MessageSender>token</urn1:MessageSender>
            |    <urn1:MessageRecipient>token</urn1:MessageRecipient>
            |    <urn1:DateOfPreparation>2011-02-05+00:00</urn1:DateOfPreparation>
            |    <urn1:TimeOfPreparation>16:09:40</urn1:TimeOfPreparation>
            |    <urn1:MessageIdentifier>token</urn1:MessageIdentifier>
            |    <!--Optional:-->
            |    <urn1:CorrelationIdentifier>token</urn1:CorrelationIdentifier>
            |  </urn:Header>
            |  <urn:Body>
            |    <urn:SubmittedDraftOfEADESAD>
            |      <urn:Attributes>
            |        <urn:SubmissionMessageType>2</urn:SubmissionMessageType>
            |        <!--Optional:-->
            |        <urn:DeferredSubmissionFlag>0</urn:DeferredSubmissionFlag>
            |      </urn:Attributes>
            |      <!--Optional:-->
            |      <urn:ConsigneeTrader language="to">
            |        <!--Optional:-->
            |        <urn:Traderid>token</urn:Traderid>
            |        <urn:TraderName>token</urn:TraderName>
            |        <urn:StreetName>token</urn:StreetName>
            |        <!--Optional:-->
            |        <urn:StreetNumber>token</urn:StreetNumber>
            |        <urn:Postcode>token</urn:Postcode>
            |        <urn:City>token</urn:City>
            |        <!--Optional:-->
            |        <urn:EoriNumber>token</urn:EoriNumber>
            |      </urn:ConsigneeTrader>
            |      <urn:ConsignorTrader language="to">
            |        <urn:TraderExciseNumber>tokentokentok</urn:TraderExciseNumber>
            |        <urn:TraderName>token</urn:TraderName>
            |        <urn:StreetName>token</urn:StreetName>
            |        <!--Optional:-->
            |        <urn:StreetNumber>token</urn:StreetNumber>
            |        <urn:Postcode>token</urn:Postcode>
            |        <urn:City>token</urn:City>
            |      </urn:ConsignorTrader>
            |      <!--Optional:-->
            |      <urn:PlaceOfDispatchTrader language="to">
            |        <!--Optional:-->
            |        <urn:ReferenceOfTaxWarehouse>tokentokentok</urn:ReferenceOfTaxWarehouse>
            |        <!--Optional:-->
            |        <urn:TraderName>token</urn:TraderName>
            |        <!--Optional:-->
            |        <urn:StreetName>token</urn:StreetName>
            |        <!--Optional:-->
            |        <urn:StreetNumber>token</urn:StreetNumber>
            |        <!--Optional:-->
            |        <urn:Postcode>token</urn:Postcode>
            |        <!--Optional:-->
            |        <urn:City>token</urn:City>
            |      </urn:PlaceOfDispatchTrader>
            |      <!--Optional:-->
            |      <urn:DispatchImportOffice>
            |        <urn:ReferenceNumber>tokentok</urn:ReferenceNumber>
            |      </urn:DispatchImportOffice>
            |      <!--Optional:-->
            |      <urn:ComplementConsigneeTrader>
            |        <urn:MemberStateCode>to</urn:MemberStateCode>
            |        <!--Optional:-->
            |        <urn:SerialNumberOfCertificateOfExemption>token</urn:SerialNumberOfCertificateOfExemption>
            |      </urn:ComplementConsigneeTrader>
            |      <!--Optional:-->
            |      <urn:DeliveryPlaceTrader language="to">
            |        <!--Optional:-->
            |        <urn:Traderid>token</urn:Traderid>
            |        <!--Optional:-->
            |        <urn:TraderName>token</urn:TraderName>
            |        <!--Optional:-->
            |        <urn:StreetName>token</urn:StreetName>
            |        <!--Optional:-->
            |        <urn:StreetNumber>token</urn:StreetNumber>
            |        <!--Optional:-->
            |        <urn:Postcode>token</urn:Postcode>
            |        <!--Optional:-->
            |        <urn:City>token</urn:City>
            |      </urn:DeliveryPlaceTrader>
            |      <!--Optional:-->
            |      <urn:DeliveryPlaceCustomsOffice>
            |        <urn:ReferenceNumber>tokentok</urn:ReferenceNumber>
            |      </urn:DeliveryPlaceCustomsOffice>
            |      <urn:CompetentAuthorityDispatchOffice>
            |        <urn:ReferenceNumber>tokentok</urn:ReferenceNumber>
            |      </urn:CompetentAuthorityDispatchOffice>
            |      <!--Optional:-->
            |      <urn:TransportArrangerTrader language="to">
            |        <!--Optional:-->
            |        <urn:VatNumber>token</urn:VatNumber>
            |        <urn:TraderName>token</urn:TraderName>
            |        <urn:StreetName>token</urn:StreetName>
            |        <!--Optional:-->
            |        <urn:StreetNumber>token</urn:StreetNumber>
            |        <urn:Postcode>token</urn:Postcode>
            |        <urn:City>token</urn:City>
            |      </urn:TransportArrangerTrader>
            |      <!--Optional:-->
            |      <urn:FirstTransporterTrader language="to">
            |        <!--Optional:-->
            |        <urn:VatNumber>token</urn:VatNumber>
            |        <urn:TraderName>token</urn:TraderName>
            |        <urn:StreetName>token</urn:StreetName>
            |        <!--Optional:-->
            |        <urn:StreetNumber>token</urn:StreetNumber>
            |        <urn:Postcode>token</urn:Postcode>
            |        <urn:City>token</urn:City>
            |      </urn:FirstTransporterTrader>
            |      <!--0 to 9 repetitions:-->
            |      <urn:DocumentCertificate>
            |        <!--Optional:-->
            |        <urn:DocumentType>toke</urn:DocumentType>
            |        <!--Optional:-->
            |        <urn:DocumentReference>token</urn:DocumentReference>
            |        <!--Optional:-->
            |        <urn:DocumentDescription language="to">token</urn:DocumentDescription>
            |        <!--Optional:-->
            |        <urn:ReferenceOfDocument language="to">token</urn:ReferenceOfDocument>
            |      </urn:DocumentCertificate>
            |      <urn:HeaderEadEsad>
            |        <urn:DestinationTypeCode>2</urn:DestinationTypeCode>
            |        <urn:JourneyTime>tok</urn:JourneyTime>
            |        <urn:TransportArrangement>3</urn:TransportArrangement>
            |      </urn:HeaderEadEsad>
            |      <urn:TransportMode>
            |        <urn:TransportModeCode>to</urn:TransportModeCode>
            |        <!--Optional:-->
            |        <urn:ComplementaryInformation language="to">token</urn:ComplementaryInformation>
            |      </urn:TransportMode>
            |      <urn:MovementGuarantee>
            |        <urn:GuarantorTypeCode>124</urn:GuarantorTypeCode>
            |        <!--0 to 2 repetitions:-->
            |        <urn:GuarantorTrader language="to">
            |          <!--Optional:-->
            |          <urn:TraderExciseNumber>tokentokentok</urn:TraderExciseNumber>
            |          <!--Optional:-->
            |          <urn:TraderName>token</urn:TraderName>
            |          <!--Optional:-->
            |          <urn:StreetName>token</urn:StreetName>
            |          <!--Optional:-->
            |          <urn:StreetNumber>token</urn:StreetNumber>
            |          <!--Optional:-->
            |          <urn:City>token</urn:City>
            |          <!--Optional:-->
            |          <urn:Postcode>token</urn:Postcode>
            |          <!--Optional:-->
            |          <urn:VatNumber>token</urn:VatNumber>
            |        </urn:GuarantorTrader>
            |      </urn:MovementGuarantee>
            |      <!--1 or more repetitions:-->
            |      <urn:BodyEadEsad>
            |        <urn:BodyRecordUniqueReference>tok</urn:BodyRecordUniqueReference>
            |        <urn:ExciseProductCode>toke</urn:ExciseProductCode>
            |        <urn:CnCode>tokentok</urn:CnCode>
            |        <urn:Quantity>1000.00000000000</urn:Quantity>
            |        <urn:GrossMass>1000.000000000000</urn:GrossMass>
            |        <urn:NetMass>1000.000000000000</urn:NetMass>
            |        <!--Optional:-->
            |        <urn:AlcoholicStrengthByVolumeInPercentage>1000.00</urn:AlcoholicStrengthByVolumeInPercentage>
            |        <!--Optional:-->
            |        <urn:DegreePlato>1000.00</urn:DegreePlato>
            |        <!--Optional:-->
            |        <urn:FiscalMark language="to">token</urn:FiscalMark>
            |        <!--Optional:-->
            |        <urn:FiscalMarkUsedFlag>1</urn:FiscalMarkUsedFlag>
            |        <!--Optional:-->
            |        <urn:DesignationOfOrigin language="to">token</urn:DesignationOfOrigin>
            |        <!--Optional:-->
            |        <urn:SizeOfProducer>token</urn:SizeOfProducer>
            |        <!--Optional:-->
            |        <urn:Density>1000.00</urn:Density>
            |        <!--Optional:-->
            |        <urn:CommercialDescription language="to">token</urn:CommercialDescription>
            |        <!--Optional:-->
            |        <urn:BrandNameOfProducts language="to">token</urn:BrandNameOfProducts>
            |        <!--Optional:-->
            |        <urn:MaturationPeriodOrAgeOfProducts language="to">token</urn:MaturationPeriodOrAgeOfProducts>
            |        <!--1 to 99 repetitions:-->
            |        <urn:Package>
            |          <urn:KindOfPackages>to</urn:KindOfPackages>
            |          <!--Optional:-->
            |          <urn:NumberOfPackages>token</urn:NumberOfPackages>
            |          <!--Optional:-->
            |          <urn:ShippingMarks>token</urn:ShippingMarks>
            |          <!--Optional:-->
            |          <urn:CommercialSealIdentification>token</urn:CommercialSealIdentification>
            |          <!--Optional:-->
            |          <urn:SealInformation language="to">token</urn:SealInformation>
            |        </urn:Package>
            |        <!--Optional:-->
            |        <urn:WineProduct>
            |          <urn:WineProductCategory>4</urn:WineProductCategory>
            |          <!--Optional:-->
            |          <urn:WineGrowingZoneCode>to</urn:WineGrowingZoneCode>
            |          <!--Optional:-->
            |          <urn:ThirdCountryOfOrigin>to</urn:ThirdCountryOfOrigin>
            |          <!--Optional:-->
            |          <urn:OtherInformation language="to">token</urn:OtherInformation>
            |          <!--0 to 99 repetitions:-->
            |          <urn:WineOperation>
            |            <urn:WineOperationCode>to</urn:WineOperationCode>
            |          </urn:WineOperation>
            |        </urn:WineProduct>
            |      </urn:BodyEadEsad>
            |      <urn:EadEsadDraft>
            |        <urn:LocalReferenceNumber>token</urn:LocalReferenceNumber>
            |        <urn:InvoiceNumber>token</urn:InvoiceNumber>
            |        <!--Optional:-->
            |        <urn:InvoiceDate>2008-11-25+00:00</urn:InvoiceDate>
            |        <urn:OriginTypeCode>2</urn:OriginTypeCode>
            |        <urn:DateOfDispatch>2014-01-25</urn:DateOfDispatch>
            |        <!--Optional:-->
            |        <urn:TimeOfDispatch>07:28:11+00:00</urn:TimeOfDispatch>
            |        <!--0 to 9 repetitions:-->
            |        <urn:ImportSad>
            |          <urn:ImportSadNumber>token</urn:ImportSadNumber>
            |        </urn:ImportSad>
            |      </urn:EadEsadDraft>
            |      <!--1 to 99 repetitions:-->
            |      <urn:TransportDetails>
            |        <urn:TransportUnitCode>to</urn:TransportUnitCode>
            |        <!--Optional:-->
            |        <urn:IdentityOfTransportUnits>token</urn:IdentityOfTransportUnits>
            |        <!--Optional:-->
            |        <urn:CommercialSealIdentification>token</urn:CommercialSealIdentification>
            |        <!--Optional:-->
            |        <urn:ComplementaryInformation language="to">token</urn:ComplementaryInformation>
            |        <!--Optional:-->
            |        <urn:SealInformation language="to">token</urn:SealInformation>
            |      </urn:TransportDetails>
            |    </urn:SubmittedDraftOfEADESAD>
            |  </urn:Body>
            |</urn:IE815>""".stripMargin

  //scala.xml.XML.loadString(body)
  private def postRequest(body : String) = {
    await(wsClient.url(s"http://localhost:$port/customs/excise/movements")
      .addHttpHeaders(
        "Authorization" -> "TOKEN",
        "Content-Type" -> "application/xml"
      ).post(validIE815XML)

    )
  }
}
