package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.mockito.MockitoSugar.when
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumberWorkItem, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import scala.concurrent.ExecutionContext
import java.time.Duration

class ExciseNumberQueueWorkItemRepositoryItSpec extends PlaySpec
  with CleanMongoCollectionSupport
  with PlayMongoRepositorySupport[WorkItem[ExciseNumberWorkItem]]
  with IntegrationPatience
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with OptionValues
  with GuiceOneAppPerSuite{

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val appConfig = mock[AppConfig]
  private val dateTimeService = mock[DateTimeService]
  private val timestamp = Instant.parse("2018-11-30T18:35:24.00Z")

  override protected val repository = new ExciseNumberQueueWorkItemRepository(
    appConfig,
    mongoComponent,
    dateTimeService

  )

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )

  override implicit lazy val app: Application = appBuilder.build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(dateTimeService.instant).thenReturn(timestamp)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    dropDatabase()
  }

  "inProgressRetryAfter" should {
    "be configurable" in {
      when(appConfig.retryAfterMinutes).thenReturn(Duration.ofMinutes(3))

      repository.inProgressRetryAfter mustBe Duration.ofMinutes(3)
    }
  }
}
