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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import generated.NewMessagesDataResponse
import play.api.http.Status.OK
import play.api.libs.json.{Json, Reads}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.{AppConfig, Service}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector.GetMessagesException
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageReceiptSuccessResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IEMessage}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{AuditService, HttpHeader}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.nio.charset.StandardCharsets
import java.util.{Base64, UUID}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

@Singleton
class MessageConnector @Inject() (
  configuration: Configuration,
  httpClient: HttpClientV2,
  messageFactory: IEMessageFactory,
  dateTimeService: DateTimeService,
  auditService: AuditService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  private val service: Service    = configuration.get[Service]("microservice.services.eis")
  private val bearerToken: String = configuration.get[String]("microservice.services.eis.messages-bearer-token")

  private def enforceCorrelationId(hc: HeaderCarrier): HeaderCarrier =
    hc.headers(Seq(HttpHeader.xCorrelationId)).headOption match {
      case Some(_) => hc
      case None    =>
        val correlationId = UUID.randomUUID().toString
        logger.info(s"generated new correlation id: $correlationId")
        hc.withExtraHeaders(HttpHeader.xCorrelationId -> correlationId)
    }

  def getNewMessages(ern: String, batchId: String, jobId: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[GetMessagesResponse] = {
    logger.info(s"[MessageConnector]: Getting new messages")

    val hc2 = enforceCorrelationId(hc)

    val timestamp = dateTimeService.timestamp().asStringInMilliseconds

    httpClient
      .put(url"${service.baseUrl}/emcs/messages/v1/show-new-messages?exciseregistrationnumber=$ern")(hc2)
      .setHeader("X-Forwarded-Host" -> "MDTP")
      .setHeader("Source" -> "APIP")
      .setHeader("DateTime" -> timestamp)
      .setHeader("Authorization" -> s"Bearer $bearerToken")
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == OK) Future.fromTry {
          for {
            response <- parseJson[EISConsumptionResponse](response.body)
            messages <- getMessages(response)(hc2)
            count    <- countOfMessagesAvailable(response.message)
          } yield {
            val getMessagesResponse = GetMessagesResponse(messages, count)
            auditService.messageProcessingSuccess(ern, getMessagesResponse, batchId, jobId)(hc2)
            getMessagesResponse
          }
        }
        else {
          logger.warn(s"[MessageConnector]: Invalid status returned: ${response.status}")
          Future.failed(new RuntimeException("Invalid status returned"))
        }
      }
      .recoverWith { case NonFatal(e) =>
        auditService.messageProcessingFailure(
          ern = ern,
          failureReason = e.getMessage,
          batchId = batchId,
          jobId = jobId
        )(hc2)
        Future.failed(GetMessagesException(ern, e))
      }
  }

  def acknowledgeMessages(ern: String, batchId: String, jobId: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[MessageReceiptSuccessResponse] = {

    val hc2 = enforceCorrelationId(hc)

    logger.info(s"[MessageConnector]: Acknowledging messages")
    val timestamp = dateTimeService.timestamp().asStringInMilliseconds

    httpClient
      .put(url"${service.baseUrl}/emcs/messages/v1/message-receipt?exciseregistrationnumber=$ern")(hc2)
      .setHeader("X-Forwarded-Host" -> "MDTP")
      .setHeader("Source" -> "APIP")
      .setHeader("DateTime" -> timestamp)
      .setHeader("Authorization" -> s"Bearer $bearerToken")
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == OK) Future.fromTry {
          parseJson[MessageReceiptSuccessResponse](response.body)
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
    if (appConfig.oldAuditingEnabled) {
      xmlResponse.Messages.messagesoption.map(messageFactory.createIEMessage).tapEach {
        auditService.auditMessage(_)(headerCarrier)
      }
    } else xmlResponse.Messages.messagesoption.map(messageFactory.createIEMessage)
  }

  private def countOfMessagesAvailable(encodedMessage: String): Try[Int] = Try {
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
