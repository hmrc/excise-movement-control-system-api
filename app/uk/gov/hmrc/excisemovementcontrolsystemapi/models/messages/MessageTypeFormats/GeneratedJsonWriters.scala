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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats

import generated._
import play.api.libs.json.{Format, JsError, JsObject, JsString, JsSuccess, Json, OWrites, Reads, Writes}
import scalaxb.{DataRecord, DataTypeFactory}

import javax.xml.datatype.XMLGregorianCalendar
import scala.util.Try

/**
  * It is important to make sure that the implicit writers are in child-first order in this file.
  * xImplicit writes in the same file must always be before the writes that use them.
  */
trait GeneratedJsonWriters {

  implicit val mapWrites: OWrites[Map[String, DataRecord[Any]]] = OWrites { map =>
    JsObject(map.toSeq.map { case (s, r) =>
      s -> JsString(r.value.toString)
    })
  }

  implicit val xmlDateTime: Format[XMLGregorianCalendar]            = Format(xmlDateTimeReads, xmlDateTimeWrites)
  implicit lazy val xmlDateTimeWrites: Writes[XMLGregorianCalendar] = Writes { dateTime =>
    JsString(dateTime.toXMLFormat)
  }
  implicit lazy val xmlDateTimeReads: Reads[XMLGregorianCalendar]   = Reads {
    case JsString(dateTime) =>
      Try(JsSuccess(DataTypeFactory.initialValue().newXMLGregorianCalendar(dateTime))).getOrElse(JsError())
    case _                  => JsError()
  }

  implicit val SubmissionType: Writes[SubmissionType] = Writes[SubmissionType] { submissionType =>
    JsString(submissionType.toString)
  }
  implicit val Flag: Writes[Flag]                     = Writes[Flag] { flag =>
    JsString(flag.toString)
  }

  implicit val AttributesTypeType11: OWrites[AttributesTypeType11] = Json.writes[AttributesTypeType11]

  implicit val ConsigneeTraderTypeType2: OWrites[ConsigneeTraderTypeType2] = Json.writes[ConsigneeTraderTypeType2]
  implicit val ConsignorTraderTypeType: OWrites[ConsignorTraderTypeType]   = Json.writes[ConsignorTraderTypeType]

  implicit val PlaceOfDispatchTraderTypeType: OWrites[PlaceOfDispatchTraderTypeType] =
    Json.writes[PlaceOfDispatchTraderTypeType]

  implicit val DispatchImportOfficeType: OWrites[DispatchImportOfficeType] = Json.writes[DispatchImportOfficeType]

  implicit val ComplementConsigneeTraderType: OWrites[ComplementConsigneeTraderType] =
    Json.writes[ComplementConsigneeTraderType]

  implicit val DeliveryPlaceTraderTypeType4: OWrites[DeliveryPlaceTraderTypeType4] =
    Json.writes[DeliveryPlaceTraderTypeType4]

  implicit val DeliveryPlaceCustomsOfficeTypeType3: OWrites[DeliveryPlaceCustomsOfficeTypeType3] =
    Json.writes[DeliveryPlaceCustomsOfficeTypeType3]

  implicit val CompetentAuthorityDispatchOfficeTypeType: OWrites[CompetentAuthorityDispatchOfficeTypeType] =
    Json.writes[CompetentAuthorityDispatchOfficeTypeType]
  implicit val TransportArrangerTraderTypeType: OWrites[TransportArrangerTraderTypeType]                   =
    Json.writes[TransportArrangerTraderTypeType]
  implicit val FirstTransporterTraderTypeType: OWrites[FirstTransporterTraderTypeType]                     =
    Json.writes[FirstTransporterTraderTypeType]
  implicit val LSDReferenceOfDocumentTypeType: OWrites[LSDReferenceOfDocumentTypeType]                     =
    Json.writes[LSDReferenceOfDocumentTypeType]

  implicit val DestinationTypeCode: Writes[DestinationTypeCode] = Writes[DestinationTypeCode] { code =>
    JsString(code.toString)
  }

  implicit val LSDComplementaryInformationTypeType10: OWrites[LSDComplementaryInformationTypeType10] =
    Json.writes[LSDComplementaryInformationTypeType10]

  implicit val GuarantorTypeCode: Writes[GuarantorTypeCode] = Writes[GuarantorTypeCode] { code =>
    JsString(code.toString)
  }

