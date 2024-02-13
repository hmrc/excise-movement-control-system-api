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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.MessageTypeFormats

import generated._
import play.api.libs.json.{Format, JsError, JsObject, JsString, JsSuccess, Json, OWrites, Reads, Writes}
import scalaxb.{DataRecord, DataTypeFactory}

import javax.xml.datatype.XMLGregorianCalendar
import scala.util.Try

trait GeneratedJsonWriters {

  implicit val mapWrites: OWrites[Map[String, DataRecord[Any]]] = OWrites {
    map => {
      JsObject(map.toSeq.map { case (s, r) =>
        s -> JsString(r.value.toString)
      })
    }
  }

  implicit val xmlDateTime = Format(xmlDateTimeReads, xmlDateTimeWrites)
  implicit lazy val xmlDateTimeWrites: Writes[XMLGregorianCalendar] = Writes {
    dateTime =>
      JsString(dateTime.toXMLFormat)
  }
  implicit lazy val xmlDateTimeReads: Reads[XMLGregorianCalendar] = Reads {
    case JsString(dateTime) => Try(JsSuccess(DataTypeFactory.initialValue().newXMLGregorianCalendar(dateTime))).getOrElse(JsError())
    case _                  => JsError()
  }

  implicit val SubmissionType = Writes[SubmissionType] {
    submissionType => JsString(submissionType.toString)
  }
  implicit val Flag = Writes[Flag] {
    flag => JsString(flag.toString)
  }

  implicit val AttributesTypeType11 = Json.writes[AttributesTypeType11]



  implicit val ConsigneeTraderTypeType2 = Json.writes[ConsigneeTraderTypeType2]
  implicit val ConsignorTraderTypeType = Json.writes[ConsignorTraderTypeType]

  implicit val PlaceOfDispatchTraderTypeType = Json.writes[PlaceOfDispatchTraderTypeType]

  implicit val DispatchImportOfficeType = Json.writes[DispatchImportOfficeType]

  implicit val ComplementConsigneeTraderType = Json.writes[ComplementConsigneeTraderType]

  implicit val DeliveryPlaceTraderTypeType4 = Json.writes[DeliveryPlaceTraderTypeType4]

  implicit val DeliveryPlaceCustomsOfficeTypeType3 = Json.writes[DeliveryPlaceCustomsOfficeTypeType3]

  implicit val CompetentAuthorityDispatchOfficeTypeType = Json.writes[CompetentAuthorityDispatchOfficeTypeType]
  implicit val TransportArrangerTraderTypeType = Json.writes[TransportArrangerTraderTypeType]
  implicit val FirstTransporterTraderTypeType = Json.writes[FirstTransporterTraderTypeType]
  implicit val LSDReferenceOfDocumentTypeType = Json.writes[LSDReferenceOfDocumentTypeType]

  implicit val DestinationTypeCode = Writes[DestinationTypeCode] {
    code => JsString(code.toString)
  }

  implicit val LSDComplementaryInformationTypeType10 = Json.writes[LSDComplementaryInformationTypeType10]

  implicit val GuarantorTypeCode = Writes[GuarantorTypeCode] {
    code => JsString(code.toString)
  }

  implicit val LSDFiscalMarkTypeType2 = Json.writes[LSDFiscalMarkTypeType2]
  implicit val LSDDesignationOfOriginTypeType = Json.writes[LSDDesignationOfOriginTypeType]
  implicit val LSDCommercialDescriptionTypeType2 = Json.writes[LSDCommercialDescriptionTypeType2]
  implicit val LSDBrandNameOfProductsTypeType2 = Json.writes[LSDBrandNameOfProductsTypeType2]
  implicit val LSDMaturationPeriodOrAgeOfProductsTypeType = Json.writes[LSDMaturationPeriodOrAgeOfProductsTypeType]
  implicit val ImportSadTypeType = Json.writes[ImportSadTypeType]
  implicit val LSDSealInformationTypeType5 = Json.writes[LSDSealInformationTypeType5]

  //IE815 Reordered

