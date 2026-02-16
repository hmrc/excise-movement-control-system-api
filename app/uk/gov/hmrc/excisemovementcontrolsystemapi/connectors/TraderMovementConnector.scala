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

import generated.{MessageBodyType, MovementForTraderDataResponse}
import play.api.{Configuration, Logging}
import play.api.http.Status.{OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.Service
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.EISConsumptionResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.IEMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.HttpHeader
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

@Singleton
class TraderMovementConnector @Inject() (
  configuration: Configuration,
  httpClient: HttpClientV2,
  messageFactory: IEMessageFactory,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends Logging {

  private def enforceCorrelationId(hc: HeaderCarrier): HeaderCarrier =
    hc.headers(Seq(HttpHeader.xCorrelationId)).headOption match {
      case Some(_) => hc
      case None    =>
        val correlationId = UUID.randomUUID().toString
        logger.info(s"generated new correlation id: $correlationId")
        hc.withExtraHeaders(HttpHeader.xCorrelationId -> correlationId)
    }

  private val service: Service    = configuration.get[Service]("microservice.services.eis")
  private val bearerToken: String = configuration.get[String]("microservice.services.eis.movement-bearer-token")

  def getMovementMessages(ern: String, arc: String)(implicit hc: HeaderCarrier): Future[Seq[IEMessage]] = {
    logger.info(s"[TraderMovementConnector]: Getting movement messages")
    val timestamp = dateTimeService.timestamp().asStringInMilliseconds
    val hc2       = enforceCorrelationId(hc)

    httpClient
      .get(url"${service.baseUrl}/emcs/movements/v1/trader-movement?exciseregistrationnumber=$ern&arc=$arc")(hc2)
      .setHeader("X-Forwarded-Host" -> "MDTP")
      .setHeader("Source" -> "APIP")
      .setHeader("DateTime" -> timestamp)
      .setHeader("Authorization" -> s"Bearer $bearerToken")
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == OK) Future.fromTry {
          for {
            response <- parseJson[EISConsumptionResponse](response.body)
            messages <- getMessages(response)
          } yield messages
        }
        else if (response.status == UNPROCESSABLE_ENTITY) {
          Future.successful(Seq())
        } else {
          logger.warn(s"[TraderMovementConnector]: Invalid status returned: ${response.status}")
          Future.failed(new RuntimeException("[TraderMovementConnector]: Invalid status returned"))
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
  ): Try[Seq[IEMessage]] = Try {
    val decodedMessage: String = base64Decode(response.message)
    val xmlResponse            = scalaxb.fromXML[MovementForTraderDataResponse](scala.xml.XML.loadString(decodedMessage))
    xmlResponse.IE934.Body.MessagePackage.MessageBody.map(getIeMessage)
  }

  private def getIeMessage(messageBodyType: MessageBodyType): IEMessage = {
    val messageType = messageBodyType.TechnicalMessageType.toString
    val decodedXml  = scala.xml.XML.loadString(base64Decode(messageBodyType.MessageData.toString))
    messageFactory.createFromXml(messageType, decodedXml)
  }

  private def base64Decode(string: String): String =
    new String(Base64.getDecoder.decode(string), StandardCharsets.UTF_8)
}
