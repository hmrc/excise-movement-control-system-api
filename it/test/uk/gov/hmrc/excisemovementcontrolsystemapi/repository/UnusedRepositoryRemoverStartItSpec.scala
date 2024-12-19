package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.{MongoComponent, MongoUtils}
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