  implicit val ComplementConsigneeTraderTypeType = Json.writes[ComplementConsigneeTraderTypeType]
  implicit val DispatchImportOfficeTypeType = Json.writes[DispatchImportOfficeTypeType]
  implicit val LSDDocumentDescriptionTypeType = Json.writes[LSDDocumentDescriptionTypeType]

  implicit val TransportArrangement = Writes[TransportArrangement] {
    a => JsString(a.toString)
  }

  implicit val GuarantorTraderTypeType2 = Json.writes[GuarantorTraderTypeType2]
  implicit val WineOperationTypeType = Json.writes[WineOperationTypeType]
  implicit val LSDOtherInformationTypeType = Json.writes[LSDOtherInformationTypeType]

  implicit val CategoryOfWineProduct = Writes[CategoryOfWineProduct] {
    cat => JsString(cat.toString)
  }
  implicit val OriginTypeCode = Writes[OriginTypeCode] {
    code => JsString(code.toString)
  }
  implicit val WineProductTypeType = Json.writes[WineProductTypeType]
  implicit val PackageTypeType2 = Json.writes[PackageTypeType2]
  implicit val TransportDetailsTypeType5 = Json.writes[TransportDetailsTypeType5]
  implicit val EadEsadDraftType = Json.writes[EadEsadDraftType]
  implicit val BodyEadEsadTypeType = Json.writes[BodyEadEsadTypeType]
  implicit val MovementGuaranteeTypeType2 = Json.writes[MovementGuaranteeTypeType2]
  implicit val TransportModeTypeType = Json.writes[TransportModeTypeType]
  implicit val HeaderEadEsadTypeType = Json.writes[HeaderEadEsadTypeType]
  implicit val DocumentCertificateTypeType = Json.writes[DocumentCertificateTypeType]
  implicit val SubmittedDraftOfEADESADType = Json.writes[SubmittedDraftOfEADESADType]

  implicit val BodyTypeType14 = Json.writes[BodyTypeType14]
  implicit val HeaderType = Json.writes[HeaderType]

  implicit val IE815Type = Json.writes[IE815Type]

  //IE704

  implicit val FunctionalErrorCodesType = Writes[FunctionalErrorCodesType] {
    code => JsString(code.toString)
  }
  implicit val FunctionalErrorType = Json.writes[FunctionalErrorType]
  implicit val AttributesTypeType9 = Json.writes[AttributesTypeType9]
  implicit val GenericRefusalMessageType = Json.writes[GenericRefusalMessageType]
  implicit val BodyTypeType10 = Json.writes[BodyTypeType10]
  implicit val IE704Type = Json.writes[IE704Type]

  //IE801

