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

import org.mockito.MockitoSugar
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{MessageConnector, TraderMovementConnector}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{MessageParams, XmlMessageGeneratorFactory}
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes.{IE801, IE802, IE818}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MiscodedMovementArchiveRepository, MovementArchiveRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import java.time.temporal.ChronoUnit

class MiscodedMovementArchivingItSpec
    extends AnyFreeSpec
    with Matchers
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[Movement]
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with MockitoSugar
    with OptionValues {

  private val now                         = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val mockDateTimeService         = mock[DateTimeService]
  private val mockMessageConnector        = mock[MessageConnector]
  private val mockTraderMovementConnector = mock[TraderMovementConnector]
  private val mockCorrelationIdService    = mock[CorrelationIdService]

  val movementRepository = app.injector.instanceOf[MovementRepository]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[MessageConnector].toInstance(mockMessageConnector),
      bind[DateTimeService].toInstance(mockDateTimeService),
      bind[CorrelationIdService].toInstance(mockCorrelationIdService),
      bind[TraderMovementConnector].toInstance(mockTraderMovementConnector)
    )
    .build()

  override protected val repository: MiscodedMovementArchiveRepository =
    app.injector.instanceOf[MiscodedMovementArchiveRepository]

  "fixProblemMovement" - {

    val utils          = new EmcsUtils()
    val messageFactory = IEMessageFactory()

    def formatXml(ern: String, params: MessageParams): String =
      utils.encode(
        messageFactory
          .createFromXml(
            params.messageType.value,
            XmlMessageGeneratorFactory.generate(ern, params)
          )
          .toXml
          .toString
      )

    "must archive the miscoded movement" in {

      val movement = Movement(None, "testLrn", "testConsignor", None, None, lastUpdated = now)

      when(mockDateTimeService.timestamp()).thenReturn(now)
      val miscodedMovementService = app.injector.instanceOf[MiscodedMovementService]

      movementRepository.ensureIndexes().futureValue
      movementRepository.save(movement).futureValue

      miscodedMovementService.archiveAndRecode(movement._id).futureValue

      val results = findAll().futureValue
      results.size mustBe 1
      results.head mustEqual movement
    }
  }
}
