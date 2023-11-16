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

package uk.gov.hmrc.excisemovementcontrolsystemapi.service

import dispatch.Future
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.WorkItemService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemRepository}

import java.time.Instant
import scala.concurrent.ExecutionContext

class WorkItemServiceSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  protected implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockWorkItemRepo = mock[WorkItemRepository[ExciseNumberWorkItem]]

  private val workItemService = new WorkItemService(mockWorkItemRepo)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockWorkItemRepo)
  }


  "create work item" should {
    "return the created work item" in {

      val ern = "ern123"
      val expectedWorkItem = ExciseNumberWorkItem(ern)

      when(mockWorkItemRepo.pushNew(any, any, any)).thenReturn(Future.successful(createWorkItem(expectedWorkItem)))

      val result = await(workItemService.createWorkItem(ern))

      result.item mustBe expectedWorkItem

    }

  }

  private def createWorkItem(exciseNumberWorkItem: ExciseNumberWorkItem) = {
    WorkItem(
      id = new ObjectId(),
      receivedAt = Instant.now,
      updatedAt = Instant.now,
      availableAt = Instant.now,
      status = ToDo,
      failureCount = 0,
      item = exciseNumberWorkItem
    )
  }
}

