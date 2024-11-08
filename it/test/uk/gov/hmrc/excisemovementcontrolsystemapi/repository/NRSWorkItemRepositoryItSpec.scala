package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.mockito.MockitoSugar.when
import org.mongodb.scala.bson.{BsonInt32, BsonString}
import org.mongodb.scala.model.Indexes
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext

class NRSWorkItemRepositoryItSpec
  extends PlaySpec
    with CleanMongoCollectionSupport
    with DefaultPlayMongoRepositorySupport[WorkItem[NrsSubmissionWorkItem]]
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite {

  private lazy val dateTimeService = mock[DateTimeService]
  private lazy val timestamp = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  def servicesConfig: Map[String, String] = Map("mongodb.nrsSubmission.TTL" -> "5 seconds")

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(servicesConfig)
      .build()

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  override protected lazy val repository = new NRSWorkItemRepository(appConfig, mongoComponent)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }

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
