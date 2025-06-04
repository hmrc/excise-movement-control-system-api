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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ErnRetrievalRepository, ErnSubmissionRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class PollingNewMessagesJobSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  private val movementRepository         = mock[MovementRepository]
  private val ernSubmissionRepository    = mock[ErnSubmissionRepository]
  private val ernRetrievalRepository     = mock[ErnRetrievalRepository]
  private val messageService             = mock[MessageService]
  private val timeService                = mock[DateTimeService]
  private val now                        = Instant.now
  private lazy val pollingNewMessagesJob = app.injector.instanceOf[PollingNewMessagesJob]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MovementRepository].toInstance(movementRepository),
      bind[ErnSubmissionRepository].toInstance(ernSubmissionRepository),
      bind[ErnRetrievalRepository].toInstance(ernRetrievalRepository),
      bind[MessageService].toInstance(messageService),
      bind[DateTimeService].toInstance(timeService)
    )
    .configure(
      "scheduler.pollingNewMessagesJob.initialDelay"        -> "2 minutes",
      "scheduler.pollingNewMessagesJob.interval"            -> "1 minute",
      "scheduler.pollingNewMessagesJob.fastPollingInterval" -> "5 minutes",
      "scheduler.pollingNewMessagesJob.fastPollingCutoff"   -> "15 minutes",
      "scheduler.pollingNewMessagesJob.slowPollingInterval" -> "30 minutes",
      "scheduler.pollingNewMessagesJob.numberOfInstances"   -> "1337"
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset[Any](
      movementRepository,
      ernSubmissionRepository,
      ernRetrievalRepository,
      messageService,
      timeService
    )
  }

  "polling new messages job must" - {
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
    "use numberOfInstance from configuration" in {
      pollingNewMessagesJob.numberOfInstances mustBe 1337
    }
  }

  ".execute" - {

    "must update movements for the ern" - {

      "when the last submission for the ern is outside of the fast polling cutoff" - {

        "and last retrieved for the ern is inside of the slow polling interval" in {

          val ernsAndLastReceived  = Map.empty[String, Instant]
          val ernsAndLastSubmitted = Map("GB12345678900" -> now.minus(16, ChronoUnit.MINUTES))
          when(timeService.timestamp()).thenReturn(now)
          when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
          when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
          when(ernRetrievalRepository.getLastRetrieved(any))
            .thenReturn(Future.successful(Some(now.minus(31, ChronoUnit.MINUTES))))
          when(messageService.updateMessages(any, any, any)(any))
            .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

          val result = pollingNewMessagesJob.execute.futureValue

          result mustBe ScheduledJob.Result.Completed
          verify(messageService)
            .updateMessages(eqTo("GB12345678900"), eqTo(Some(now.minus(31, ChronoUnit.MINUTES))), any)(any)
        }
      }

      "when the last submission for the ern is inside of the fast polling cutoff" - {

        "and last retrieved for the ern is inside of the fast polling interval" in {

          val ernsAndLastReceived  = Map.empty[String, Instant]
          val ernsAndLastSubmitted = Map("GB12345678900" -> now.minus(14, ChronoUnit.MINUTES))
          when(timeService.timestamp()).thenReturn(now)
          when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
          when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
          when(ernRetrievalRepository.getLastRetrieved(any))
            .thenReturn(Future.successful(Some(now.minus(6, ChronoUnit.MINUTES))))
          when(messageService.updateMessages(any, any, any)(any))
            .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

          val result = pollingNewMessagesJob.execute.futureValue

          result mustBe ScheduledJob.Result.Completed
          verify(messageService)
            .updateMessages(eqTo("GB12345678900"), eqTo(Some(now.minus(6, ChronoUnit.MINUTES))), any)(any)
        }
      }

      "when the last received for the ern is outside of the fast polling cutoff" - {

        "and last retrieved for the ern is inside of the slow polling interval" in {

          val ernsAndLastReceived  = Map("GB12345678900" -> now.minus(16, ChronoUnit.MINUTES))
          val ernsAndLastSubmitted = Map.empty[String, Instant]
          when(timeService.timestamp()).thenReturn(now)
          when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
          when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
          when(ernRetrievalRepository.getLastRetrieved(any))
            .thenReturn(Future.successful(Some(now.minus(31, ChronoUnit.MINUTES))))
          when(messageService.updateMessages(any, any, any)(any))
            .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

          val result = pollingNewMessagesJob.execute.futureValue

          result mustBe ScheduledJob.Result.Completed
          verify(messageService)
            .updateMessages(eqTo("GB12345678900"), eqTo(Some(now.minus(31, ChronoUnit.MINUTES))), any)(any)
        }
      }

      "when the last received for the ern is inside of the fast polling cutoff" - {

        "and last retrieved for the ern is inside of the fast polling interval" in {

          val ernsAndLastReceived  = Map("GB12345678900" -> now.minus(14, ChronoUnit.MINUTES))
          val ernsAndLastSubmitted = Map.empty[String, Instant]
          when(timeService.timestamp()).thenReturn(now)
          when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
          when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
          when(ernRetrievalRepository.getLastRetrieved(any))
            .thenReturn(Future.successful(Some(now.minus(6, ChronoUnit.MINUTES))))
          when(messageService.updateMessages(any, any, any)(any))
            .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

          val result = pollingNewMessagesJob.execute.futureValue

          result mustBe ScheduledJob.Result.Completed
          verify(messageService)
            .updateMessages(eqTo("GB12345678900"), eqTo(Some(now.minus(6, ChronoUnit.MINUTES))), any)(any)
        }
      }
    }

    "must not update movements for the ern" - {

      "when the ern is invalid (has been filtered out)" in {

        val ernsAndLastReceived  = Map.empty[String, Instant]
        val ernsAndLastSubmitted = Map("Volvo Estate" -> now.minus(16, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(ernRetrievalRepository.getLastRetrieved(any))
          .thenReturn(Future.successful(Some(now.minus(31, ChronoUnit.MINUTES))))
        when(messageService.updateMessages(any, any, any)(any))
          .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(ernRetrievalRepository, never())
          .getLastRetrieved(eqTo("Volvo Estate"))
        verify(messageService, never())
          .updateMessages(eqTo("Volvo Estate"), eqTo(Some(now.minus(31, ChronoUnit.MINUTES))), any)(any)
      }

      "when the deadline is in the past" in {

        val ernsAndLastReceived  = Map("GB12345678900" -> now.minus(6, ChronoUnit.MINUTES))
        val ernsAndLastSubmitted = Map("GB12345678900" -> now.minus(6, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(
          now,
          now.plus(1, ChronoUnit.MINUTES)
        )
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(messageService.updateMessages(any, any, any)(any))
          .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Cancelled
        verify(messageService, never()).updateMessages(any, any, any)(any)
      }

      "when the last submission for the ern is outside of the fast polling cutoff" - {

        "and last retrieved for the ern is outside of the slow polling interval" in {

          val ernsAndLastReceived  = Map.empty[String, Instant]
          val ernsAndLastSubmitted = Map("GB12345678900" -> now.minus(16, ChronoUnit.MINUTES))
          when(timeService.timestamp()).thenReturn(now)
          when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
          when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
          when(ernRetrievalRepository.getLastRetrieved(any))
            .thenReturn(Future.successful(Some(now.minus(30, ChronoUnit.MINUTES))))
          when(messageService.updateMessages(any, any, any)(any))
            .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

          val result = pollingNewMessagesJob.execute.futureValue

          result mustBe ScheduledJob.Result.Completed
          verify(messageService, never).updateMessages(any, any, any)(any)
        }
      }

      "when the last submission for the ern is within the fast polling cutoff" - {

        "and last retrieved for the ern is inside of the fast polling interval" in {

          val ernsAndLastReceived  = Map.empty[String, Instant]
          val ernsAndLastSubmitted = Map("GB12345678900" -> now.minus(14, ChronoUnit.MINUTES))
          when(timeService.timestamp()).thenReturn(now)
          when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
          when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
          when(ernRetrievalRepository.getLastRetrieved(any))
            .thenReturn(Future.successful(Some(now.minus(5, ChronoUnit.MINUTES))))
          when(messageService.updateMessages(any, any, any)(any))
            .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

          val result = pollingNewMessagesJob.execute.futureValue

          result mustBe ScheduledJob.Result.Completed
          verify(messageService, never).updateMessages(any, any, any)(any)
        }
      }

      "when the last received for the ern is outside of the fast polling cutoff" - {

        "and last retrieved for the ern is outside of the slow polling interval" in {

          val ernsAndLastReceived  = Map("GB12345678900" -> now.minus(16, ChronoUnit.MINUTES))
          val ernsAndLastSubmitted = Map.empty[String, Instant]
          when(timeService.timestamp()).thenReturn(now)
          when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
          when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
          when(ernRetrievalRepository.getLastRetrieved(any))
            .thenReturn(Future.successful(Some(now.minus(30, ChronoUnit.MINUTES))))
          when(messageService.updateMessages(any, any, any)(any))
            .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

          val result = pollingNewMessagesJob.execute.futureValue

          result mustBe ScheduledJob.Result.Completed
          verify(messageService, never).updateMessages(any, any, any)(any)
        }
      }

      "when the last received for the ern is withing the fast polling cutoff" - {

        "and last retrieved for the ern is outside of the fast polling cutoff" in {

          val ernsAndLastReceived  = Map("GB12345678900" -> now.minus(14, ChronoUnit.MINUTES))
          val ernsAndLastSubmitted = Map.empty[String, Instant]
          when(timeService.timestamp()).thenReturn(now)
          when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
          when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
          when(ernRetrievalRepository.getLastRetrieved(any))
            .thenReturn(Future.successful(Some(now.minus(5, ChronoUnit.MINUTES))))
          when(messageService.updateMessages(any, any, any)(any))
            .thenReturn(Future.successful(MessageService.UpdateOutcome.Updated))

          val result = pollingNewMessagesJob.execute.futureValue

          result mustBe ScheduledJob.Result.Completed
          verify(messageService, never).updateMessages(any, any, any)(any)
        }
      }
    }
  }
}