  implicit val LSDFiscalMarkTypeType2: OWrites[LSDFiscalMarkTypeType2]                                         = Json.writes[LSDFiscalMarkTypeType2]
  implicit val LSDDesignationOfOriginTypeType: OWrites[LSDDesignationOfOriginTypeType]                         =
    Json.writes[LSDDesignationOfOriginTypeType]
  implicit val LSDCommercialDescriptionTypeType2: OWrites[LSDCommercialDescriptionTypeType2]                   =
    Json.writes[LSDCommercialDescriptionTypeType2]
  implicit val LSDBrandNameOfProductsTypeType2: OWrites[LSDBrandNameOfProductsTypeType2]                       =
    Json.writes[LSDBrandNameOfProductsTypeType2]
  implicit val LSDMaturationPeriodOrAgeOfProductsTypeType: OWrites[LSDMaturationPeriodOrAgeOfProductsTypeType] =
    Json.writes[LSDMaturationPeriodOrAgeOfProductsTypeType]
  implicit val LSDIndependentSmallProducersDeclarationTypeType
    : OWrites[LSDIndependentSmallProducersDeclarationTypeType]                                                 =
    Json.writes[LSDIndependentSmallProducersDeclarationTypeType]
  implicit val ImportSadTypeType: OWrites[ImportSadTypeType]                                                   = Json.writes[ImportSadTypeType]
  implicit val LSDSealInformationTypeType5: OWrites[LSDSealInformationTypeType5]                               =
    Json.writes[LSDSealInformationTypeType5]

  //IE815 Reordered

  implicit val ComplementConsigneeTraderTypeType: OWrites[ComplementConsigneeTraderTypeType] =
    Json.writes[ComplementConsigneeTraderTypeType]
  implicit val DispatchImportOfficeTypeType: OWrites[DispatchImportOfficeTypeType]           =
    Json.writes[DispatchImportOfficeTypeType]
  implicit val LSDDocumentDescriptionTypeType: OWrites[LSDDocumentDescriptionTypeType]       =
    Json.writes[LSDDocumentDescriptionTypeType]

  implicit val TransportArrangement: Writes[TransportArrangement] = Writes[TransportArrangement] { a =>
    JsString(a.toString)
  }

  implicit val GuarantorTraderTypeType2: OWrites[GuarantorTraderTypeType2]       = Json.writes[GuarantorTraderTypeType2]
  implicit val WineOperationTypeType: OWrites[WineOperationTypeType]             = Json.writes[WineOperationTypeType]
  implicit val LSDOtherInformationTypeType: OWrites[LSDOtherInformationTypeType] =
    Json.writes[LSDOtherInformationTypeType]

  implicit val CategoryOfWineProduct: Writes[CategoryOfWineProduct]              = Writes[CategoryOfWineProduct] { cat =>
    JsString(cat.toString)
  }
  implicit val OriginTypeCode: Writes[OriginTypeCode]                            = Writes[OriginTypeCode] { code =>
    JsString(code.toString)
  }
  implicit val WineProductTypeType: OWrites[WineProductTypeType]                 = Json.writes[WineProductTypeType]
  implicit val PackageTypeType2: OWrites[PackageTypeType2]                       = Json.writes[PackageTypeType2]
  implicit val TransportDetailsTypeType5: OWrites[TransportDetailsTypeType5]     = Json.writes[TransportDetailsTypeType5]
  implicit val EadEsadDraftType: OWrites[EadEsadDraftType]                       = Json.writes[EadEsadDraftType]
  implicit val BodyEadEsadTypeType: OWrites[BodyEadEsadTypeType]                 = Json.writes[BodyEadEsadTypeType]
  implicit val MovementGuaranteeTypeType2: OWrites[MovementGuaranteeTypeType2]   = Json.writes[MovementGuaranteeTypeType2]
  implicit val TransportModeTypeType: OWrites[TransportModeTypeType]             = Json.writes[TransportModeTypeType]
  implicit val HeaderEadEsadTypeType: OWrites[HeaderEadEsadTypeType]             = Json.writes[HeaderEadEsadTypeType]
  implicit val DocumentCertificateTypeType: OWrites[DocumentCertificateTypeType] =
    Json.writes[DocumentCertificateTypeType]
  implicit val SubmittedDraftOfEADESADType: OWrites[SubmittedDraftOfEADESADType] =
    Json.writes[SubmittedDraftOfEADESADType]

  implicit val BodyTypeType14: OWrites[BodyTypeType14] = Json.writes[BodyTypeType14]
  implicit val HeaderType: OWrites[HeaderType]         = Json.writes[HeaderType]

  implicit val IE815Type: OWrites[IE815Type] = Json.writes[IE815Type]

  //IE704

  implicit val FunctionalErrorCodesType: Writes[FunctionalErrorCodesType]    = Writes[FunctionalErrorCodesType] { code =>
    JsString(code.toString)
  }
  implicit val FunctionalErrorType: OWrites[FunctionalErrorType]             = Json.writes[FunctionalErrorType]
  implicit val AttributesTypeType9: OWrites[AttributesTypeType9]             = Json.writes[AttributesTypeType9]
  implicit val GenericRefusalMessageType: OWrites[GenericRefusalMessageType] = Json.writes[GenericRefusalMessageType]
  implicit val BodyTypeType10: OWrites[BodyTypeType10]                       = Json.writes[BodyTypeType10]
  implicit val IE704Type: OWrites[IE704Type]                                 = Json.writes[IE704Type]

