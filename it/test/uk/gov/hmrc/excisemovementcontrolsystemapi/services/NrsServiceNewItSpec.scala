package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

class NrsServiceNewItSpec extends PlaySpec
  with CleanMongoCollectionSupport
  with EitherValues
  with IntegrationPatience
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with MockitoSugar {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val dateTimeService = mock[DateTimeService]
  private val timestampSupport: TimestampSupport = app.injector.instanceOf[TimestampSupport]

  private val fakeMongoLockRepositoryWithLockFail: MongoLockRepository = new MongoLockRepository(mongoComponent, timestampSupport) {
    override def takeLock(lockId: String, owner: String, ttl: Duration): Future[Option[Lock]] =
      Future.successful(None)

    override def releaseLock(lockId: String, owner: String): Future[Unit] =
      Future.successful(())
  }

  private val fakeMongoLockRepositoryWithLockSuccess: MongoLockRepository = new MongoLockRepository(mongoComponent, timestampSupport) {
    override def takeLock(lockId: String, owner: String, ttl: Duration): Future[Option[Lock]] =
      Future.successful(Some(Lock("id", "owner", Instant.now(), Instant.now().plus(5, ChronoUnit.MINUTES))))

    override def releaseLock(lockId: String, owner: String): Future[Unit] =
      Future.successful(())
  }

  private val service = app.injector.instanceOf[NrsServiceNew]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(dateTimeService),
      bind[MongoLockRepository].toInstance(fakeMongoLockRepositoryWithLockFail)
    )
    .build()

  "processAllWithLock" should {
    "when a lock is available" should {
//      val lock = Lock("id", "owner", timeStamp, timeStamp.plus(1, ChronoUnit.HOURS))

      "call NRS multiple times if there are more than one submission to process" in {

        service.processAllWithLock().futureValue

        verify(fakeMongoLockRepositoryWithLockFail, times(1)).refreshExpiry(any,any,any)
        verify(fakeMongoLockRepositoryWithLockFail, times(1)).takeLock(any,any,any)

//        verify(mockNrsWorkItemRepository, times(4)).pullOutstanding(any(), any())
//        verify(mockNrsWorkItemRepository, times(3)).complete(testWorkItem.id, Succeeded)
      }
//      "not call NRS if there is nothing to process" in {
//        when(mockLockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(Some(lock)))
//        when(mockLockRepository.releaseLock(any(), any())).thenReturn(Future.unit)
//
//        when(mockNrsWorkItemRepository.pullOutstanding(any(), any()))
//          .thenReturn(Future.successful(None))
//
//        service.processAllWithLock().futureValue
//
//        verify(mockNrsWorkItemRepository, times(1)).pullOutstanding(any(), any())
//        verify(mockNrsWorkItemRepository, never()).complete(any(), any())
//      }
    }
//    "when a lock is not available" should {
//      "not do anything" in {
//        when(mockLockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(None))
//        when(mockLockRepository.releaseLock(any(), any())).thenReturn(Future.unit)
//
//        when(mockNrsConnectorNew.sendToNrs(any(), any())(any())).thenReturn(Future.successful(Done))
//        when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))
//
//        when(mockNrsWorkItemRepository.pullOutstanding(any(), any())).thenReturn(
//          Future.successful(Some(testWorkItem)),
//          Future.successful(Some(testWorkItem)),
//          Future.successful(Some(testWorkItem)),
//          Future.successful(None)
//        )
//
//        service.processAllWithLock().futureValue
//
//        verify(mockNrsWorkItemRepository, never()).pullOutstanding(any(), any())
//      }
//    }
  }

}
