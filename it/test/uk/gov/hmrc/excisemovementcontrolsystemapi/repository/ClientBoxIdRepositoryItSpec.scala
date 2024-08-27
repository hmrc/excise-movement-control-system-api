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

import com.mongodb.MongoWriteException
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ClientBoxId
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}


class ClientBoxIdRepositoryItSpec extends PlaySpec
  with DefaultPlayMongoRepositorySupport[ClientBoxId]
  with IntegrationPatience
  with GuiceOneAppPerSuite {

  private val mockTimeService = mock[DateTimeService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(mockTimeService)
    )
    .configure(
      "mongodb.clientBoxId.TTL" -> "5 minutes"
    ).build()

  override protected lazy val repository: ClientBoxIdRepository = app.injector.instanceOf[ClientBoxIdRepository]

  "initialise" should {
    "setup the createdOn index" in {
      val createdOnIdx = repository.indexes.find(_.getOptions.getName == "createdOn_ttl_idx").value
      createdOnIdx.getOptions.getExpireAfter(TimeUnit.MINUTES) mustBe 5
      createdOnIdx.getKeys mustEqual Indexes.ascending("createdOn")
    }
    "setup the clientId index" in {
      val ern = repository.indexes.find(_.getOptions.getName == "clientId_idx").value
      ern.getOptions.isUnique mustBe true
      ern.getKeys mustEqual Indexes.ascending("clientId")
    }
  }

  "save" should {
    "save when there isn't one there already" in {
      val fixedInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(fixedInstant)

      repository.save("testClient", "testBox").futureValue

      find(Filters.eq("clientId", "testClient")).futureValue.head mustBe ClientBoxId("testClient", "testBox", fixedInstant)
    }

    "throw a MongoWriteException if there's already a record in the database" in {
      val fixedInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(fixedInstant)

      insert(ClientBoxId("testClient", "testBox", fixedInstant)).futureValue

      val failure = repository.save("testClient", "testBox2").failed.futureValue

      failure mustBe a [MongoWriteException]
    }

    mustPreserveMdc(repository.save("testClient", "testBox"))
  }

  "getBoxId" should {

    "return None if there is no boxId for the client" in {
      repository.getBoxId("testClient").futureValue mustBe None
    }

    "return the boxId for the client if there is one" in {
      val fixedInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
      when(mockTimeService.timestamp()).thenReturn(fixedInstant)

      insert(ClientBoxId("testClient", "testBox", fixedInstant)).futureValue

      val boxId = repository.getBoxId("testClient").futureValue
      boxId mustBe Some("testBox")
    }

    mustPreserveMdc(repository.getBoxId("testClient"))
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