  //IE801

  implicit val LSDReferenceOfDocumentType: OWrites[LSDReferenceOfDocumentType]                                   = Json.writes[LSDReferenceOfDocumentType]
  implicit val LSDDocumentDescriptionType: OWrites[LSDDocumentDescriptionType]                                   = Json.writes[LSDDocumentDescriptionType]
  implicit val ImportSadType: OWrites[ImportSadType]                                                             = Json.writes[ImportSadType]
  implicit val LSDSealInformationTypeType4: OWrites[LSDSealInformationTypeType4]                                 =
    Json.writes[LSDSealInformationTypeType4]
  implicit val WineOperationType: OWrites[WineOperationType]                                                     = Json.writes[WineOperationType]
  implicit val LSDOtherInformationType: OWrites[LSDOtherInformationType]                                         = Json.writes[LSDOtherInformationType]
  implicit val GuarantorTraderTypeType: OWrites[GuarantorTraderTypeType]                                         = Json.writes[GuarantorTraderTypeType]
  implicit val WineProductType: OWrites[WineProductType]                                                         = Json.writes[WineProductType]
  implicit val PackageTypeType: OWrites[PackageTypeType]                                                         = Json.writes[PackageTypeType]
  implicit val LSDMaturationPeriodOrAgeOfProductsType: OWrites[LSDMaturationPeriodOrAgeOfProductsType]           =
    Json.writes[LSDMaturationPeriodOrAgeOfProductsType]
  implicit val LSDIndependentSmallProducersDeclarationType: OWrites[LSDIndependentSmallProducersDeclarationType] =
    Json.writes[LSDIndependentSmallProducersDeclarationType]
  implicit val LSDBrandNameOfProductsTypeType: OWrites[LSDBrandNameOfProductsTypeType]                           =
    Json.writes[LSDBrandNameOfProductsTypeType]
  implicit val LSDCommercialDescriptionTypeType: OWrites[LSDCommercialDescriptionTypeType]                       =
    Json.writes[LSDCommercialDescriptionTypeType]
  implicit val LSDDesignationOfOriginType: OWrites[LSDDesignationOfOriginType]                                   = Json.writes[LSDDesignationOfOriginType]
  implicit val LSDFiscalMarkTypeType: OWrites[LSDFiscalMarkTypeType]                                             = Json.writes[LSDFiscalMarkTypeType]
  implicit val LSDComplementaryInformationTypeType8: OWrites[LSDComplementaryInformationTypeType8]               =
    Json.writes[LSDComplementaryInformationTypeType8]
  implicit val TransportDetailsTypeType4: OWrites[TransportDetailsTypeType4]                                     = Json.writes[TransportDetailsTypeType4]
  implicit val BodyEadEsadType: OWrites[BodyEadEsadType]                                                         = Json.writes[BodyEadEsadType]
  implicit val MovementGuaranteeTypeType: OWrites[MovementGuaranteeTypeType]                                     = Json.writes[MovementGuaranteeTypeType]
  implicit val TransportModeType: OWrites[TransportModeType]                                                     = Json.writes[TransportModeType]
  implicit val HeaderEadEsadType: OWrites[HeaderEadEsadType]                                                     = Json.writes[HeaderEadEsadType]
  implicit val EadEsadType: OWrites[EadEsadType]                                                                 = Json.writes[EadEsadType]
  implicit val DocumentCertificateType: OWrites[DocumentCertificateType]                                         = Json.writes[DocumentCertificateType]
  implicit val FirstTransporterTraderType: OWrites[FirstTransporterTraderType]                                   = Json.writes[FirstTransporterTraderType]
  implicit val TransportArrangerTraderType: OWrites[TransportArrangerTraderType]                                 =
    Json.writes[TransportArrangerTraderType]
  implicit val CompetentAuthorityDispatchOfficeType: OWrites[CompetentAuthorityDispatchOfficeType]               =
    Json.writes[CompetentAuthorityDispatchOfficeType]
  implicit val DeliveryPlaceCustomsOfficeTypeType2: OWrites[DeliveryPlaceCustomsOfficeTypeType2]                 =
    Json.writes[DeliveryPlaceCustomsOfficeTypeType2]
  implicit val DeliveryPlaceTraderTypeType3: OWrites[DeliveryPlaceTraderTypeType3]                               =
    Json.writes[DeliveryPlaceTraderTypeType3]
  implicit val PlaceOfDispatchTraderType: OWrites[PlaceOfDispatchTraderType]                                     = Json.writes[PlaceOfDispatchTraderType]
  implicit val ConsignorTraderType: OWrites[ConsignorTraderType]                                                 = Json.writes[ConsignorTraderType]
  implicit val ExciseMovementTypeType5: OWrites[ExciseMovementTypeType5]                                         = Json.writes[ExciseMovementTypeType5]
  implicit val ConsigneeTraderTypeType: OWrites[ConsigneeTraderTypeType]                                         = Json.writes[ConsigneeTraderTypeType]
  implicit val EADESADContainerType: OWrites[EADESADContainerType]                                               = Json.writes[EADESADContainerType]
  implicit val BodyTypeType11: OWrites[BodyTypeType11]                                                           = Json.writes[BodyTypeType11]
  implicit val IE801Type: OWrites[IE801Type]                                                                     = Json.writes[IE801Type]

