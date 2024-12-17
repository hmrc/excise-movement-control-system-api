package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import scala.concurrent.Future

class UnusedRepositoryRemoverItSpec extends PlaySpec with CleanMongoCollectionSupport {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  private lazy val remover: UnusedRepositoryRemover  = new UnusedRepositoryRemover(mongoComponent)

  "removeMiscodedMovements" should {
    "remove miscoded movements archive from database" in {

      val collectionName = "miscoded-movements-archive"

      val result = for {
        _   <- mongoComponent.database.createCollection(collectionName).toFuture()
        _   <- remover.removeMiscodedMovements()
        res <- existsCollection(mongoComponent, collectionName)
      } yield res

      val response = await(result)
      response mustBe false
    }

    "remove miscoded movements workItems from database" in {

      val collectionName = "miscoded-movements-workItems"

      val result = for {
        _   <- mongoComponent.database.createCollection(collectionName).toFuture()
        _   <- remover.removeMiscodedMovements()
        res <- existsCollection(mongoComponent, collectionName)
      } yield res

      val response = await(result)
      response mustBe false
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
