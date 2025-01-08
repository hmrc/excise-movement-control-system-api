package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import play.api.mvc.ActionTransformer
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.ParsedXmlRequest

import scala.concurrent.{ExecutionContext, Future}

class CorrelationIdAction()(implicit val executionContext: ExecutionContext) extends ActionTransformer[ParsedXmlRequest, ParsedXmlRequest] {

  override def transform[A](request: ParsedXmlRequest[A]): Future[ParsedXmlRequest[A]] = {
    Future.successful(request)
  }

}
