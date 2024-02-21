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

package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.mockito.MockitoSugar.{reset, when}
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, TestUtils}
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.InProgress
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext

class ExciseNumberQueueWorkItemRepositoryItSpec extends PlaySpec
  with CleanMongoCollectionSupport
  with PlayMongoRepositorySupport[WorkItem[ExciseNumberWorkItem]]
  with IntegrationPatience
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with OptionValues
  with GuiceOneAppPerSuite {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val appConfig = app.injector.instanceOf[AppConfig]
  private lazy val dateTimeService = mock[DateTimeService]
  private lazy val timestamp = Instant.parse("2018-11-30T18:35:24.001234Z")
  private lazy val timestampInMillis = timestamp.truncatedTo(ChronoUnit.MILLIS)

  protected override val repository = new ExciseNumberQueueWorkItemRepository(
    appConfig,
    mongoComponent,
    dateTimeService
  )

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("mongodb.uri" -> mongoUri)
      .overrides(bind[DateTimeService].to(dateTimeService))

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(dateTimeService)

    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  "getWorkItemForErn" should {

    "return Work Item with that ern if one exists in the db " in {
      val expectedWorkItem = createWorkItemForErn("ern123")
      insert(expectedWorkItem).futureValue

      val result = repository.getWorkItemForErn("ern123").futureValue

      result mustBe Some(expectedWorkItem)
    }

    "return empty list if that ern does not exist in db" in {

      val result = repository.getWorkItemForErn("ern124").futureValue

      result mustBe None
    }

  }

  "updateWorkItem" should {

    "update Work Item in db" in {

      val originalWI = createWorkItemForErn("ern1", timestamp.minusSeconds(120))
      insert(originalWI).futureValue

      val updatedWI = originalWI.copy(
        item = originalWI.item.copy(fastPollRetriesLeft = 23),
        availableAt = Instant.parse("2023-11-23T15:25:21.123456Z"),
        receivedAt = timestamp.plusSeconds(10),
        status = InProgress,
        failureCount = 18
      )

      repository.saveUpdatedWorkItem(updatedWI).futureValue mustBe true

      val savedWorkItems = find(
          Filters.in("item.exciseNumber", "ern1"),
        ).futureValue

      withClue("should update rather than insert an item - ern is unique index") {
        savedWorkItems.size mustBe 1
      }

      val savedWorkItem = savedWorkItems.head

      withClue("should update the item") {
        savedWorkItem.item mustBe updatedWI.item
      }

      // The database is storing these as ISODates and so truncated to milliseconds
      withClue("should update availableAt") {
        savedWorkItem.availableAt mustBe updatedWI.availableAt.truncatedTo(ChronoUnit.MILLIS)
      }

      withClue("should update updatedAt") {
        savedWorkItem.updatedAt mustBe timestamp.truncatedTo(ChronoUnit.MILLIS)
      }

      withClue("should update ReceivedAt (lastSubmitted)") {
        savedWorkItem.receivedAt mustBe updatedWI.receivedAt.truncatedTo(ChronoUnit.MILLIS)
      }

      withClue("should update status") {
        savedWorkItem.status mustBe updatedWI.status
      }

      withClue("should update failure count") {
        savedWorkItem.failureCount mustBe updatedWI.failureCount
      }

    }
  }

  private def createWorkItemForErn(ern: String, updateAt: Instant = timestamp)  =
    TestUtils.createWorkItem(
      ern = ern,
      availableAt = timestampInMillis,
      updatedAt = timestampInMillis,
      receivedAt = timestampInMillis
    )
}
