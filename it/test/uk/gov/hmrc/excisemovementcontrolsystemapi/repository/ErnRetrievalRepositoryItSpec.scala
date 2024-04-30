package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.mockito.MockitoSugar.when
import org.mongodb.scala.model.Filters
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ErnRetrieval
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import play.api.inject.bind
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant


class ErnRetrievalRepositoryItSpec extends PlaySpec
  with DefaultPlayMongoRepositorySupport[ErnRetrieval]
  with GuiceOneAppPerSuite {

  val mockTimeService = mock[DateTimeService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(mockTimeService)
    ).build()

  override protected lazy val repository: ErnRetrievalRepository = app.injector.instanceOf[ErnRetrievalRepository]

  "save" should {
    "save when there isn't one there already" in {
      val fixedInstant = Instant.now()
      when(mockTimeService.timestamp()).thenReturn(fixedInstant)

      repository.save("testErn")

      find(Filters.eq("ern", "testErn")) mustBe ErnRetrieval("testErn", fixedInstant)

    }
  }
}
