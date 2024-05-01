package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import org.apache.pekko.Done
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class MessageConnector @Inject() () {

  def getNewMessages(ern: String)(implicit hc: HeaderCarrier): Future[Seq[IEMessage]] = ???

  def acknowledgeMessages(ern: String)(implicit hc: HeaderCarrier): Future[Done] = ???
}