  //IE802

  implicit val ReminderMessageType: Writes[ReminderMessageType]                                    = Writes[ReminderMessageType] { t =>
    JsString(t.toString)
  }
  implicit val LSDReminderInformationType: OWrites[LSDReminderInformationType]                     = Json.writes[LSDReminderInformationType]
  implicit val ExciseMovementTypeType3: OWrites[ExciseMovementTypeType3]                           = Json.writes[ExciseMovementTypeType3]
  implicit val AttributesTypeType7: OWrites[AttributesTypeType7]                                   = Json.writes[AttributesTypeType7]
  implicit val ReminderMessageForExciseMovementType: OWrites[ReminderMessageForExciseMovementType] =
    Json.writes[ReminderMessageForExciseMovementType]
  implicit val BodyTypeType8: OWrites[BodyTypeType8]                                               = Json.writes[BodyTypeType8]
  implicit val IE802Type: OWrites[IE802Type]                                                       = Json.writes[IE802Type]

  //IE803

  implicit val NotificationType: Writes[NotificationType]                                    = Writes[NotificationType] { t =>
    JsString(t.toString)
  }
  implicit val DownstreamArcType: OWrites[DownstreamArcType]                                 = Json.writes[DownstreamArcType]
  implicit val ExciseNotificationType: OWrites[ExciseNotificationType]                       = Json.writes[ExciseNotificationType]
  implicit val NotificationOfDivertedEADESADType: OWrites[NotificationOfDivertedEADESADType] =
    Json.writes[NotificationOfDivertedEADESADType]
  implicit val BodyTypeType12: OWrites[BodyTypeType12]                                       = Json.writes[BodyTypeType12]
  implicit val IE803Type: OWrites[IE803Type]                                                 = Json.writes[IE803Type]

  //IE807

  implicit val LSDComplementaryInformationTypeType6: OWrites[LSDComplementaryInformationTypeType6] =
    Json.writes[LSDComplementaryInformationTypeType6]
  implicit val ReferenceEventReportType: OWrites[ReferenceEventReportType]                         = Json.writes[ReferenceEventReportType]
  implicit val ReferenceControlReportType: OWrites[ReferenceControlReportType]                     = Json.writes[ReferenceControlReportType]
  implicit val AttributesTypeType6: OWrites[AttributesTypeType6]                                   = Json.writes[AttributesTypeType6]
  implicit val InterruptionOfMovementType: OWrites[InterruptionOfMovementType]                     = Json.writes[InterruptionOfMovementType]
  implicit val BodyTypeType7: OWrites[BodyTypeType7]                                               = Json.writes[BodyTypeType7]
  implicit val IE807Type: OWrites[IE807Type]                                                       = Json.writes[IE807Type]

  //IE810

  implicit val LSDComplementaryInformationTypeType9: OWrites[LSDComplementaryInformationTypeType9] =
    Json.writes[LSDComplementaryInformationTypeType9]
  implicit val CancellationType: OWrites[CancellationType]                                         = Json.writes[CancellationType]
  implicit val ExciseMovementEadType: OWrites[ExciseMovementEadType]                               = Json.writes[ExciseMovementEadType]
  implicit val AttributesTypeType10: OWrites[AttributesTypeType10]                                 = Json.writes[AttributesTypeType10]
  implicit val CancellationOfEADType: OWrites[CancellationOfEADType]                               = Json.writes[CancellationOfEADType]
  implicit val BodyTypeType13: OWrites[BodyTypeType13]                                             = Json.writes[BodyTypeType13]
  implicit val IE810Type: OWrites[IE810Type]                                                       = Json.writes[IE810Type]

  //IE813

