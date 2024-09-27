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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{MovementWorkItem, ProblemMovementsWorkItemRepo}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MessageService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class MovementsCorrectingJobSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  private val problemMovementsWorkItemRepo = mock[ProblemMovementsWorkItemRepo]
  private val messageService               = mock[MessageService]
  private val timeService                  = mock[DateTimeService]
  private val now                          = Instant.now
  private lazy val movementsCorrectingJob  = app.injector.instanceOf[MovementsCorrectingJob]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[ProblemMovementsWorkItemRepo].toInstance(problemMovementsWorkItemRepo),
      bind[MessageService].toInstance(messageService),
      bind[DateTimeService].toInstance(timeService)
    )
    .configure(
      "scheduler.movementsCorrectingJob.initialDelay"      -> "1 minutes",
      "scheduler.movementsCorrectingJob.interval"          -> "1 minute",
      "scheduler.movementsCorrectingJob.numberOfInstances" -> "1337"
    )
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset[Any](
      problemMovementsWorkItemRepo,
      messageService,
      timeService
    )
  }

  "polling new messages job must" - {
    "have the correct name" in {
      movementsCorrectingJob.name mustBe "movements-correcting-job"
    }
    "be enabled" in {
      movementsCorrectingJob.enabled mustBe true
    }
    "use initial delay from configuration" in {
      movementsCorrectingJob.initialDelay mustBe FiniteDuration(1, "minutes")
    }
    "use interval from configuration" in {
      movementsCorrectingJob.interval mustBe FiniteDuration(1, "minute")
    }
    "use numberOfInstance from configuration" in {
      movementsCorrectingJob.numberOfInstances mustBe 1337
    }
  }

  ".execute" - {

    "must correct movements for the movement id" - {
      "when a movement is found" - {

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
        when(problemMovementsWorkItemRepo.pullOutstanding(any[Instant], any[Instant]))
          .thenReturn(Future.successful(Some(workItem)))
        when(messageService.archiveAndFixProblemMovement(any)(any))
          .thenReturn(Future.successful(Done))

        val result = movementsCorrectingJob.execute.futureValue

        result mustBe ScheduledJob.Result.Completed
        verify(messageService).archiveAndFixProblemMovement(eqTo("12345"))(any)

      }
    }

    "must not correct movements" - {
      "when no movement is found" - {}
    }

  }
}
