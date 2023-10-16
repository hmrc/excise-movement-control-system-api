/*
 * Copyright 2023 HM Revenue & Customs
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

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{reset, times, verify, verifyZeroInteractions, when}
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ShowNewMessageResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{GetNewMessageService, MovementMessageService}

import java.time.LocalDateTime
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

class PollingNewMessagesJobSpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  implicit override lazy val app: Application = new GuiceApplicationBuilder().build()
  implicit lazy val materializer: Materializer = app.materializer
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val appConfig = mock[AppConfig]
  private val newMessageService = mock[GetNewMessageService]
  private val movementService = mock[MovementMessageService]

  private val job = new PollingNewMessagesJob(
    newMessageService,
    movementService,
    appConfig
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(movementService, appConfig, newMessageService)

    when(appConfig.parallelism).thenReturn(5)
    when(movementService.getUniqueConsignorId).thenReturn(Future.successful(createSource))
  }

  "Job" should {

    "have interval defined" in {
      val interval = Duration.create(5, SECONDS)
      when(appConfig.interval).thenReturn(interval)

      job.interval mustBe interval
    }

    "have initial delay defined" in {
      val initialDelay = Duration.create(5, SECONDS)
      when(appConfig.initialDelay).thenReturn(initialDelay)

      job.initialDelay mustBe initialDelay
    }

    "Get Pending ERN from Mongo" in {
      await(job.executeInMutex)

      verify(movementService).getUniqueConsignorId
    }

    "send getNewMessage request for each pending ern" in {
      val newMessageResponse = ShowNewMessageResponse(
        LocalDateTime.of(2023, 5, 6, 9,10,13),
        "123",
        "any message"
      )
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.successful(Some(newMessageResponse)))

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-message Job ran successfully."
      val captor = ArgCaptor[String]
      verify(newMessageService, times(3)).getNewMessagesAndAcknowledge(captor.capture)(any)

      captor.values  mustBe Seq("1", "3", "4")

      withClue("Save the new messages to the cache") {
        verify(movementService).updateMovement("2", "1", "any message")
        verify(movementService).updateMovement("3", "3", "any message")
        verify(movementService).updateMovement("4", "4", "any message")
      }
    }


    "not process any message if no pending message exist" in {
      when(movementService.getUniqueConsignorId).thenReturn(Future.successful(Source(Seq.empty)))

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-message Job ran successfully."
      verifyZeroInteractions(newMessageService)
    }


    "not change status in the database if show new message API has errors" in {
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any))
        .thenReturn(Future.successful(None))

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-message Job ran successfully."

      verify(movementService, never()).saveMovementMessage(any)
    }

    "carry on polling if GetNewMessage Api fails" in {
      when(newMessageService.getNewMessagesAndAcknowledge(any)(any)).thenReturn(Future.successful(None))

      val result = await(job.executeInMutex)

      result.message mustBe "polling-new-message Job ran successfully."
      verify(movementService, never()).saveMovementMessage(any)
    }

    "return an error" when {
      "stream return an error" in {
        when(appConfig.parallelism).thenReturn(0)

        val result = await(job.executeInMutex)

        result.message mustBe "The execution of scheduled job polling-new-message failed with error 'size must be positive'. The next execution of the job will do retry."
        verify(movementService, never()).saveMovementMessage(any)
      }
    }
  }

  private def createSource: Source[Movement, NotUsed] = {
    Source(
      Seq(
        Movement("2","1", None),
        Movement("3","3", None),
        Movement("4","4", None)
      )
    )
  }
}
