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
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MiscodedMovementsWorkItemRepo, MovementWorkItem, ProblemMovementsWorkItemRepo}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{MessageService, MiscodedMovementService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class MiscodedMovementsCorrectingJobSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  private val miscodedMovementsWorkItemRepo = mock[MiscodedMovementsWorkItemRepo]
  private val miscodedMovementService       = mock[MiscodedMovementService]
  private val timeService                   = mock[DateTimeService]
  private val now                           = Instant.now
  private lazy val miscodedMovementsCorrectingJob  = app.injector.instanceOf[MiscodedMovementsCorrectingJob]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MiscodedMovementsWorkItemRepo].toInstance(miscodedMovementsWorkItemRepo),
      bind[MiscodedMovementService].toInstance(miscodedMovementService),
      bind[DateTimeService].toInstance(timeService)
    )
    .configure(
      "scheduler.miscodedMovementsCorrectingJob.initialDelay"      -> "1 minutes",
      "scheduler.miscodedMovementsCorrectingJob.interval"          -> "1 minute",
      "scheduler.miscodedMovementsCorrectingJob.numberOfInstances" -> "1337",
      "featureFlags.miscodedMovementsCorrectingEnabled"            -> true
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset[Any](
      miscodedMovementsWorkItemRepo,
      miscodedMovementService,
      timeService
    )
  }

  "miscoded movements correcting job must" - {
    "have the correct name" in {
      miscodedMovementsCorrectingJob.name mustBe "miscoded-movements-correcting-job"
    }
    "be enabled" in {
      miscodedMovementsCorrectingJob.enabled mustBe true
    }
    "use initial delay from configuration" in {
      miscodedMovementsCorrectingJob.initialDelay mustBe FiniteDuration(1, "minutes")
    }
    "use interval from configuration" in {
      miscodedMovementsCorrectingJob.interval mustBe FiniteDuration(1, "minute")
    }
    "use numberOfInstance from configuration" in {
      miscodedMovementsCorrectingJob.numberOfInstances mustBe 1337
    }
  }

  ".execute" - {

    "must correct miscoded movements for the movement id" - {
      "when a movement is found" in {

        val workItem = WorkItem(
          id = new ObjectId(),
          receivedAt = now,
          updatedAt = now,
          availableAt = now,
          status = ToDo,
          failureCount = 0,
          item = MovementWorkItem("12345")
        )

        when(timeService.timestamp()).thenReturn(now)
        when(miscodedMovementsWorkItemRepo.pullOutstanding(any[Instant], any[Instant]))
          .thenReturn(Future.successful(Some(workItem)))
        when(miscodedMovementService.archiveAndRecode(any))
          .thenReturn(Future.successful(Done))

        val result = miscodedMovementsCorrectingJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(miscodedMovementService).archiveAndRecode(eqTo("12345"))

      }
    }

    "must not correct movements" - {
      "when no movement is found" in {

        when(timeService.timestamp()).thenReturn(now)
        when(miscodedMovementsWorkItemRepo.pullOutstanding(any[Instant], any[Instant]))
          .thenReturn(Future.successful(None))

        val result = miscodedMovementsCorrectingJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(miscodedMovementService, times(0)).archiveAndRecode(eqTo("12345"))
      }
    }

  }
}