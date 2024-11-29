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

import org.mongodb.scala.model.Indexes
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.util.concurrent.TimeUnit

class NRSWorkItemRepositoryItSpec
  extends PlaySpec
    with DefaultPlayMongoRepositorySupport[WorkItem[NrsSubmissionWorkItem]]
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite {

  def servicesConfig: Map[String, String] = Map("mongodb.nrsSubmission.TTL" -> "5 seconds")
  private lazy val dateTimeService = mock[DateTimeService]

  override def fakeApplication: Application =
    new GuiceApplicationBuilder()
      .configure(servicesConfig)
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent),
        bind[DateTimeService].toInstance(dateTimeService)
      )

      .build()

  override protected val repository: NRSWorkItemRepository = app.injector.instanceOf[NRSWorkItemRepository]

  "initialise" should {

    "ensure indexes are created" in {
      await(repository.collection.listIndexes().toFuture()).size mustBe 5
    }

    "have a TTL index on the updatedAt field, with an expiry time set by AppConfig" in {

      val test = repository.indexes.find(_.getOptions.getName == "updatedAt_ttl_idx").value
      test.getKeys mustEqual Indexes.ascending("updatedAt")
      test.getOptions.getExpireAfter(TimeUnit.SECONDS) mustBe 5
    }
  }
}
