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

import akka.stream.scaladsl.Sink
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.Play.materializer
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumber
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext

class ExciseNumberRepositorySpec extends PlaySpec
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with PlayMongoRepositorySupport[ExciseNumber]
  with CleanMongoCollectionSupport
  with IntegrationPatience
  with GuiceOneAppPerSuite {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val instant = Instant.now
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
  private val appConfig = app.injector.instanceOf[AppConfig]

  protected override val repository = new ExciseNumberRepository(
    mongoComponent,
    appConfig,
    stubClock
  )

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    prepareDatabase()
  }

  "getAll" should {
    "retrieve the all the ern" in {

      val lastUpdate =  instant.truncatedTo(ChronoUnit.MILLIS)
      insert(ExciseNumber("123", "345",  lastUpdate)).futureValue
      insert(ExciseNumber("12", "789",  lastUpdate)).futureValue

      val result: Seq[ExciseNumber] = await(repository.getAll.runWith(Sink.seq))

      result.size mustBe 2
      result mustBe Seq(
        ExciseNumber("123", "345", lastUpdate),
        ExciseNumber("12", "789", lastUpdate)
      )
    }
  }

  "save" should {
    "save an Excise number" in {

      val exciseNumber = ExciseNumber("123", "345")

      val result = repository.save(exciseNumber).futureValue
      val insertedRecord = find(
        Filters.and(
          Filters.equal("exciseNumber", "123"),
          Filters.equal("localReferenceNumber", "345")
        )
      ).futureValue
        .headOption
        .value

      result mustEqual true
      insertedRecord.exciseNumber mustBe "123"
      insertedRecord.localReferenceNumber mustEqual "345"
    }
  }
}