  implicit val GuarantorTraderType: OWrites[GuarantorTraderType]                                   = Json.writes[GuarantorTraderType]
  implicit val MovementGuaranteeType: OWrites[MovementGuaranteeType]                               = Json.writes[MovementGuaranteeType]
  implicit val DeliveryPlaceCustomsOfficeTypeType: OWrites[DeliveryPlaceCustomsOfficeTypeType]     =
    Json.writes[DeliveryPlaceCustomsOfficeTypeType]
  implicit val DeliveryPlaceTraderTypeType2: OWrites[DeliveryPlaceTraderTypeType2]                 =
    Json.writes[DeliveryPlaceTraderTypeType2]
  implicit val NewConsigneeTraderTypeType: OWrites[NewConsigneeTraderTypeType]                     = Json.writes[NewConsigneeTraderTypeType]
  implicit val LSDSealInformationTypeType2: OWrites[LSDSealInformationTypeType2]                   =
    Json.writes[LSDSealInformationTypeType2]
  implicit val ChangedDestinationTypeCode: Writes[ChangedDestinationTypeCode]                      = Writes[ChangedDestinationTypeCode] {
    code => JsString(code.toString)
  }
  implicit val LSDComplementaryInformationTypeType5: OWrites[LSDComplementaryInformationTypeType5] =
    Json.writes[LSDComplementaryInformationTypeType5]
  implicit val TransportDetailsTypeType2: OWrites[TransportDetailsTypeType2]                       = Json.writes[TransportDetailsTypeType2]
  implicit val NewTransporterTraderTypeType2: OWrites[NewTransporterTraderTypeType2]               =
    Json.writes[NewTransporterTraderTypeType2]
  implicit val DestinationChangedTypeType: OWrites[DestinationChangedTypeType]                     = Json.writes[DestinationChangedTypeType]
  implicit val UpdateEadEsadType: OWrites[UpdateEadEsadType]                                       = Json.writes[UpdateEadEsadType]
  implicit val NewTransportArrangerTraderTypeType2: OWrites[NewTransportArrangerTraderTypeType2]   =
    Json.writes[NewTransportArrangerTraderTypeType2]
  implicit val AttributesTypeType5: OWrites[AttributesTypeType5]                                   = Json.writes[AttributesTypeType5]
  implicit val ChangeOfDestinationType: OWrites[ChangeOfDestinationType]                           = Json.writes[ChangeOfDestinationType]
  implicit val BodyTypeType6: OWrites[BodyTypeType6]                                               = Json.writes[BodyTypeType6]
  implicit val IE813Type: OWrites[IE813Type]                                                       = Json.writes[IE813Type]

  //IE818

  implicit val LSDComplementaryInformationTypeType4: OWrites[LSDComplementaryInformationTypeType4]               =
    Json.writes[LSDComplementaryInformationTypeType4]
  implicit val UnsatisfactoryReasonType: OWrites[UnsatisfactoryReasonType]                                       = Json.writes[UnsatisfactoryReasonType]
  implicit val IndicatorOfShortageOrExcess: Writes[IndicatorOfShortageOrExcess]                                  = Writes[IndicatorOfShortageOrExcess] {
    i => JsString(i.toString)
  }
  implicit val GlobalConclusionOfReceipt: Writes[GlobalConclusionOfReceipt]                                      = Writes[GlobalConclusionOfReceipt] { r =>
    JsString(r.toString)
  }
  implicit val BodyReportOfReceiptExportType: OWrites[BodyReportOfReceiptExportType]                             =
    Json.writes[BodyReportOfReceiptExportType]
  implicit val ReportOfReceiptExportType: OWrites[ReportOfReceiptExportType]                                     = Json.writes[ReportOfReceiptExportType]
  implicit val DestinationOfficeType: OWrites[DestinationOfficeType]                                             = Json.writes[DestinationOfficeType]
  implicit val DeliveryPlaceTraderTypeType: OWrites[DeliveryPlaceTraderTypeType]                                 =
    Json.writes[DeliveryPlaceTraderTypeType]
  implicit val ExciseMovementTypeType2: OWrites[ExciseMovementTypeType2]                                         = Json.writes[ExciseMovementTypeType2]
  implicit val ConsigneeTraderType: OWrites[ConsigneeTraderType]                                                 = Json.writes[ConsigneeTraderType]
  implicit val AttributesTypeType4: OWrites[AttributesTypeType4]                                                 = Json.writes[AttributesTypeType4]
  implicit val AcceptedOrRejectedReportOfReceiptExportType: OWrites[AcceptedOrRejectedReportOfReceiptExportType] =
    Json.writes[AcceptedOrRejectedReportOfReceiptExportType]
  implicit val BodyTypeType5: OWrites[BodyTypeType5]                                                             = Json.writes[BodyTypeType5]
  implicit val IE818Type: OWrites[IE818Type]                                                                     = Json.writes[IE818Type]

  //IE819

