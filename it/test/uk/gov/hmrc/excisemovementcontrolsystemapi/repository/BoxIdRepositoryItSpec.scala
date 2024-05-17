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

package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.mockito.MockitoSugar.when
import org.mongodb.scala.model.{Filters, Indexes}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.BoxIdRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import play.api.inject.bind

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class BoxIdRepositoryItSpec extends PlaySpec
  with DefaultPlayMongoRepositorySupport[BoxIdRecord]
  with GuiceOneAppPerSuite {

  private val mockTimeService = mock[DateTimeService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(mockTimeService)
    )
    .configure(
      "mongodb.boxId.TTL" -> "5 minutes"
    ).build()

  override protected lazy val repository: BoxIdRepository = app.injector.instanceOf[BoxIdRepository]

  "initialise" should {
    "setup the lastUpdated index" in {
      val lastUpdatedIdx = repository.indexes.find(_.getOptions.getName == "lastUpdated_ttl_index").value
      lastUpdatedIdx.getOptions.getExpireAfter(TimeUnit.MINUTES) mustBe 5
      lastUpdatedIdx.getKeys mustEqual Indexes.ascending("lastUpdated")
    }
    "setup the ern_boxId_index" in {
      val ern = repository.indexes.find(_.getOptions.getName == "ern_boxId_index").value
      ern.getOptions.isUnique mustBe true
      ern.getKeys mustEqual Indexes.compoundIndex(
        Indexes.ascending("ern"),
        Indexes.ascending("boxId")
      )
    }
  }

  "save" should {
    "save when there isn't one there already" in {
      val fixedInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(fixedInstant)

      repository.save("testErn", "testBoxId").futureValue

      find(Filters.eq("ern", "testErn")).futureValue.head mustBe BoxIdRecord("testErn", "testBoxId", fixedInstant)

    }
  }

}
