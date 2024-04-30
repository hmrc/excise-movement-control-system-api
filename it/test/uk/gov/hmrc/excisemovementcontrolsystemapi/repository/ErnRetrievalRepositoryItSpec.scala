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
import org.mongodb.scala.model.Filters
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ErnRetrieval
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import play.api.inject.bind
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import java.time.temporal.ChronoUnit


class ErnRetrievalRepositoryItSpec extends PlaySpec
  with DefaultPlayMongoRepositorySupport[ErnRetrieval]
  with GuiceOneAppPerSuite {

  private val mockTimeService = mock[DateTimeService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(mockTimeService)
    ).build()

  override protected lazy val repository: ErnRetrievalRepository = app.injector.instanceOf[ErnRetrievalRepository]

  "save" should {
    "save when there isn't one there already" in {
      val fixedInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(fixedInstant)

      repository.save("testErn").futureValue

      find(Filters.eq("ern", "testErn")).futureValue.head mustBe ErnRetrieval("testErn", fixedInstant)
    }

    "update the lastRetrieved when there is one there already" in {
      val originalInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS).minusSeconds(60)
      val updatedInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(updatedInstant)

      insert(ErnRetrieval("testErn", originalInstant))

      repository.save("testErn").futureValue

      find(Filters.eq("ern", "testErn")).futureValue.head mustBe ErnRetrieval("testErn", updatedInstant)
    }
  }

  "getLastRetrieved" should {
    "return none when ern does not exist" in {
      val lastRetrieved = repository.getLastRetrieved("testErn").futureValue

      lastRetrieved mustBe None
    }

    "return the lastRetrieved for an ern that exists" in {
      val lastRetrieved = Instant.now.truncatedTo(ChronoUnit.MILLIS)

      insert(ErnRetrieval("testErn", lastRetrieved)).futureValue

      val actualLastRetrieved = repository.getLastRetrieved("testErn").futureValue

      actualLastRetrieved mustBe Some(lastRetrieved)
    }
  }
}
