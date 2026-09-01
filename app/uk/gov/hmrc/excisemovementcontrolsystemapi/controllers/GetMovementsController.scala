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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import cats.data._
import cats.implicits.toFlatMapOps
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.http.HttpEntity.Strict
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions._
import uk.gov.hmrc.excisemovementcontrolsystemapi.filters.{MovementFilter, TraderType}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.EnrolmentRequest
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.MovementIdValidation
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, ExciseMovementResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services._
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService.DateTimeFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps
import scala.util.control.NonFatal

class GetMovementsController @Inject() (
  authAction: AuthAction,
  correlationIdAction: CorrelationIdAction,
  validateErnParameterAction: ValidateErnParameterAction,
  validateUpdatedSinceAction: ValidateUpdatedSinceAction,
  validateTraderTypeAction: ValidateTraderTypeAction,
  cc: ControllerComponents,
  movementService: MovementService,
  dateTimeService: DateTimeService,
  messageService: MessageService,
  movementIdValidator: MovementIdValidation,
  auditService: AuditService
)(implicit ec: ExecutionContext, materializer: Materializer)
    extends BackendController(cc)
    with Logging {

  def getMovements(
    ern: Option[String],
    lrn: Option[String],
    arc: Option[String],
    updatedSince: Option[String],
    traderType: Option[String]
  ): Action[AnyContent] =
    (authAction
      andThen correlationIdAction
      andThen validateErnParameterAction(ern)
      andThen validateUpdatedSinceAction(updatedSince)
      andThen validateTraderTypeAction(traderType)).async(parse.default) { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val filter =
        MovementFilter(
          ern,
          lrn,
          arc,
          updatedSince.map(Instant.parse(_)),
          traderType.map(trader => TraderType(trader, request.erns.toSeq))
        )

      val result = for {
        _       <- messageService.updateAllMessages(ern.fold(request.erns)(Set(_)))
        payload <- movementService
                     .streamMovementsByErn(request.erns.toSeq)
                     .pipe(sizedJsonArrayPayload)
                     .flatTap { case (count, _) => Future.successful(audit(count, filter)) }
                     .map(_._2)

      } yield {
        Result(ResponseHeader(OK), Strict(payload, Some("application/json")))
      }

      result.recover { case NonFatal(ex) =>
        logger.warn(
          s"Error getting movements for erns ${request.erns} with filters ern: $ern, lrn: $lrn, arc: $arc, updatedSince: $updatedSince, traderType: $traderType",
          ex
        )
        InternalServerError(
          Json.toJson(
            ErrorResponse(
              dateTimeService.timestamp(),
              "Error getting movements",
              "Unknown error while getting movements"
            )
          )
        )
      }
    }

  private def audit(movementCount: Int, filter: MovementFilter)(implicit
    request: EnrolmentRequest[AnyContent],
    hc: HeaderCarrier
  ): Unit =
    auditService.getInformationForGetMovements(filter, movementCount, request)

  private def sizedJsonArrayPayload(source: Source[Movement, NotUsed]): Future[(Int, ByteString)]    =
    source
      .map(createResponseFrom)
      .map(Json.toJson(_).toString())
      .grouped(2)
      .map(group => group.size -> ByteString(group.mkString(",")))
      .pipe(Source.single(0 -> ByteString("[")) ++ _ ++ Source.single(0 -> ByteString("]")))
      .runFold(0 -> ByteString.empty)(foldSizedPayload)

  private def foldSizedPayload(left: (Int, ByteString), right: (Int, ByteString)): (Int, ByteString) =
    left._1 + right._1 -> left._2.concat(right._2)

  def getMovement(movementId: String): Action[AnyContent] =
    (authAction andThen correlationIdAction).async(parse.default) { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val result = for {
        validatedMovementId <- validateMovementId(movementId)
        _                   <- EitherT.right(messageService.updateAllMessages(request.erns))
        movement            <- getMovementFromDb(validatedMovementId)
      } yield {
        val authorisedErns = request.erns
        val movementErns   = getErnsForMovement(movement)

        if (authorisedErns.intersect(movementErns).isEmpty) {
          logger.warn(
            s"[GetMovementsController] - Movement $movementId is not found within the data for authorisedERNs"
          )
          NotFound(
            Json.toJson(
              ErrorResponse(
                dateTimeService.timestamp(),
                "Movement not found",
                s"Movement $movementId is not found within the data for ERNs ${authorisedErns.mkString("/")}"
              )
            )
          )
        } else {
          auditService.getInformationForGetSpecificMovement(movementId, request)
          Ok(Json.toJson(createResponseFrom(movement)))
        }

      }

      result.merge
    }

  private def validateMovementId(movementId: String): EitherT[Future, Result, String] =
    EitherT.fromEither[Future](movementIdValidator.validateMovementId(movementId)).leftMap { x =>
      movementIdValidator.convertErrorToResponse(x, dateTimeService.timestamp())
    }

  private def getMovementFromDb(id: String): EitherT[Future, Result, Movement] =
    OptionT(movementService.getMovementById(id)).toRightF(
      Future.successful(
        NotFound(
          Json.toJson(
            ErrorResponse(dateTimeService.timestamp(), "Movement not found", s"Movement $id could not be found")
          )
        )
      )
    )

  private def getErnsForMovement(movement: Movement): Set[String] = {
    val messageRecipients = movement.messages.map(_.recipient)
    Set(Some(movement.consignorId), movement.consigneeId, messageRecipients).flatten
  }

  private def createResponseFrom(movement: Movement) =
    ExciseMovementResponse(
      movement._id,
      None,
      movement.localReferenceNumber,
      movement.consignorId,
      movement.consigneeId,
      movement.administrativeReferenceCode,
      Some(movement.lastUpdated.asStringInMilliseconds)
    )

}
