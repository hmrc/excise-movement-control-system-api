package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.AuthAction
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GetMovementController @Inject()(
                                       authAction: AuthAction,
                                       movementService: MovementService,
                                       cc: ControllerComponents
                                     )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def getMovement(movementId: String): Action[AnyContent] = {

    authAction.async(parse.default) {
      implicit request => movementService.getMovementById(movementId).map {
      case Some(_) => Ok("ok :)")
      case None => BadRequest("sad :(") //TODO error
    }}

  }

}
