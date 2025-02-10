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

import org.mockito.{Mockito, MockitoSugar}
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MovementArchiveRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import java.time.temporal.ChronoUnit

class MovementArchivingItSpec
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

  override protected val repository: MovementArchiveRepository = app.injector.instanceOf[MovementArchiveRepository]

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

    "must archive the problem movement" in {

      val hc         = HeaderCarrier()
      val consignor  = "testErn"
      val consignee1 = "testErn2"
      val consignee2 = "testErn3"
      val consignee3 = "testErn4"

      val rootLrn = "2"
      val rootArc = "arc"
      val root801 = formatXml(
        consignor,
        MessageParams(
          IE801,
          "XI000001",
          consigneeErn = Some(consignee1),
          localReferenceNumber = Some(rootLrn),
          administrativeReferenceCode = Some(rootArc)
        )
      )

      val secondLrn = "22"
      val secondArc = "arc2"
      val second801 = formatXml(
        consignor,
        MessageParams(
          IE801,
          "XI000002",
          consigneeErn = Some(consignee2),
          localReferenceNumber = Some(secondLrn),
          administrativeReferenceCode = Some(secondArc)
        )
      )
      val second818 = formatXml(
        consignor,
        MessageParams(
          IE818,
          "XI000004",
          consigneeErn = Some(consignee2),
          localReferenceNumber = None,
          administrativeReferenceCode = Some(secondArc)
        )
      )

      val thirdLrn = "23"
      val thirdArc = "arc3"
      val third801 = formatXml(
        consignor,
        MessageParams(
          IE801,
          "XI000003",
          consigneeErn = Some(consignee3),
          localReferenceNumber = Some(thirdLrn),
          administrativeReferenceCode = Some(thirdArc)
        )
      )
      val third802 = formatXml(
        consignor,
        MessageParams(
          IE802,
          "XI000006",
          consigneeErn = None,
          localReferenceNumber = None,
          administrativeReferenceCode = Some(thirdArc)
        )
      )

      val rootMovement = Movement(
        boxId = None,
        localReferenceNumber = rootLrn,
        consignorId = consignor,
        consigneeId = Some(consignee3), // note: consignee is wrong
        administrativeReferenceCode = Some(rootArc),
        lastUpdated = now.minus(1, ChronoUnit.DAYS),
        messages = Seq(
          Message(root801, "IE801", "XI000001", consignor, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(root801, "IE801", "XI000001", consignee1, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(second801, "IE801", "XI000002", consignor, Set.empty, now.minus(3, ChronoUnit.DAYS)),
          Message(second801, "IE801", "XI000002", consignee2, Set.empty, now.minus(3, ChronoUnit.DAYS)),
          Message(third801, "IE801", "XI000003", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
          Message(third801, "IE801", "XI000003", consignee3, Set.empty, now.minus(2, ChronoUnit.DAYS)),
          Message(third802, "IE802", "XI000006", consignor, Set.empty, now.minus(2, ChronoUnit.DAYS)),
          Message(second818, "IE818", "XI000005", consignee2, Set.empty, now.minus(1, ChronoUnit.DAYS))
        )
      )

      when(mockDateTimeService.timestamp()).thenReturn(now)
      val messageService = app.injector.instanceOf[MessageService]

      movementRepository.ensureIndexes().futureValue
      movementRepository.saveMovement(rootMovement).futureValue

      messageService.archiveAndFixProblemMovement(rootMovement._id)(hc).futureValue

      val results = findAll().futureValue
      results.size mustBe 1
      results.head mustEqual rootMovement.copy(lastUpdated = now)
    }
  }
}
