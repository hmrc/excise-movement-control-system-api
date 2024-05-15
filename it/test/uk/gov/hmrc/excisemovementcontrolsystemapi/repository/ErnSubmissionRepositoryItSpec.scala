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
import org.scalactic.source.Position
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.slf4j.MDC
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ErnSubmission
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}


class ErnSubmissionRepositoryItSpec extends PlaySpec
  with DefaultPlayMongoRepositorySupport[ErnSubmission]
  with IntegrationPatience
  with GuiceOneAppPerSuite {

  private val mockTimeService = mock[DateTimeService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(mockTimeService)
    )
    .configure(
      "mongodb.ernSubmission.TTL" -> "5 minutes"
    ).build()

  override protected lazy val repository: ErnSubmissionRepository = app.injector.instanceOf[ErnSubmissionRepository]

  "initialise" should {
    "setup the lastSubmitted index" in {
      val lastSubmittedIdx = repository.indexes.find(_.getOptions.getName == "lastSubmitted_ttl_index").value
      lastSubmittedIdx.getOptions.getExpireAfter(TimeUnit.MINUTES) mustBe 5
      lastSubmittedIdx.getKeys mustEqual Indexes.ascending("lastSubmitted")
    }
    "setup the ern index" in {
      val ern = repository.indexes.find(_.getOptions.getName == "ern_index").value
      ern.getOptions.isUnique mustBe true
      ern.getKeys mustEqual Indexes.ascending("ern")
    }
  }

  "save" should {
    "save when there isn't one there already" in {
      val fixedInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(fixedInstant)

      repository.save("testErn").futureValue

      find(Filters.eq("ern", "testErn")).futureValue.head mustBe ErnSubmission("testErn", fixedInstant)
    }

    "update the lastSubmitted when there is one there already" in {
      val originalInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS).minusSeconds(60)
      val updatedInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(updatedInstant)

      insert(ErnSubmission("testErn", originalInstant)).futureValue

      repository.save("testErn").futureValue

      find(Filters.eq("ern", "testErn")).futureValue.head mustBe ErnSubmission("testErn", updatedInstant)
    }

    mustPreserveMdc(repository.save("testErn"))
  }

  "getErnsAndLastSubmitted" should {

    "return an empty map if there are no ern submissions" in {
      repository.getErnsAndLastSubmitted.futureValue mustEqual Map.empty
    }

    "return a map of all erns and last submitted timestamps" in {
      val firstInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      val secondInstant = firstInstant.minus(1, ChronoUnit.SECONDS)

      insert(ErnSubmission("testErn1", firstInstant)).futureValue
      insert(ErnSubmission("testErn2", secondInstant)).futureValue

      val expectedMap = Map("testErn1" -> firstInstant, "testErn2" -> secondInstant)

      val ernsAndLastSubmitted = repository.getErnsAndLastSubmitted.futureValue

      ernsAndLastSubmitted mustBe expectedMap
    }

    mustPreserveMdc(repository.getErnsAndLastSubmitted)
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
