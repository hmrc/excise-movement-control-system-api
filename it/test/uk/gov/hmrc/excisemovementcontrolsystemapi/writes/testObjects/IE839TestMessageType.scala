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

package uk.gov.hmrc.excisemovementcontrolsystemapi.writes.testObjects
import play.api.libs.json.{JsValue, Json}

object IE839TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse(
    """
      |{
      |    "Header": {
      |        "MessageSender": "NDEA.XI",
      |        "MessageRecipient": "NDEA.FR",
      |        "DateOfPreparation": "2024-06-26",
      |        "TimeOfPreparation": "00:18:33",
      |        "MessageIdentifier": "XI004322",
      |        "CorrelationIdentifier": "6dddas1231ff3a678fefffff3233"
      |    },
      |    "Body": {
      |        "RefusalByCustoms": {
      |            "AttributesValue": {
      |                "DateAndTimeOfIssuance": "2023-06-24T18:27:14"
      |            },
      |            "ConsigneeTrader": {
      |                "Traderid": "AT00000612158",
      |                "TraderName": "Chaz's Cigars",
      |                "StreetName": "The Street",
      |                "StreetNumber": "token",
      |                "Postcode": "MC232",
      |                "City": "Happy Town",
      |                "EoriNumber": "91",
      |                "attributes": {
      |                    "@language": "en"
      |                }
      |            },
      |            "ExportPlaceCustomsOffice": {
      |                "ReferenceNumber": "FR883393"
      |            },
      |            "Rejection": {
      |                "RejectionDateAndTime": "2009-05-16T13:42:28",
      |                "RejectionReasonCode": "3"
      |            },
      |            "ExportDeclarationInformation": {
      |                "LocalReferenceNumber": "lrnie8155755329",
      |                "DocumentReferenceNumber": "123",
      |                "NegativeCrosscheckValidationResults": [
      |                    {
      |                        "UbrCrosscheckResult": [
      |                            {
      |                                "AdministrativeReferenceCode": "23XI00000000000056341",
      |                                "BodyRecordUniqueReference": "1",
      |                                "DiagnosisCode": "4",
      |                                "ValidationResult": "1",
      |                                "RejectionReason": "token",
      |                                "CombinedNomenclatureCodeCrosscheckResult": {
      |                                    "ValidationResult": "1",
      |                                    "RejectionReason": "token"
      |                                },
      |                                "NetMassCrosscheckResult": {
      |                                    "ValidationResult": "1",
      |                                    "RejectionReason": "token"
      |                                }
      |                            }
      |                        ]
      |                    }
      |                ],
      |                "NNonDes": {
      |                    "DocumentReferenceNumber": "token"
      |                }
      |            },
      |            "CEadVal": [
      |                {
      |                    "AdministrativeReferenceCode": "23XI00000000000056341",
      |                    "SequenceNumber": "1"
      |                }
      |            ],
      |            "NEadSub": {
      |                "LocalReferenceNumber": "lrnie8155755329"
      |            }
      |        }
      |    }
      |}
      |""".stripMargin)
}