  implicit val LSDReferenceOfDocumentType = Json.writes[LSDReferenceOfDocumentType]
  implicit val LSDDocumentDescriptionType = Json.writes[LSDDocumentDescriptionType]
  implicit val ImportSadType = Json.writes[ImportSadType]
  implicit val LSDSealInformationTypeType4 = Json.writes[LSDSealInformationTypeType4]
  implicit val WineOperationType = Json.writes[WineOperationType]
  implicit val LSDOtherInformationType = Json.writes[LSDOtherInformationType]
  implicit val GuarantorTraderTypeType = Json.writes[GuarantorTraderTypeType]
  implicit val WineProductType = Json.writes[WineProductType]
  implicit val PackageTypeType = Json.writes[PackageTypeType]
  implicit val LSDMaturationPeriodOrAgeOfProductsType = Json.writes[LSDMaturationPeriodOrAgeOfProductsType]
  implicit val LSDBrandNameOfProductsTypeType = Json.writes[LSDBrandNameOfProductsTypeType]
  implicit val LSDCommercialDescriptionTypeType = Json.writes[LSDCommercialDescriptionTypeType]
  implicit val LSDDesignationOfOriginType = Json.writes[LSDDesignationOfOriginType]
  implicit val LSDFiscalMarkTypeType = Json.writes[LSDFiscalMarkTypeType]
  implicit val LSDComplementaryInformationTypeType8 = Json.writes[LSDComplementaryInformationTypeType8]
  implicit val TransportDetailsTypeType4 = Json.writes[TransportDetailsTypeType4]
  implicit val BodyEadEsadType = Json.writes[BodyEadEsadType]
  implicit val MovementGuaranteeTypeType = Json.writes[MovementGuaranteeTypeType]
  implicit val TransportModeType = Json.writes[TransportModeType]
  implicit val HeaderEadEsadType = Json.writes[HeaderEadEsadType]
  implicit val EadEsadType = Json.writes[EadEsadType]
  implicit val DocumentCertificateType = Json.writes[DocumentCertificateType]
  implicit val FirstTransporterTraderType = Json.writes[FirstTransporterTraderType]
  implicit val TransportArrangerTraderType = Json.writes[TransportArrangerTraderType]
  implicit val CompetentAuthorityDispatchOfficeType = Json.writes[CompetentAuthorityDispatchOfficeType]
  implicit val DeliveryPlaceCustomsOfficeTypeType2 = Json.writes[DeliveryPlaceCustomsOfficeTypeType2]
  implicit val DeliveryPlaceTraderTypeType3 = Json.writes[DeliveryPlaceTraderTypeType3]
  implicit val PlaceOfDispatchTraderType = Json.writes[PlaceOfDispatchTraderType]
  implicit val ConsignorTraderType = Json.writes[ConsignorTraderType]
  implicit val ExciseMovementTypeType5 = Json.writes[ExciseMovementTypeType5]
  implicit val ConsigneeTraderTypeType = Json.writes[ConsigneeTraderTypeType]
  implicit val EADESADContainerType = Json.writes[EADESADContainerType]
  implicit val BodyTypeType11 = Json.writes[BodyTypeType11]
  implicit val IE801Type = Json.writes[IE801Type]

  //IE802

  implicit val ReminderMessageType = Writes[ReminderMessageType] {
    t => JsString(t.toString)
  }
  implicit val LSDReminderInformationType = Json.writes[LSDReminderInformationType]
  implicit val ExciseMovementTypeType3 = Json.writes[ExciseMovementTypeType3]
  implicit val AttributesTypeType7 = Json.writes[AttributesTypeType7]
  implicit val ReminderMessageForExciseMovementType = Json.writes[ReminderMessageForExciseMovementType]
  implicit val BodyTypeType8 = Json.writes[BodyTypeType8]
  implicit val IE802Type = Json.writes[IE802Type]

  //IE803

  implicit val NotificationType = Writes[NotificationType] {
    t => JsString(t.toString)
  }
  implicit val DownstreamArcType = Json.writes[DownstreamArcType]
  implicit val ExciseNotificationType = Json.writes[ExciseNotificationType]
  implicit val NotificationOfDivertedEADESADType = Json.writes[NotificationOfDivertedEADESADType]
  implicit val BodyTypeType12 = Json.writes[BodyTypeType12]
  implicit val IE803Type = Json.writes[IE803Type]

  //IE807

  implicit val LSDComplementaryInformationTypeType6 = Json.writes[LSDComplementaryInformationTypeType6]
  implicit val ReferenceEventReportType = Json.writes[ReferenceEventReportType]
  implicit val ReferenceControlReportType = Json.writes[ReferenceControlReportType]
  implicit val AttributesTypeType6 = Json.writes[AttributesTypeType6]
  implicit val InterruptionOfMovementType = Json.writes[InterruptionOfMovementType]
  implicit val BodyTypeType7 = Json.writes[BodyTypeType7]
  implicit val IE807Type = Json.writes[IE807Type]

  //IE810

  implicit val LSDComplementaryInformationTypeType9 = Json.writes[LSDComplementaryInformationTypeType9]
  implicit val CancellationType = Json.writes[CancellationType]
  implicit val ExciseMovementEadType = Json.writes[ExciseMovementEadType]
  implicit val AttributesTypeType10 = Json.writes[AttributesTypeType10]
  implicit val CancellationOfEADType = Json.writes[CancellationOfEADType]
  implicit val BodyTypeType13 = Json.writes[BodyTypeType13]
  implicit val IE810Type = Json.writes[IE810Type]

  //IE813

