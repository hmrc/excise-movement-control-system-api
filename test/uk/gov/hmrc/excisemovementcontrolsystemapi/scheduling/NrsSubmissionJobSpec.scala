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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnectorNew
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class NrsSubmissionJobSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with NrsTestData {

  implicit val ec: ExecutionContext     = ExecutionContext.global
  val mockNrsConnector: NrsConnectorNew = mock[NrsConnectorNew]
  val mockNrsService: NrsService        = mock[NrsService]
  val appConfig: AppConfig              = app.injector.instanceOf[AppConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[NrsConnectorNew].toInstance(mockNrsConnector),
      bind[NrsService].toInstance(mockNrsService)
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
      mockNrsConnector,
      mockNrsService
    )
  }

  private lazy val nrsSubmissionScheduler = app.injector.instanceOf[NrsSubmissionJob]

  "nrs submission scheduler must" - {
    "have the correct name" in {
      nrsSubmissionScheduler.name mustBe "nrs-submission-scheduler"
    }
    "be enabled" in {
      nrsSubmissionScheduler.enabled mustBe false
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
    "call the nrs service to find out if there is an nrs workitem to process" in {

      when(mockNrsService.processAllWithLock()(any))
        .thenReturn(Future.successful(Done))

      val result = nrsSubmissionScheduler.execute.futureValue

      result mustBe ScheduledJob.Result.Completed
      verify(mockNrsService, times(1)).processAllWithLock()(any)
    }
  }
}
