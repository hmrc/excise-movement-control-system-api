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

///*
// * Copyright 2025 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers
//
//import com.google.inject.Inject
//import play.api.Logging
//import play.api.libs.json._
//import play.api.mvc.{Action, ControllerComponents}
//import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.InjectController.CsvRow
//import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
//import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
//import uk.gov.hmrc.internalauth.client._
//import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
//
//import java.time.Instant
//import scala.concurrent.ExecutionContext
//
//class InjectController @Inject() (
//  cc: ControllerComponents,
//  movementRepository: MovementRepository,
//  auth: BackendAuthComponents
//)(implicit ec: ExecutionContext)
//    extends BackendController(cc)
//    with Logging {
//
//  private val permission = Predicate.Permission(
//    Resource(ResourceType("excise-movement-control-system-api"), ResourceLocation("inject/submit")),
//    IAAction("ADMIN")
//  )
//
//  def submitBatch(): Action[JsValue] =
//    auth.authorizedAction(permission).async(parse.json[JsValue]) { implicit request: AuthenticatedRequest[JsValue, _] =>
//      withJsonBody[List[CsvRow]] { csvRows =>
//        movementRepository
//          .saveMovements(csvRows.map(_.toMovement))
//          .map(_ => Accepted)
//      }
//    }
//}
//
//object InjectController {
//
//  case class CsvRow(
//    pk: Int,
//    arcUk: Option[String],
//    localReferenceNumber: String,
//    consignorExciseNumber: String,
//    sequenceNumber: Int,
//    status: String,
//    createdOn: Instant,
//    consigneeExciseNumber: Option[String]
//  ) {
//    def toMovement: Movement =
//      Movement(
//        boxId = None,
//        localReferenceNumber = localReferenceNumber,
//        consignorId = consignorExciseNumber,
//        consigneeId = consigneeExciseNumber,
//        administrativeReferenceCode = arcUk,
//        lastUpdated = createdOn,
//        messages = Seq.empty
//      )
//  }
//  object CsvRow {
//    implicit val reads: Reads[CsvRow] = Json.reads[CsvRow]
//  }
//}