  implicit val GuarantorTraderType = Json.writes[GuarantorTraderType]
  implicit val MovementGuaranteeType = Json.writes[MovementGuaranteeType]
  implicit val DeliveryPlaceCustomsOfficeTypeType = Json.writes[DeliveryPlaceCustomsOfficeTypeType]
  implicit val DeliveryPlaceTraderTypeType2 = Json.writes[DeliveryPlaceTraderTypeType2]
  implicit val NewConsigneeTraderTypeType = Json.writes[NewConsigneeTraderTypeType]
  implicit val LSDSealInformationTypeType2 = Json.writes[LSDSealInformationTypeType2]
  implicit val ChangedDestinationTypeCode = Writes[ChangedDestinationTypeCode] {
    code => JsString(code.toString)
  }
  implicit val LSDComplementaryInformationTypeType5 = Json.writes[LSDComplementaryInformationTypeType5]
  implicit val TransportDetailsTypeType2 = Json.writes[TransportDetailsTypeType2]
  implicit val NewTransporterTraderTypeType2 = Json.writes[NewTransporterTraderTypeType2]
  implicit val DestinationChangedTypeType = Json.writes[DestinationChangedTypeType]
  implicit val UpdateEadEsadType = Json.writes[UpdateEadEsadType]
  implicit val NewTransportArrangerTraderTypeType2 = Json.writes[NewTransportArrangerTraderTypeType2]
  implicit val AttributesTypeType5 = Json.writes[AttributesTypeType5]
  implicit val ChangeOfDestinationType = Json.writes[ChangeOfDestinationType]
  implicit val BodyTypeType6 = Json.writes[BodyTypeType6]
  implicit val IE813Type = Json.writes[IE813Type]

  //IE818

  implicit val LSDComplementaryInformationTypeType4 = Json.writes[LSDComplementaryInformationTypeType4]
  implicit val UnsatisfactoryReasonType = Json.writes[UnsatisfactoryReasonType]
  implicit val IndicatorOfShortageOrExcess = Writes[IndicatorOfShortageOrExcess] {
    i => JsString(i.toString)
  }
  implicit val GlobalConclusionOfReceipt = Writes[GlobalConclusionOfReceipt] {
    r => JsString(r.toString)
  }
  implicit val BodyReportOfReceiptExportType = Json.writes[BodyReportOfReceiptExportType]
  implicit val ReportOfReceiptExportType = Json.writes[ReportOfReceiptExportType]
  implicit val DestinationOfficeType = Json.writes[DestinationOfficeType]
  implicit val DeliveryPlaceTraderTypeType = Json.writes[DeliveryPlaceTraderTypeType]
  implicit val ExciseMovementTypeType2 = Json.writes[ExciseMovementTypeType2]
  implicit val ConsigneeTraderType = Json.writes[ConsigneeTraderType]
  implicit val AttributesTypeType4 = Json.writes[AttributesTypeType4]
  implicit val AcceptedOrRejectedReportOfReceiptExportType = Json.writes[AcceptedOrRejectedReportOfReceiptExportType]
  implicit val BodyTypeType5 = Json.writes[BodyTypeType5]
  implicit val IE818Type = Json.writes[IE818Type]

  //IE819

  implicit val LSDComplementaryInformationTypeType11 = Json.writes[LSDComplementaryInformationTypeType11]
  implicit val AlertOrRejectionOfEadEsadReasonType = Json.writes[AlertOrRejectionOfEadEsadReasonType]
  implicit val AlertOrRejectionType = Json.writes[AlertOrRejectionType]
  implicit val DestinationOfficeTypeType = Json.writes[DestinationOfficeTypeType]
  implicit val ExciseMovementTypeType6 = Json.writes[ExciseMovementTypeType6]
  implicit val ConsigneeTraderTypeType3 = Json.writes[ConsigneeTraderTypeType3]
  implicit val AttributesTypeType12 = Json.writes[AttributesTypeType12]
  implicit val AlertOrRejectionOfEADESADType = Json.writes[AlertOrRejectionOfEADESADType]
  implicit val BodyTypeType15 = Json.writes[BodyTypeType15]
  implicit val IE819Type = Json.writes[IE819Type]

  //IE829

