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
import org.scalactic.source.Position
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.slf4j.MDC
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ErnRetrieval
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import play.api.inject.bind
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}


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

  "getLastRetrieved" should {

    "return none and update the lastRetrieved time when ern does not exist" in {

      val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(instant)

      val lastRetrieved = repository.getLastRetrieved("testErn").futureValue

      lastRetrieved mustBe None

      find(Filters.eq("ern", "testErn")).futureValue.head mustBe ErnRetrieval("testErn", instant)
    }

    "return the lastRetrieved and update the lastRetrieved time for an ern that exists" in {

      val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(instant)

      val lastRetrieved = instant.minus(5, ChronoUnit.MINUTES)
      insert(ErnRetrieval("testErn", lastRetrieved)).futureValue

      val actualLastRetrieved = repository.getLastRetrieved("testErn").futureValue
      actualLastRetrieved mustBe Some(lastRetrieved)

      find(Filters.eq("ern", "testErn")).futureValue.head mustBe ErnRetrieval("testErn", instant)
    }

    mustPreserveMdc(repository.getLastRetrieved("testErn"))
  }

  private def mustPreserveMdc[A](f: => Future[A])(implicit pos: Position): Unit =
    "must preserve MDC" in {

      val ec = app.injector.instanceOf[ExecutionContext]

      MDC.put("test", "foo")

      f.map { _ =>
        MDC.get("test") mustEqual "foo"
      }(ec).futureValue
    }
}
