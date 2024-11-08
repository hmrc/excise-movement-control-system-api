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

import org.mockito.Mockito
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

import java.time.{Instant, ZonedDateTime}
import scala.concurrent.duration.FiniteDuration

class NrsSubmissionSchedulerSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with NrsTestData {

  val mockNrsSubmissionWorkItemRepository: NRSWorkItemRepository = mock[NRSWorkItemRepository]
  val mockNrsConnector: NrsConnector                             = mock[NrsConnector]
  val appConfig: AppConfig                                       = app.injector.instanceOf[AppConfig]
  private lazy val nrsSubmissionScheduler                        = app.injector.instanceOf[NrsSubmissionScheduler]
  private val now                                                = Instant.now
  private val timeStamp                                          = ZonedDateTime.now()
  private val nrsMetadata                                        = NrsMetadata(
    businessId = "emcs",
    notableEvent = "excise-movement-control-system",
    payloadContentType = "application/json",
    payloadSha256Checksum = sha256Hash("payload for NRS"),
    userSubmissionTimestamp = timeStamp.toString,
    identityData = testNrsIdentityData,
    userAuthToken = testAuthToken,
    headerData = Map(),
    searchKeys = Map("ern" -> "123")
  )
  private val nrsPayLoad                                         = NrsPayload("encodepayload", nrsMetadata)
  override def fakeApplication(): Application                    = GuiceApplicationBuilder()
    .overrides(
      bind[NRSWorkItemRepository].toInstance(mockNrsSubmissionWorkItemRepository),
      bind[NrsConnector].toInstance(mockNrsConnector)
    )
    .configure(
      "scheduler.nrsSubmissionJob.initialDelay" -> "1 minutes",
      "scheduler.nrsSubmissionJob.interval"     -> "1 minute",
      "featureFlags.nrsSubmissionEnabled"       -> true
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset[Any](
      mockNrsSubmissionWorkItemRepository,
      mockNrsConnector
    )
  }

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
  }

}