  implicit val ExportAcceptanceType = Json.writes[ExportAcceptanceType]
  implicit val ExportPlaceCustomsOfficeType = Json.writes[ExportPlaceCustomsOfficeType]
  implicit val ExciseMovementEadTypeType = Json.writes[ExciseMovementEadTypeType]
  implicit val ConsigneeTraderTypeType4 = Json.writes[ConsigneeTraderTypeType4]
  implicit val AttributesTypeType13 = Json.writes[AttributesTypeType13]
  implicit val NotificationOfAcceptedExportType = Json.writes[NotificationOfAcceptedExportType]
  implicit val BodyTypeType16 = Json.writes[BodyTypeType16]
  implicit val IE829Type = Json.writes[IE829Type]

  //IE837

  implicit val MessageRoleCode = Writes[MessageRoleCode] {
    code => JsString(code.toString)
  }
  implicit val LSDComplementaryInformationTypeType2 = Json.writes[LSDComplementaryInformationTypeType2]
  implicit val SubmitterType = Writes[SubmitterType] {
    t => JsString(t.toString)
  }
  implicit val ExciseMovementTypeType = Json.writes[ExciseMovementTypeType]
  implicit val AttributesTypeType3 = Json.writes[AttributesTypeType3]
  implicit val ExplanationOnDelayForDeliveryType = Json.writes[ExplanationOnDelayForDeliveryType]
  implicit val BodyTypeType3 = Json.writes[BodyTypeType3]
  implicit val IE837Type = Json.writes[IE837Type]

  //IE839

  implicit val CustomsRejectionReasonCode = Writes[CustomsRejectionReasonCode] {
    code => JsString(code.toString)
  }
  implicit val DiagnosisCode = Writes[DiagnosisCode] {
    code => JsString(code.toString)
  }
  implicit val DiagnosisType = Json.writes[DiagnosisType]
  implicit val NEadSubType = Json.writes[NEadSubType]
  implicit val CEadValType = Json.writes[CEadValType]
  implicit val RejectionType = Json.writes[RejectionType]
  implicit val ExportCrossCheckingDiagnosesType = Json.writes[ExportCrossCheckingDiagnosesType]
  implicit val ExportPlaceCustomsOfficeTypeType = Json.writes[ExportPlaceCustomsOfficeTypeType]
  implicit val ConsigneeTraderTypeType5 = Json.writes[ConsigneeTraderTypeType5]
  implicit val AttributesTypeType14 = Json.writes[AttributesTypeType14]
  implicit val RefusalByCustomsType = Json.writes[RefusalByCustomsType]
  implicit val BodyTypeType17 = Json.writes[BodyTypeType17]
  implicit val IE839Type = Json.writes[IE839Type]

  //IE840

