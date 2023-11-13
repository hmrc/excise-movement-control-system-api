package uk.gov.hmrc.excisemovementcontrolsystemapi.fixture

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementMessageRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.DateTimeService
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

trait MovementRepositoryFixture
  extends ScalaFutures
    with PlayMongoRepositorySupport[Movement]
    with IntegrationPatience {


  private val timeService = mock[DateTimeService]
  private val timestamp = Instant.parse("2018-11-30T18:35:24.00Z")
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val appConfig = mock[AppConfig]
  val repo = new MovementMessageRepository(
    mongoComponent,
    appConfig,
    timeService
  )

  def findAll: Future[Seq[Movement]] =
    repository.collection.find().toFuture()
}
