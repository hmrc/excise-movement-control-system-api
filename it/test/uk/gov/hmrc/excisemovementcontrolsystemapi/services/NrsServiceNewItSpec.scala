package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.pekko.Done
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, EitherValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.ACCEPTED
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.{NrsMetadata, NrsPayload}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NRSWorkItemRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.http.test.WireMockSupport
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
  with NrsTestData
  with WireMockSupport {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("testAuthToken")))

  private val dateTimeService = mock[DateTimeService]

  when(dateTimeService.timestamp()).thenReturn(Instant.now())

  private val timestamp = dateTimeService.timestamp()
  private val nrsMetadata = NrsMetadata(
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
  private val nrsPayLoad = NrsPayload("encodepayload", nrsMetadata)
  private val workItem1 = WorkItem(id = new ObjectId(),
    receivedAt = timestamp.minusSeconds(60),
    updatedAt = timestamp.minusSeconds(60),
    availableAt = timestamp.minusSeconds(60),
    status = ToDo,
    failureCount = 0,
    NrsSubmissionWorkItem(nrsPayLoad))
  private val workItem2 = WorkItem(id = new ObjectId(),
    receivedAt = timestamp.minusSeconds(60),
    updatedAt = timestamp.minusSeconds(60),
    availableAt = timestamp.minusSeconds(60),
    status = ToDo,
    failureCount = 0,
    NrsSubmissionWorkItem(nrsPayLoad))

  private val mockMongoLockRepository = mock[MongoLockRepository]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(dateTimeService),
      bind[MongoLockRepository].toInstance(mockMongoLockRepository)
    )
    .configure(
      "microservice.services.nrs.port" -> wireMockPort,
      "microservice.services.nrs.api-key" -> "some-bearer",
      "microservice.services.nrs.max-failures" -> 1,
      "microservice.services.nrs.reset-timeout" -> "1 second",
      "microservice.services.nrs.call-timeout" -> "30 seconds",
      "microservice.services.nrs.max-reset-timeout" -> "30 seconds",
      "microservice.services.nrs.exponential-backoff-factor" -> 2.0
    )
    .build()

  override protected val repository: NRSWorkItemRepository = app.injector.instanceOf[NRSWorkItemRepository]//  private val dateTimeService = mock[DateTimeService]

  private val service = app.injector.instanceOf[NrsServiceNew]

  "processAllWithLock" should {

    val url = "/submission"
    wireMockServer.stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(ACCEPTED)
        )
    )

    "when a lock is available" should {
      "call NRS once if there is one submission to process" in {
        when(mockMongoLockRepository.takeLock(any, any, any))
          .thenReturn(Future.successful(None))
        when(mockMongoLockRepository.releaseLock(any, any))
          .thenReturn(Future.successful(()))
        when(mockMongoLockRepository.refreshExpiry(any,any,any))
          .thenReturn(Future.successful(false)) // should definitely be false

        insert(workItem1).futureValue

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(1, postRequestedFor(urlMatching(url)))

      }
      "call NRS multiple times if there are more than one submission to process" in {
        when(mockMongoLockRepository.takeLock(any, any, any))
          .thenReturn(Future.successful(None))
        when(mockMongoLockRepository.releaseLock(any, any))
          .thenReturn(Future.successful(()))
        when(mockMongoLockRepository.refreshExpiry(any,any,any))
          .thenReturn(Future.successful(false))

        insert(workItem1).futureValue
        insert(workItem2).futureValue

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(2, postRequestedFor(urlMatching(url)))
      }
      "not call NRS if there is nothing to process" in {
        when(mockMongoLockRepository.takeLock(any, any, any))
          .thenReturn(Future.successful(None))
        when(mockMongoLockRepository.releaseLock(any, any))
          .thenReturn(Future.successful(()))
        when(mockMongoLockRepository.refreshExpiry(any,any,any))
          .thenReturn(Future.successful(false))

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(0, postRequestedFor(urlMatching(url)))
      }
    }

    "when a lock is not available" should {
      "not do anything, and not call NRS even if there is an item to process" in {

        when(mockMongoLockRepository.takeLock(any, any, any))
          .thenReturn(Future.successful(Some(Lock("id", "owner", Instant.now(), Instant.now().plus(5, ChronoUnit.MINUTES)))))
        when(mockMongoLockRepository.releaseLock(any, any))
          .thenReturn(Future.successful(()))
        when(mockMongoLockRepository.refreshExpiry(any,any,any))
          .thenReturn(Future.successful(false))

        insert(workItem1).futureValue

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(0, postRequestedFor(urlMatching(url)))
      }
    }

  }

}
