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

package uk.gov.hmrc.excisemovementcontrolsystemapi.writes.testObjects
import play.api.libs.json.{JsValue, Json}

object IE829TestMessageType extends TestMessageType {

  override def json1: JsValue = Json.parse("""
      |{
      |    "Body": {
      |        "NotificationOfAcceptedExport": {
      |            "AttributesValue": {
      |                "DateAndTimeOfIssuance": "2024-06-26T09:14:54"
      |            },
      |            "ConsigneeTrader": {
      |                "attributes": {
      |                    "@language": "en"
      |                },
      |                "City": "Happy Town",
      |                "EoriNumber": "7",
      |                "Postcode": "MC232",
      |                "StreetName": "The Street",
      |                "StreetNumber": "token",
      |                "Traderid": "AT00000612157",
      |                "TraderName": "Whale Oil Lamps Co."
      |            },
      |            "ExciseMovementEad": [
      |                {
      |                    "AdministrativeReferenceCode": "23XI00000000000056339",
      |                    "SequenceNumber": "1"
      |                },
      |                {
      |                    "AdministrativeReferenceCode": "23XI00000000000056340",
      |                    "SequenceNumber": "1"
      |                }
      |            ],
      |            "ExportDeclarationAcceptanceRelease": {
      |                "DateOfAcceptance": "2013-05-22+01:00",
      |                "DateOfRelease": "2002-11-05Z",
      |                "DocumentReferenceNumber": "token",
      |                "IdentificationOfSenderCustomsOfficer": "token",
      |                "ReferenceNumberOfSenderCustomsOffice": "tokentok",
      |                "ExportDeclarationAcceptanceOrGoodsReleasedForExport": "1"
      |            },
      |            "ExportPlaceCustomsOffice": {
      |                "ReferenceNumber": "tokentok"
      |            }
      |        }
      |    },
      |    "Header": {
      |        "CorrelationIdentifier": "PORTAL6de1b822562c43fb9220d236e487c920",
      |        "DateOfPreparation": "2023-06-26",
      |        "MessageIdentifier": "XI004321B",
      |        "MessageRecipient": "NDEA.AT",
      |        "MessageSender": "NDEA.XI",
      |        "TimeOfPreparation": "09:15:33"
      |    }
      |}
      |""".stripMargin)

  override def auditEvent: JsValue = Json.parse("""
      |{
      |  "messageCode": "IE829",
      |  "content": {
      |    "Header": {
      |      "MessageSender": "NDEA.XI",
      |      "MessageRecipient": "NDEA.AT",
      |      "DateOfPreparation": "2023-06-26",
      |      "TimeOfPreparation": "09:15:33",
      |      "MessageIdentifier": "XI004321B",
      |      "CorrelationIdentifier": "PORTAL6de1b822562c43fb9220d236e487c920"
      |    },
      |    "Body": {
      |      "NotificationOfAcceptedExport": {
      |        "AttributesValue": {
      |          "DateAndTimeOfIssuance": "2024-06-26T09:14:54"
      |        },
      |        "ConsigneeTrader": {
      |          "Traderid": "AT00000612157",
      |          "TraderName": "Whale Oil Lamps Co.",
      |          "StreetName": "The Street",
      |          "StreetNumber": "token",
      |          "Postcode": "MC232",
      |          "City": "Happy Town",
      |          "EoriNumber": "7",
      |          "attributes": {
      |            "@language": "en"
      |          }
      |        },
      |        "ExciseMovementEad": [
      |          {
      |            "AdministrativeReferenceCode": "23XI00000000000056339",
      |            "SequenceNumber": "1"
      |          },
      |          {
      |            "AdministrativeReferenceCode": "23XI00000000000056340",
      |            "SequenceNumber": "1"
      |          }
      |        ],
      |        "ExportPlaceCustomsOffice": {
      |          "ReferenceNumber": "tokentok"
      |        },
      |        "ExportDeclarationAcceptanceRelease": {
      |          "ReferenceNumberOfSenderCustomsOffice": "tokentok",
      |          "IdentificationOfSenderCustomsOfficer": "token",
      |          "DateOfAcceptance": "2013-05-22+01:00",
      |          "DateOfRelease": "2002-11-05Z",
      |          "DocumentReferenceNumber": "token",
      |          "ExportDeclarationAcceptanceOrGoodsReleasedForExport": "1"
      |        }
      |      }
      |    }
      |  },
      |  "outcome": {
      |    "status": "SUCCESS"
      |  }
      |}""".stripMargin)

  override def auditFailure(failureReason: String): JsValue = Json.parse(s"""
       |{
       |  "messageCode": "IE829",
       |  "content": {
       |    "Header": {
       |      "MessageSender": "NDEA.XI",
       |      "MessageRecipient": "NDEA.AT",
       |      "DateOfPreparation": "2023-06-26",
       |      "TimeOfPreparation": "09:15:33",
       |      "MessageIdentifier": "XI004321B",
       |      "CorrelationIdentifier": "PORTAL6de1b822562c43fb9220d236e487c920"
       |    },
       |    "Body": {
       |      "NotificationOfAcceptedExport": {
       |        "AttributesValue": {
       |          "DateAndTimeOfIssuance": "2024-06-26T09:14:54"
       |        },
       |        "ConsigneeTrader": {
       |          "Traderid": "AT00000612157",
       |          "TraderName": "Whale Oil Lamps Co.",
       |          "StreetName": "The Street",
       |          "StreetNumber": "token",
       |          "Postcode": "MC232",
       |          "City": "Happy Town",
       |          "EoriNumber": "7",
       |          "attributes": {
       |            "@language": "en"
       |          }
       |        },
       |        "ExciseMovementEad": [
       |          {
       |            "AdministrativeReferenceCode": "23XI00000000000056339",
       |            "SequenceNumber": "1"
       |          },
       |          {
       |            "AdministrativeReferenceCode": "23XI00000000000056340",
       |            "SequenceNumber": "1"
       |          }
       |        ],
       |        "ExportPlaceCustomsOffice": {
       |          "ReferenceNumber": "tokentok"
       |        },
       |        "ExportDeclarationAcceptanceRelease": {
       |          "ReferenceNumberOfSenderCustomsOffice": "tokentok",
       |          "IdentificationOfSenderCustomsOfficer": "token",
       |          "DateOfAcceptance": "2013-05-22+01:00",
       |          "DateOfRelease": "2002-11-05Z",
       |          "DocumentReferenceNumber": "token",
       |          "ExportDeclarationAcceptanceOrGoodsReleasedForExport": "1"
       |        }
       |      }
       |    }
       |  },
       |  "outcome": {
       |    "status": "FAILURE",
       |    "failureReason": "Test Message"
       |  }
       |}""".stripMargin)
}
