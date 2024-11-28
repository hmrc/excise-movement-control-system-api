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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import org.apache.pekko.Done
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.MockitoSugar.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NRSWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsServiceNew
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.WorkItem
import scala.concurrent.duration.FiniteDuration

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class NrsSubmissionJobSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with NrsTestData {

  implicit val ec: ExecutionContext                              = ExecutionContext.global
  val mockNrsSubmissionWorkItemRepository: NRSWorkItemRepository = mock[NRSWorkItemRepository]
  val mockNrsConnector: NrsConnector                             = mock[NrsConnector]
  val mockNrsService: NrsServiceNew                              = mock[NrsServiceNew]
  val mockDateTimeService: DateTimeService                       = mock[DateTimeService]
  val appConfig: AppConfig                                       = app.injector.instanceOf[AppConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[NRSWorkItemRepository].toInstance(mockNrsSubmissionWorkItemRepository),
      bind[NrsConnector].toInstance(mockNrsConnector),
      bind[NrsServiceNew].toInstance(mockNrsService),
      bind[DateTimeService].toInstance(mockDateTimeService)
    )
    .configure(
      "scheduler.nrsSubmissionJob.initialDelay"      -> "1 minutes",
      "scheduler.nrsSubmissionJob.interval"          -> "1 minute",
      "scheduler.nrsSubmissionJob.numberOfInstances" -> 1,
      "featureFlags.nrsSubmissionEnabled"            -> true
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset[Any](
      mockNrsSubmissionWorkItemRepository,
      mockNrsConnector,
      mockNrsService
    )
  }

  private val timestamp = LocalDateTime.of(2024, 3, 2, 12, 30, 45, 100).toInstant(ZoneOffset.UTC)
  when(mockDateTimeService.timestamp()).thenReturn(timestamp)

  private lazy val nrsSubmissionScheduler = app.injector.instanceOf[NrsSubmissionJob]
  private val nrsMetadata                 = NrsMetadata(
    businessId = "emcs",
    notableEvent = "excise-movement-control-system",
    payloadContentType = "application/json",
    payloadSha256Checksum = sha256Hash("payload for NRS"),
    userSubmissionTimestamp = timestamp.toString,
    identityData = testNrsIdentityData,
    userAuthToken = testAuthToken,
    headerData = Map(),
    searchKeys = Map("ern" -> "123")
  )
  private val nrsPayLoad                  = NrsPayload("encodepayload", nrsMetadata)

  "nrs submission scheduler must" - {
    "have the correct name" in {
      nrsSubmissionScheduler.name mustBe "nrs-submission-scheduler"
    }
    "be enabled" in {
      nrsSubmissionScheduler.enabled mustBe true
    }
    "use initial delay from configuration" in {
      nrsSubmissionScheduler.initialDelay mustBe FiniteDuration(1, "minutes")
    }
    "use interval from configuration" in {
      nrsSubmissionScheduler.interval mustBe FiniteDuration(1, "minute")
    }
    "use the correct number of instances from configuration" in {
      nrsSubmissionScheduler.numberOfInstances mustBe 1
    }
    "submit to NRS when there is an outstanding nrs workitem to process" in {

      val workItem = WorkItem(
        id = new ObjectId(),
        receivedAt = timestamp,
        updatedAt = timestamp,
        availableAt = timestamp,
        status = ToDo,
        failureCount = 0,
        item = NrsSubmissionWorkItem(nrsPayLoad)
      )

      when(mockNrsSubmissionWorkItemRepository.pullOutstanding(any(), any()))
        .thenReturn(Future.successful(Some(workItem)))
      when(mockNrsService.submitNrs(any())(any))
        .thenReturn(Future.successful(Done))

      val result = nrsSubmissionScheduler.execute.futureValue

      result mustBe ScheduledJob.Result.Completed
      verify(mockNrsService).submitNrs(eqTo(workItem))(any)
    }

    "not call NRS when there are no outstanding nrs workitems to process" in {

      when(mockNrsSubmissionWorkItemRepository.pullOutstanding(any(), any()))
        .thenReturn(Future.successful(None))

      val result = nrsSubmissionScheduler.execute.futureValue

      result mustBe ScheduledJob.Result.Completed
      verify(mockNrsService, times(0)).submitNrs(any())(any)
    }
  }
}
