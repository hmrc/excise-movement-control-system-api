package uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._

case class GetMovementsRequest(
  exciseRegistrationNumber: Option[String],
  administrativeReferenceCode: Option[String],
  localReferenceNumber: Option[String],
  updatedSince: Option[String],
  traderType: Option[String]
)
object GetMovementsRequest {
  implicit val write: OWrites[GetMovementsRequest] =
    (
      (JsPath \ "exciseRegistrationNumber").writeNullable[String] and
        (JsPath \ "administrativeReferenceCode").writeNullable[String] and
        (JsPath \ "localReferenceNumber").writeNullable[String] and
        (JsPath \ "updatedSince").writeNullable[String] and
        (JsPath \ "traderType").writeNullable[String]
    )(unlift(GetMovementsRequest.unapply))
}

case class GetMovementsResponse(
  numberOfMovements: Int
)
object GetMovementsResponse {
  implicit val writes: OWrites[GetMovementsResponse] = Json.writes[GetMovementsResponse]

}

case class GetMovementsDetails(
  requestType: String = "AllMovements",
  request: GetMovementsRequest,
  response: GetMovementsResponse
)
object GetMovementsDetails {
  implicit val writes: OWrites[GetMovementsDetails] = Json.writes[GetMovementsDetails]

}
