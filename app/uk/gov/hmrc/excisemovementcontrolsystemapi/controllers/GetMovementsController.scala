package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import play.api.mvc.ControllerComponents
import play.api.mvc.Results.Ok
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsController @Inject()(
                                        cc: ControllerComponents
                                      )(implicit ec: ExecutionContext) extends BackendController(cc)  {

  def getMovements = Future.successful(

    Ok("blahblah")
  )
}
