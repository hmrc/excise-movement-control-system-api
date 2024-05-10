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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.apache.pekko.Done
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.{Mockito, MockitoSugar}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.MessageConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{MessageParams, XmlMessageGeneratorFactory}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes.IE704
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{GetMessagesResponse, IE704Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Future

class MessageServiceItSpec
  extends AnyFreeSpec
    with Matchers
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[Movement]
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar {

  private val mockMessageConnector = mock[MessageConnector]
  private val mockDateTimeService = mock[DateTimeService]
  private val mockCorrelationIdService = mock[CorrelationIdService]

  private val now = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val newId = "some-id"
  private val utils = new EmcsUtils

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockMessageConnector)
  }

  override protected lazy val repository: MovementRepository =
    app.injector.instanceOf[MovementRepository]

  private lazy val service = app.injector.instanceOf[MessageService]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent),
        bind[MessageConnector].toInstance(mockMessageConnector),
        bind[DateTimeService].toInstance(mockDateTimeService),
        bind[CorrelationIdService].toInstance(mockCorrelationIdService)
      )
      .build()

  "MessageService" - {

    "must only add messages once, even if we process the message multiple times" in {

      val hc = HeaderCarrier()
      val ern = "testErn"
      val lrn = "lrnie8158976912"

      val ie704 = XmlMessageGeneratorFactory.generate(ern, MessageParams(IE704, "XI000001", localReferenceNumber = Some(lrn)))
      val messages = Seq(IE704Message.createFromXml(ie704))

      val expectedMovement = Movement(
        newId,
        None,
        lrn,
        ern,
        None,
        None,
        now,
        messages = Seq(Message(utils.encode(messages.head.toXml.toString()), "IE704", "XI000001", now))
      )

      when(mockDateTimeService.timestamp()).thenReturn(now)
      when(mockCorrelationIdService.generateCorrelationId()).thenReturn(newId)

      when(mockMessageConnector.getNewMessages(any)(any)).thenReturn(Future.successful(GetMessagesResponse(messages, 1)))

      when(mockMessageConnector.acknowledgeMessages(any)(any)).thenReturn(
        Future.failed(new RuntimeException()),
        Future.successful(Done)
      )

      service.updateMessages(ern)(hc).failed.futureValue
      val result1 = repository.getMovementByLRNAndERNIn(lrn, List(ern)).futureValue

      result1 must contain only expectedMovement

      service.updateMessages(ern)(hc).futureValue
      val result2 = repository.getMovementByLRNAndERNIn(lrn, List(ern)).futureValue

      result1 mustEqual result2
    }
  }
}
