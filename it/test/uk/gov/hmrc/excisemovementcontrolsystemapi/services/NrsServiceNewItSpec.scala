package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import org.apache.pekko.Done
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, EitherValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{NrsConnector, NrsConnectorNew}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{NrsMetadata, NrsPayload}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NRSWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class NrsServiceNewItSpec extends PlaySpec
  with CleanMongoCollectionSupport
  with DefaultPlayMongoRepositorySupport[WorkItem[NrsSubmissionWorkItem]]
  with EitherValues
  with IntegrationPatience
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with MockitoSugar
  with NrsTestData {

  override protected val repository: NRSWorkItemRepository = app.injector.instanceOf[NRSWorkItemRepository]

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("testAuthToken")))

//  private val dateTimeService = mock[DateTimeService]
//  private val timestampSupport: TimestampSupport = app.injector.instanceOf[TimestampSupport]
//
//  private val fakeMongoLockRepositoryWithLockFail: MongoLockRepository = new MongoLockRepository(mongoComponent, timestampSupport) {
//    override def takeLock(lockId: String, owner: String, ttl: Duration): Future[Option[Lock]] =
//      Future.successful(Some(Lock("id", "owner", Instant.now(), Instant.now().plus(5, ChronoUnit.MINUTES))))
//
//    override def releaseLock(lockId: String, owner: String): Future[Unit] =
//      Future.successful(())
//  }

//  private val fakeMongoLockRepositoryWithLockSuccess: MongoLockRepository = new MongoLockRepository(mongoComponent, timestampSupport) {
//    override def takeLock(lockId: String, owner: String, ttl: Duration): Future[Option[Lock]] =
//      Future.successful(Some(Lock("id", "owner", Instant.now(), Instant.now().plus(5, ChronoUnit.MINUTES))))
//
//    override def releaseLock(lockId: String, owner: String): Future[Unit] =
//      Future.successful(())
//  }


//  override def fakeApplication(): Application = GuiceApplicationBuilder()
//    .overrides(
//      bind[MongoComponent].toInstance(mongoComponent),
//      bind[DateTimeService].toInstance(dateTimeService),
//      bind[MongoLockRepository].toInstance(fakeMongoLockRepositoryWithLockFail)
//    )
//    .build()

  "processAllWithLock" should {
    "when a lock is available" should {
      val dateTimeService = mock[DateTimeService]
      when(dateTimeService.timestamp()).thenReturn(Instant.now())
//      val timestampSupport: TimestampSupport = app.injector.instanceOf[TimestampSupport]
      val timestamp = dateTimeService.timestamp()

      "call NRS and mark a workItem as complete if there is a single item to be processed" in {
        val nrsMetadata = NrsMetadata(
          businessId = "emcs",
          notableEvent = "excise-movement-control-system",
          payloadContentType = "application/json",
          payloadSha256Checksum = sha256Hash("payload for NRS"),
          userSubmissionTimestamp = timestamp.toString,
          identityData = testNrsIdentityData,
          userAuthToken = testAuthToken,
          headerData = Map(),
          searchKeys = Map("ern" -> "123")
        )
        val nrsPayLoad = NrsPayload("encodepayload", nrsMetadata)
        val workItem = WorkItem(id = new ObjectId(),
          receivedAt = timestamp.minusSeconds(60),
          updatedAt = timestamp.minusSeconds(60),
          availableAt = timestamp.minusSeconds(60),
          status = ToDo,
          failureCount = 0,
          NrsSubmissionWorkItem(nrsPayLoad))

        insert(workItem).futureValue

//        val fakeMongoLockRepositoryWithLockFail: MongoLockRepository = new MongoLockRepository(mongoComponent, timestampSupport) {
//          override def takeLock(lockId: String, owner: String, ttl: Duration): Future[Option[Lock]] =
//            Future.successful(Some(Lock("id", "owner", Instant.now(), Instant.now().plus(5, ChronoUnit.MINUTES))))
//
//          override def releaseLock(lockId: String, owner: String): Future[Unit] =
//            Future.successful(())
//        }

        val mockLockRepository = mock[MongoLockRepository]
        val mockNrsConnector = mock[NrsConnectorNew]

        val testApp = GuiceApplicationBuilder()
          .overrides(
            bind[MongoComponent].toInstance(mongoComponent),
            bind[DateTimeService].toInstance(dateTimeService),
            bind[MongoLockRepository].toInstance(mockLockRepository),
            bind[NrsConnectorNew].toInstance(mockNrsConnector)
          )
          .build()

        when(mockLockRepository.takeLock(any,any, any))
          .thenReturn(Future.successful(Some(Lock("id", "owner", Instant.now(), Instant.now().plus(5, ChronoUnit.MINUTES)))))
        when(mockLockRepository.releaseLock(any, any)).thenReturn(Future.successful(()))
        when(mockLockRepository.refreshExpiry(any, any, any)).thenReturn(Future.successful(false))
        when(mockLockRepository.disownLock(any, any, any)).thenReturn(Future.successful(()))
        when(mockNrsConnector.sendToNrs(any,any)(any)).thenReturn(Future.successful(Done))

        val service = testApp.injector.instanceOf[NrsServiceNew]

        val result = service.processAllWithLock()
//          service.processAllWithLock().futureValue
        await(result)

          verify(mockLockRepository, times(1)).refreshExpiry(any, any, any)
          verify(mockLockRepository, times(1)).takeLock(any, any, any)


        await(repository.pullOutstanding(timestamp, timestamp)) mustBe None
//        verify(repository, times(4)).pullOutstanding(any, any)
//        verify(repository, times(3)).complete(workItem.id, Succeeded)
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