  implicit val LSDAssociatedInformationType = Json.writes[LSDAssociatedInformationType]
  implicit val LSDSealInformationType = Json.writes[LSDSealInformationType]
  implicit val LSDComplementaryInformationTypeType = Json.writes[LSDComplementaryInformationTypeType]
  implicit val LSDEvidenceTypeComplementType = Json.writes[LSDEvidenceTypeComplementType]
  implicit val LSDReferenceOfEvidenceType = Json.writes[LSDReferenceOfEvidenceType]
  implicit val LSDIssuingAuthorityType = Json.writes[LSDIssuingAuthorityType]
  implicit val LSDCommentsType = Json.writes[LSDCommentsType]
  implicit val LSDSubmittingPersonComplementType = Json.writes[LSDSubmittingPersonComplementType]
  implicit val LSDPlaceOfEventType = Json.writes[LSDPlaceOfEventType]
  implicit val LSDAcoComplementaryInformationType = Json.writes[LSDAcoComplementaryInformationType]
  implicit val TraderPersonType = Writes[TraderPersonType] {
    t => JsString(t.toString)
  }
  implicit val MeansOfTransportType = Json.writes[MeansOfTransportType]
  implicit val GoodsItemType = Json.writes[GoodsItemType]
  implicit val PersonInvolvedInMovementTraderType = Json.writes[PersonInvolvedInMovementTraderType]
  implicit val LSDShortDescriptionOfOtherAccompanyingDocumentType = Json.writes[LSDShortDescriptionOfOtherAccompanyingDocumentType]
  implicit val OtherAccompanyingDocumentType = Writes[OtherAccompanyingDocumentType] {
    t => JsString(t.toString)
  }
  implicit val ReportMessageType = Writes[ReportMessageType] {
    t => JsString(t.toString)
  }
  implicit val BodyEventReportType = Json.writes[BodyEventReportType]
  implicit val TransportDetailsType = Json.writes[TransportDetailsType]
  implicit val NewTransporterTraderType = Json.writes[NewTransporterTraderType]
  implicit val NewTransportArrangerTraderType = Json.writes[NewTransportArrangerTraderType]
  implicit val EvidenceOfEventType = Json.writes[EvidenceOfEventType]
  implicit val EventReportType = Json.writes[EventReportType]
  implicit val OtherAccompanyingDocumentTypeType = Json.writes[OtherAccompanyingDocumentTypeType]
  implicit val ExciseMovementType = Json.writes[ExciseMovementType]
  implicit val HeaderEventReportType = Json.writes[HeaderEventReportType]
  implicit val AttributesTypeType2 = Json.writes[AttributesTypeType2]
  implicit val EventReportEnvelopeType = Json.writes[EventReportEnvelopeType]
  implicit val BodyTypeType2 = Json.writes[BodyTypeType2]
  implicit val IE840Type = Json.writes[IE840Type]

  //IE871

  implicit val LSDExplanationType = Json.writes[LSDExplanationType]
  implicit val LSDGlobalExplanationType = Json.writes[LSDGlobalExplanationType]
  implicit val BodyAnalysisType = Json.writes[BodyAnalysisType]
  implicit val AnalysisType = Json.writes[AnalysisType]
  implicit val ConsignorTraderTypeType2 = Json.writes[ConsignorTraderTypeType2]
  implicit val ExciseMovementTypeType7 = Json.writes[ExciseMovementTypeType7]
  implicit val ConsigneeTraderTypeType6 = Json.writes[ConsigneeTraderTypeType6]
  implicit val AttributesTypeType15 = Json.writes[AttributesTypeType15]
  implicit val ExplanationOnReasonForShortageType = Json.writes[ExplanationOnReasonForShortageType]
  implicit val BodyTypeType18 = Json.writes[BodyTypeType18]
  implicit val IE871Type = Json.writes[IE871Type]

  //IE881

  implicit val LSDReferenceOfSupportingDocumentTypeType = Json.writes[LSDReferenceOfSupportingDocumentTypeType]
  implicit val LSDSupportingDocumentDescriptionTypeType = Json.writes[LSDSupportingDocumentDescriptionTypeType]
  implicit val LSDManualClosureRejectionComplementType = Json.writes[LSDManualClosureRejectionComplementType]
  implicit val LSDManualClosureRequestReasonCodeComplementTypeType = Json.writes[LSDManualClosureRequestReasonCodeComplementTypeType]
  implicit val LSDComplementaryInformationTypeType12 = Json.writes[LSDComplementaryInformationTypeType12]
  implicit val BodyManualClosureTypeType = Json.writes[BodyManualClosureTypeType]
  implicit val SupportingDocumentsTypeType = Json.writes[SupportingDocumentsTypeType]
  implicit val AttributesTypeType16 = Json.writes[AttributesTypeType16]
  implicit val ManualClosureResponseType = Json.writes[ManualClosureResponseType]
  implicit val BodyTypeType19 = Json.writes[BodyTypeType19]
  implicit val IE881Type = Json.writes[IE881Type]

  //IE905

  implicit val StatusType = Writes[StatusType] {
    t => JsString(t.toString)
  }
  implicit val RequestedMessageType = Writes[RequestedMessageType] {
    t => JsString(t.toString)
  }
  implicit val AttributesType = Json.writes[AttributesType]
  implicit val StatusResponseType = Json.writes[StatusResponseType]
  implicit val BodyType = Json.writes[BodyType]
  implicit val IE905Type = Json.writes[IE905Type]

}

