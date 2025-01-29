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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import cats.implicits.toFunctorOps
import generated.NewMessagesDataResponse
import org.apache.pekko.Done
import play.api.{Configuration, Logging}
import play.api.http.Status.OK
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.Service
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector.GetMessagesException
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageReceiptSuccessResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{AuditService, CorrelationIdService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

@Singleton
class MessageConnector @Inject() (
  configuration: Configuration,
  httpClient: HttpClientV2,
  correlationIdService: CorrelationIdService,
  messageFactory: IEMessageFactory,
  dateTimeService: DateTimeService,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends Logging {

  private val service: Service    = configuration.get[Service]("microservice.services.eis")
  private val bearerToken: String = configuration.get[String]("microservice.services.eis.messages-bearer-token")

  def getNewMessages(ern: String, batchId: String, jobId: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[GetMessagesResponse] = {
    logger.info(s"[MessageConnector]: Getting new messages")

    val correlationId = correlationIdService.generateCorrelationId()
    val timestamp     = dateTimeService.timestamp().asStringInMilliseconds

    httpClient
      .put(url"${service.baseUrl}/emcs/messages/v1/show-new-messages?exciseregistrationnumber=$ern")
      .setHeader("X-Forwarded-Host" -> "MDTP")
      .setHeader("X-Correlation-Id" -> correlationId)
      .setHeader("Source" -> "APIP")
      .setHeader("DateTime" -> timestamp)
      .setHeader("Authorization" -> s"Bearer $bearerToken")
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == OK) Future.fromTry {
          for {
            response <- parseJson[EISConsumptionResponse](response.body)
            messages <- getMessages(response)
            count    <- countOfMessagesAvailable(response.message)
          } yield GetMessagesResponse(messages, count)
        }
        else {
          logger.warn(s"[MessageConnector]: Invalid status returned: ${response.status}")
          Future.failed(new RuntimeException("Invalid status returned"))
        }
      }
      .recoverWith { case NonFatal(e) =>
        Future.failed(GetMessagesException(ern, e))
      }
  }

  def acknowledgeMessages(ern: String)(implicit hc: HeaderCarrier): Future[Done] = {

    logger.info(s"[MessageConnector]: Acknowledging messages")
    val correlationId = correlationIdService.generateCorrelationId()
    val timestamp     = dateTimeService.timestamp().asStringInMilliseconds

    httpClient
      .put(url"${service.baseUrl}/emcs/messages/v1/message-receipt?exciseregistrationnumber=$ern")
      .setHeader("X-Forwarded-Host" -> "MDTP")
      .setHeader("X-Correlation-Id" -> correlationId)
      .setHeader("Source" -> "APIP")
      .setHeader("DateTime" -> timestamp)
      .setHeader("Authorization" -> s"Bearer $bearerToken")
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == OK) Future.fromTry {
          parseJson[MessageReceiptSuccessResponse](response.body).as(Done)
        }
        else {
          logger.warn(s"[MessageConnector]: Invalid status returned: ${response.status}")
          Future.failed(new RuntimeException("Invalid status returned"))
        }
      }
  }

  private def parseJson[A](string: String)(implicit reads: Reads[A]): Try[A] =
    for {
      json     <- Try(Json.parse(string))
      response <- Try(json.as[A])
    } yield response

  private def getMessages(
    response: EISConsumptionResponse
  )(implicit headerCarrier: HeaderCarrier): Try[Seq[IEMessage]] = Try {
    val decodedMessage: String = base64Decode(response.message)
    val xmlResponse            = scalaxb.fromXML[NewMessagesDataResponse](scala.xml.XML.loadString(decodedMessage))
    xmlResponse.Messages.messagesoption.map(messageFactory.createIEMessage).tapEach {
      auditService.auditMessage(_)(headerCarrier)
    }
  }

  def countOfMessagesAvailable(encodedMessage: String): Try[Int] = Try {
    val newMessage = scala.xml.XML.loadString(base64Decode(encodedMessage))
    (newMessage \ "CountOfMessagesAvailable").text.toInt
  }

  private def base64Decode(string: String): String =
    new String(Base64.getDecoder.decode(string), StandardCharsets.UTF_8)
}

object MessageConnector {
  final case class GetMessagesException(ern: String, cause: Throwable) extends Throwable {
    override def getStackTrace: Array[StackTraceElement] = cause.getStackTrace

    override def getMessage: String = s"Failed to get messages for ern: $ern, cause: ${cause.getMessage}"
  }
}