  implicit val LSDComplementaryInformationTypeType11: OWrites[LSDComplementaryInformationTypeType11] =
    Json.writes[LSDComplementaryInformationTypeType11]
  implicit val AlertOrRejectionOfEadEsadReasonType: OWrites[AlertOrRejectionOfEadEsadReasonType]     =
    Json.writes[AlertOrRejectionOfEadEsadReasonType]
  implicit val AlertOrRejectionType: OWrites[AlertOrRejectionType]                                   = Json.writes[AlertOrRejectionType]
  implicit val DestinationOfficeTypeType: OWrites[DestinationOfficeTypeType]                         = Json.writes[DestinationOfficeTypeType]
  implicit val ExciseMovementTypeType6: OWrites[ExciseMovementTypeType6]                             = Json.writes[ExciseMovementTypeType6]
  implicit val ConsigneeTraderTypeType3: OWrites[ConsigneeTraderTypeType3]                           = Json.writes[ConsigneeTraderTypeType3]
  implicit val AttributesTypeType12: OWrites[AttributesTypeType12]                                   = Json.writes[AttributesTypeType12]
  implicit val AlertOrRejectionOfEADESADType: OWrites[AlertOrRejectionOfEADESADType]                 =
    Json.writes[AlertOrRejectionOfEADESADType]
  implicit val BodyTypeType15: OWrites[BodyTypeType15]                                               = Json.writes[BodyTypeType15]
  implicit val IE819Type: OWrites[IE819Type]                                                         = Json.writes[IE819Type]

  //IE829

  implicit val ExportDeclarationAcceptanceReleaseType: OWrites[ExportDeclarationAcceptanceReleaseType] =
    Json.writes[ExportDeclarationAcceptanceReleaseType]
  implicit val ExportPlaceCustomsOfficeType: OWrites[ExportPlaceCustomsOfficeType]                     =
    Json.writes[ExportPlaceCustomsOfficeType]
  implicit val ExciseMovementEadTypeType: OWrites[ExciseMovementEadTypeType]                           = Json.writes[ExciseMovementEadTypeType]
  implicit val ConsigneeTraderTypeType4: OWrites[ConsigneeTraderTypeType4]                             = Json.writes[ConsigneeTraderTypeType4]
  implicit val AttributesTypeType13: OWrites[AttributesTypeType13]                                     = Json.writes[AttributesTypeType13]
  implicit val NotificationOfAcceptedExportType: OWrites[NotificationOfAcceptedExportType]             =
    Json.writes[NotificationOfAcceptedExportType]
  implicit val BodyTypeType16: OWrites[BodyTypeType16]                                                 = Json.writes[BodyTypeType16]
  implicit val IE829Type: OWrites[IE829Type]                                                           = Json.writes[IE829Type]

  //IE837

  implicit val MessageRoleCode: Writes[MessageRoleCode]                                            = Writes[MessageRoleCode] { code =>
    JsString(code.toString)
  }
  implicit val LSDComplementaryInformationTypeType2: OWrites[LSDComplementaryInformationTypeType2] =
    Json.writes[LSDComplementaryInformationTypeType2]
  implicit val SubmitterType: Writes[SubmitterType]                                                = Writes[SubmitterType] { t =>
    JsString(t.toString)
  }
  implicit val ExciseMovementTypeType: OWrites[ExciseMovementTypeType]                             = Json.writes[ExciseMovementTypeType]
  implicit val AttributesTypeType3: OWrites[AttributesTypeType3]                                   = Json.writes[AttributesTypeType3]
  implicit val ExplanationOnDelayForDeliveryType: OWrites[ExplanationOnDelayForDeliveryType]       =
    Json.writes[ExplanationOnDelayForDeliveryType]
  implicit val BodyTypeType3: OWrites[BodyTypeType3]                                               = Json.writes[BodyTypeType3]
  implicit val IE837Type: OWrites[IE837Type]                                                       = Json.writes[IE837Type]

  //IE839

  implicit val CustomsRejectionReasonCode: Writes[CustomsRejectionReasonCode] = Writes[CustomsRejectionReasonCode] {
    code => JsString(code.toString)
  }
  implicit val DiagnosisCode: Writes[DiagnosisCode]                           = Writes[DiagnosisCode] { code =>
    JsString(code.toString)
  }

