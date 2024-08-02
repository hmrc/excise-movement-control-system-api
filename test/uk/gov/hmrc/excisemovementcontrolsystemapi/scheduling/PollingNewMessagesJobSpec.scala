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
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
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
    extends PlaySpec
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
      "scheduler.pollingNewMessagesJob.slowPollingInterval" -> "30 minutes"
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

  "polling new messages job" when {
    "initialised" should {
      "have the correct name" ignore {
        pollingNewMessagesJob.name mustBe "polling-new-messages-job"
      }
      "be enabled" ignore {
        pollingNewMessagesJob.enabled mustBe true
      }
      "use initial delay from configuration" ignore {
        pollingNewMessagesJob.initialDelay mustBe FiniteDuration(2, "minutes")
      }
      "use interval from configuration" ignore {
        pollingNewMessagesJob.interval mustBe FiniteDuration(1, "minute")
      }
    }
  }

  "executing" when {
    "now is after the deadline"                                                                                              should {
      "not update messages for that ern" ignore {
        val ernsAndLastReceived  = Map("testErn" -> now.minus(6, ChronoUnit.MINUTES))
        val ernsAndLastSubmitted = Map("testErn" -> now.minus(6, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(
          now,
          now.plus(1, ChronoUnit.MINUTES)
        )
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Cancelled
        verify(messageService, never()).updateMessages(any)(any)
      }
    }
    "last retrieved for an ern is sooner than the fast polling interval and last received is within the fast polling cutoff" should {
      "not update messages for that ern" ignore {
        val ernsAndLastReceived  = Map("testErn" -> now.minus(6, ChronoUnit.MINUTES))
        val ernsAndLastSubmitted = Map.empty[String, Instant]
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(ernRetrievalRepository.getLastRetrieved(any))
          .thenReturn(Future.successful(Some(now.minus(2, ChronoUnit.MINUTES))))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService, never()).updateMessages(any)(any)
      }
    }
    "last retrieved for an ern is after than the fast polling interval and last received is within the fast polling cutoff"  should {
      "update messages for that ern" ignore {
        val ernsAndLastReceived  = Map("testErn" -> now.minus(6, ChronoUnit.MINUTES))
        val ernsAndLastSubmitted = Map.empty[String, Instant]
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(ernRetrievalRepository.getLastRetrieved(any))
          .thenReturn(Future.successful(Some(now.minus(6, ChronoUnit.MINUTES))))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService, times(1)).updateMessages(any)(any)
      }
    }
    "last received for an ern is sooner than the fast polling interval"                                                      should {
      "not update messages for that ern" ignore {
        // last received is inside of the fast polling interval
        val ernsAndLastReceived  = Map("testErn" -> now.minus(2, ChronoUnit.MINUTES))
        // last submitted is outside the fast polling interval
        val ernsAndLastSubmitted = Map("testErn" -> now.minus(6, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService, never()).updateMessages(any)(any)
      }
    }
    "last activity for an ern is later than the fast polling interval"                                                       should {
      "update messages for that ern" ignore {
        // last received is outside of the fast polling interval
        val ernsAndLastReceived  = Map("testErn" -> now.minus(6, ChronoUnit.MINUTES))
        // last submitted is outside the fast polling interval
        val ernsAndLastSubmitted = Map("testErn" -> now.minus(7, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService).updateMessages(eqTo("testErn"))(any)
      }
    }
    "last activity for an ern is later than the fast polling cutoff"                                                         should {
      "not update messages for that ern" ignore {
        // last received is outside of the fast polling cutoff
        val ernsAndLastReceived  = Map("testErn" -> now.minus(17, ChronoUnit.MINUTES))
        // last submitted is outside the fast polling cutoff
        val ernsAndLastSubmitted = Map("testErn" -> now.minus(16, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService, never()).updateMessages(any)(any)
      }
    }
    "last activity for an ern is later than the slow polling interval"                                                       should {
      "update messages for that ern" ignore {
        // last received is outside of the slow polling interval
        val ernsAndLastReceived  = Map("testErn" -> now.minus(32, ChronoUnit.MINUTES))
        // last submitted is outside the slow polling interval
        val ernsAndLastSubmitted = Map("testErn" -> now.minus(31, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService).updateMessages(eqTo("testErn"))(any)
      }
    }
    "last received and last submitted have different ern keys"                                                               should {
      "the merged erns are all updated" ignore {
        // last received is outside of the fast polling interval
        val ernsAndLastReceived  = Map("testErn" -> now.minus(6, ChronoUnit.MINUTES))
        // last submitted is outside the fast polling interval
        val ernsAndLastSubmitted = Map("testErn2" -> now.minus(7, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService).updateMessages(eqTo("testErn"))(any)
        verify(messageService).updateMessages(eqTo("testErn2"))(any)
      }
    }
    "if an ern only has a last submitted"                                                                                    should {
      "update messages for that ern" ignore {
        // last submitted is outside the fast polling interval
        val ernsAndLastSubmitted = Map("testErn" -> now.minus(7, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(Map.empty))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService).updateMessages(eqTo("testErn"))(any)
      }
    }
    "if an ern only has a last received"                                                                                     should {
      "update messages for that ern" ignore {
        // last received is outside of the fast polling interval
        val ernsAndLastReceived = Map("testErn" -> now.minus(6, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(Map.empty))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.successful(Done))

        val result = pollingNewMessagesJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService).updateMessages(eqTo("testErn"))(any)
      }
    }
    "updateMessages fails"                                                                                                   should {
      "fail the job" ignore {
        // last received is outside of the fast polling interval
        val ernsAndLastReceived  = Map("testErn" -> now.minus(6, ChronoUnit.MINUTES))
        // last submitted is outside the fast polling interval
        val ernsAndLastSubmitted = Map("testErn" -> now.minus(7, ChronoUnit.MINUTES))
        when(timeService.timestamp()).thenReturn(now)
        when(movementRepository.getErnsAndLastReceived).thenReturn(Future.successful(ernsAndLastReceived))
        when(ernSubmissionRepository.getErnsAndLastSubmitted).thenReturn(Future.successful(ernsAndLastSubmitted))
        when(messageService.updateMessages(any)(any)).thenReturn(Future.failed(new RuntimeException("error")))

        val result = pollingNewMessagesJob.execute.failed.futureValue

        result.getMessage mustBe "error"
      }
    }
  }

}
