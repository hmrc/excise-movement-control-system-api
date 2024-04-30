package uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class ErnRetrieval(ern: String, lastRetrieved: Instant)

object ErnRetrieval {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  implicit lazy val format: OFormat[ErnRetrieval] = Json.format
}