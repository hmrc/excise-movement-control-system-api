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

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import scala.concurrent.Future

class UnusedRepositoryRemoverStartItSpec
    extends PlaySpec
    with CleanMongoCollectionSupport
    with GuiceOneAppPerTest
    with BeforeAndAfterEach {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent)
    )
    .build()

  override def beforeEach(): Unit = {
    val firstCollectionName  = "miscoded-movements-archive"
    val secondCollectionName = "miscoded-movements-workItems"

    val collections = for {
      firstCollectionExists  <- existsCollection(mongoComponent, firstCollectionName)
      _                      <- createIfCollectionDoesNotExist(firstCollectionName, firstCollectionExists)
      secondCollectionExists <- existsCollection(mongoComponent, secondCollectionName)
      _                      <- createIfCollectionDoesNotExist(secondCollectionName, secondCollectionExists)
    } yield ()

    await(collections)
  }

  private def createIfCollectionDoesNotExist(collectionName: String, collectionExists: Boolean) =
    if (!collectionExists) mongoComponent.database.createCollection(collectionName).toFuture()
    else Future.successful()

  "unusedRepositoryRemover" should {
    "drop repository miscoded movements archive on class instantiation" in {
      val collectionName = "miscoded-movements-archive"
      await(existsCollection(mongoComponent, collectionName)) mustBe false
    }

    "drop repository miscoded movements workItems on class instantiation" in {
      val collectionName = "miscoded-movements-workItems"
      await(existsCollection(mongoComponent, collectionName)) mustBe false
    }
  }
  def existsCollection(
    mongoComponent: MongoComponent,
    collectionName: String
  ): Future[Boolean] =
    for {
      collections <- mongoComponent.database.listCollectionNames().toFuture()
    } yield collections.contains(collectionName)
}