  implicit val NEadSubType: OWrites[NEadSubType]                                                                   = Json.writes[NEadSubType]
  implicit val CEadValType: OWrites[CEadValType]                                                                   = Json.writes[CEadValType]
  implicit val RejectionType: OWrites[RejectionType]                                                               = Json.writes[RejectionType]
  implicit val ExportPlaceCustomsOfficeTypeType: OWrites[ExportPlaceCustomsOfficeTypeType]                         =
    Json.writes[ExportPlaceCustomsOfficeTypeType]
  implicit val ConsigneeTraderTypeType5: OWrites[ConsigneeTraderTypeType5]                                         = Json.writes[ConsigneeTraderTypeType5]
  implicit val AttributesTypeType14: OWrites[AttributesTypeType14]                                                 = Json.writes[AttributesTypeType14]
  implicit val CombinedNomenclatureCodeCrosscheckResultType: OWrites[CombinedNomenclatureCodeCrosscheckResultType] =
    Json.writes[CombinedNomenclatureCodeCrosscheckResultType]
  implicit val NetMassCrosscheckResultType: OWrites[NetMassCrosscheckResultType]                                   =
    Json.writes[NetMassCrosscheckResultType]
  implicit val UbrCrosscheckResultType: OWrites[UbrCrosscheckResultType]                                           = Json.writes[UbrCrosscheckResultType]
  implicit val NegativeCrosscheckValidationResultsType: OWrites[NegativeCrosscheckValidationResultsType]           =
    Json.writes[NegativeCrosscheckValidationResultsType]
  implicit val NNonDesType: OWrites[NNonDesType]                                                                   = Json.writes[NNonDesType]
  implicit val ExportDeclarationInformationType: OWrites[ExportDeclarationInformationType]                         =
    Json.writes[ExportDeclarationInformationType]
  implicit val RefusalByCustomsType: OWrites[RefusalByCustomsType]                                                 = Json.writes[RefusalByCustomsType]
  implicit val BodyTypeType17: OWrites[BodyTypeType17]                                                             = Json.writes[BodyTypeType17]
  implicit val IE839Type: OWrites[IE839Type]                                                                       = Json.writes[IE839Type]

  //IE840

  implicit val LSDAssociatedInformationType: OWrites[LSDAssociatedInformationType]               =
    Json.writes[LSDAssociatedInformationType]
  implicit val LSDSealInformationType: OWrites[LSDSealInformationType]                           = Json.writes[LSDSealInformationType]
  implicit val LSDComplementaryInformationTypeType: OWrites[LSDComplementaryInformationTypeType] =
    Json.writes[LSDComplementaryInformationTypeType]
  implicit val LSDEvidenceTypeComplementType: OWrites[LSDEvidenceTypeComplementType]             =
    Json.writes[LSDEvidenceTypeComplementType]
  implicit val LSDReferenceOfEvidenceType: OWrites[LSDReferenceOfEvidenceType]                   = Json.writes[LSDReferenceOfEvidenceType]
  implicit val LSDIssuingAuthorityType: OWrites[LSDIssuingAuthorityType]                         = Json.writes[LSDIssuingAuthorityType]
  implicit val LSDCommentsType: OWrites[LSDCommentsType]                                         = Json.writes[LSDCommentsType]
  implicit val LSDSubmittingPersonComplementType: OWrites[LSDSubmittingPersonComplementType]     =
    Json.writes[LSDSubmittingPersonComplementType]
  implicit val LSDPlaceOfEventType: OWrites[LSDPlaceOfEventType]                                 = Json.writes[LSDPlaceOfEventType]
  implicit val LSDAcoComplementaryInformationType: OWrites[LSDAcoComplementaryInformationType]   =
    Json.writes[LSDAcoComplementaryInformationType]
  implicit val TraderPersonType: Writes[TraderPersonType]                                        = Writes[TraderPersonType] { t =>
    JsString(t.toString)
  }
  implicit val MeansOfTransportType: OWrites[MeansOfTransportType]                               = Json.writes[MeansOfTransportType]
  implicit val GoodsItemType: OWrites[GoodsItemType]                                             = Json.writes[GoodsItemType]
  implicit val PersonInvolvedInMovementTraderType: OWrites[PersonInvolvedInMovementTraderType]   =
    Json.writes[PersonInvolvedInMovementTraderType]
  implicit val LSDShortDescriptionOfOtherAccompanyingDocumentType
    : OWrites[LSDShortDescriptionOfOtherAccompanyingDocumentType]                                =
    Json.writes[LSDShortDescriptionOfOtherAccompanyingDocumentType]
  implicit val OtherAccompanyingDocumentType: Writes[OtherAccompanyingDocumentType]              =
    Writes[OtherAccompanyingDocumentType] { t =>
      JsString(t.toString)
    }
  implicit val ReportMessageType: Writes[ReportMessageType]                                      = Writes[ReportMessageType] { t =>
    JsString(t.toString)
  }
  implicit val BodyEventReportType: OWrites[BodyEventReportType]                                 = Json.writes[BodyEventReportType]
  implicit val TransportDetailsType: OWrites[TransportDetailsType]                               = Json.writes[TransportDetailsType]
  implicit val NewTransporterTraderType: OWrites[NewTransporterTraderType]                       = Json.writes[NewTransporterTraderType]
  implicit val NewTransportArrangerTraderType: OWrites[NewTransportArrangerTraderType]           =
    Json.writes[NewTransportArrangerTraderType]
  implicit val EvidenceOfEventType: OWrites[EvidenceOfEventType]                                 = Json.writes[EvidenceOfEventType]
  implicit val EventReportType: OWrites[EventReportType]                                         = Json.writes[EventReportType]
  implicit val OtherAccompanyingDocumentTypeType: OWrites[OtherAccompanyingDocumentTypeType]     =
    Json.writes[OtherAccompanyingDocumentTypeType]
  implicit val ExciseMovementType: OWrites[ExciseMovementType]                                   = Json.writes[ExciseMovementType]
  implicit val HeaderEventReportType: OWrites[HeaderEventReportType]                             = Json.writes[HeaderEventReportType]
  implicit val AttributesTypeType2: OWrites[AttributesTypeType2]                                 = Json.writes[AttributesTypeType2]
  implicit val EventReportEnvelopeType: OWrites[EventReportEnvelopeType]                         = Json.writes[EventReportEnvelopeType]
  implicit val BodyTypeType2: OWrites[BodyTypeType2]                                             = Json.writes[BodyTypeType2]
  implicit val IE840Type: OWrites[IE840Type]                                                     = Json.writes[IE840Type]

