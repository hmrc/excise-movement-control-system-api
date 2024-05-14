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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, MovementRepository}

import scala.concurrent.duration.FiniteDuration

class PollingNewMessagesJobSpec extends PlaySpec
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach {

  private val movementRepository = mock[MovementRepository]
  private val ernRetrievalRepository = mock[ErnRetrievalRepository]
  private lazy val pollingNewMessagesJob = app.injector.instanceOf[PollingNewMessagesJob]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MovementRepository].toInstance(movementRepository),
      bind[ErnRetrievalRepository].toInstance(ernRetrievalRepository),
    )
    .configure(
      "scheduler.pollingNewMessagesJob.initialDelay" -> "2 minutes",
      "scheduler.pollingNewMessagesJob.interval" -> "1 minute"
    ).build()

  "polling new messages job" when {
    "initialised" should {
      "have the correct name" in {
        pollingNewMessagesJob.name mustBe "polling-new-messages-job"
      }
      "be enabled" in {
        pollingNewMessagesJob.enabled mustBe true
      }
      "use initial delay from configuration" in {
        pollingNewMessagesJob.initialDelay mustBe FiniteDuration(2, "minutes")
      }
      "use interval from configuration" in {
        pollingNewMessagesJob.interval mustBe FiniteDuration(1, "minute")
      }
    }
  }

}
