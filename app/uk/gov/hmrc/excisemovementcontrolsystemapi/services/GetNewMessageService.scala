package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import com.google.inject.ImplementedBy
import play.api.Logging
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageReceiptConnector, ShowNewMessagesConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetNewMessageServiceImpl @Inject()(
                                          showNewMessageConnector: ShowNewMessagesConnector,
                                          messageReceiptConnector: MessageReceiptConnector,
                                          newMessageParserService: NewMessageParserService
                                        )(implicit val ec: ExecutionContext )extends GetNewMessageService with Logging {

  def getNewMessagesAndAcknowledge(
                                    exciseNumber: String
                                  )(implicit hc: HeaderCarrier): Future[Option[EISConsumptionResponse]] = {
    showNewMessageConnector.get(exciseNumber).flatMap(response =>
      response.fold(
        _ => Future.successful(None),
        success => handleSuccess(exciseNumber, success)
      )
    )
  }

  private def handleSuccess(
                             exciseNumber: String,
                             newMessageResponse: EISConsumptionResponse
                           )(implicit hc: HeaderCarrier): Future[Option[EISConsumptionResponse]] = {

    val hasMessage = newMessageParserService.countOfMessagesAvailable(newMessageResponse.message) > 0

    if(!hasMessage) {
      logger.warn(s"No more new message available for Excise Registration Number: $exciseNumber")
      Future.successful(None)
    } else {
      messageReceiptConnector.put(exciseNumber).map {
        case Right(_) => Some(newMessageResponse)
        case Left(_) if hasMessage => Some(newMessageResponse)
        case Left(_) => None
      }
    }
  }
}

@ImplementedBy(classOf[GetNewMessageServiceImpl])
trait GetNewMessageService {
  def getNewMessagesAndAcknowledge(exciseNumber: String)(implicit hc: HeaderCarrier): Future[Option[EISConsumptionResponse]]
}