  //IE871

  implicit val LSDExplanationType: OWrites[LSDExplanationType]                                 = Json.writes[LSDExplanationType]
  implicit val LSDGlobalExplanationType: OWrites[LSDGlobalExplanationType]                     = Json.writes[LSDGlobalExplanationType]
  implicit val BodyAnalysisType: OWrites[BodyAnalysisType]                                     = Json.writes[BodyAnalysisType]
  implicit val AnalysisType: OWrites[AnalysisType]                                             = Json.writes[AnalysisType]
  implicit val ConsignorTraderTypeType2: OWrites[ConsignorTraderTypeType2]                     = Json.writes[ConsignorTraderTypeType2]
  implicit val ExciseMovementTypeType7: OWrites[ExciseMovementTypeType7]                       = Json.writes[ExciseMovementTypeType7]
  implicit val ConsigneeTraderTypeType6: OWrites[ConsigneeTraderTypeType6]                     = Json.writes[ConsigneeTraderTypeType6]
  implicit val AttributesTypeType15: OWrites[AttributesTypeType15]                             = Json.writes[AttributesTypeType15]
  implicit val ExplanationOnReasonForShortageType: OWrites[ExplanationOnReasonForShortageType] =
    Json.writes[ExplanationOnReasonForShortageType]
  implicit val BodyTypeType18: OWrites[BodyTypeType18]                                         = Json.writes[BodyTypeType18]
  implicit val IE871Type: OWrites[IE871Type]                                                   = Json.writes[IE871Type]

  //IE881

  implicit val LSDReferenceOfSupportingDocumentTypeType: OWrites[LSDReferenceOfSupportingDocumentTypeType] =
    Json.writes[LSDReferenceOfSupportingDocumentTypeType]
  implicit val LSDSupportingDocumentDescriptionTypeType: OWrites[LSDSupportingDocumentDescriptionTypeType] =
    Json.writes[LSDSupportingDocumentDescriptionTypeType]
  implicit val LSDManualClosureRejectionComplementType: OWrites[LSDManualClosureRejectionComplementType]   =
    Json.writes[LSDManualClosureRejectionComplementType]
  implicit val LSDManualClosureRequestReasonCodeComplementTypeType
    : OWrites[LSDManualClosureRequestReasonCodeComplementTypeType]                                         =
    Json.writes[LSDManualClosureRequestReasonCodeComplementTypeType]
  implicit val LSDComplementaryInformationTypeType12: OWrites[LSDComplementaryInformationTypeType12]       =
    Json.writes[LSDComplementaryInformationTypeType12]
  implicit val BodyManualClosureTypeType: OWrites[BodyManualClosureTypeType]                               = Json.writes[BodyManualClosureTypeType]
  implicit val SupportingDocumentsTypeType: OWrites[SupportingDocumentsTypeType]                           =
    Json.writes[SupportingDocumentsTypeType]
  implicit val AttributesTypeType16: OWrites[AttributesTypeType16]                                         = Json.writes[AttributesTypeType16]
  implicit val ManualClosureResponseType: OWrites[ManualClosureResponseType]                               = Json.writes[ManualClosureResponseType]
  implicit val BodyTypeType19: OWrites[BodyTypeType19]                                                     = Json.writes[BodyTypeType19]
  implicit val IE881Type: OWrites[IE881Type]                                                               = Json.writes[IE881Type]

  //IE905

  implicit val StatusType: Writes[StatusType]                     = Writes[StatusType] { t =>
    JsString(t.toString)
  }
  implicit val RequestedMessageType: Writes[RequestedMessageType] = Writes[RequestedMessageType] { t =>
    JsString(t.toString)
  }
  implicit val AttributesType: OWrites[AttributesType]            = Json.writes[AttributesType]
  implicit val StatusResponseType: OWrites[StatusResponseType]    = Json.writes[StatusResponseType]
  implicit val BodyType: OWrites[BodyType]                        = Json.writes[BodyType]
  implicit val IE905Type: OWrites[IE905Type]                      = Json.writes[IE905Type]

}
