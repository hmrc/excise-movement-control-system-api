/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.pekko.Done
import org.mockito.MockitoSugar
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
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
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.Instant
import scala.concurrent.ExecutionContext

class NrsServiceItSpec extends PlaySpec
  with CleanMongoCollectionSupport
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

  private val lockId = "nrs-lock"

  private val now = Instant.now()
  when(dateTimeService.timestamp()).thenReturn(now)

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
  private val nrsPayLoad1 = NrsPayload("encodepayload1", nrsMetadata)
  private val nrsPayLoad2 = NrsPayload("encodepayload2", nrsMetadata)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(dateTimeService)
    )
    .configure(
      "microservice.services.nrs.port" -> wireMockPort,
      "microservice.services.nrs.api-key" -> "some-bearer",
      "microservice.services.nrs.max-failures" -> 1,
      "microservice.services.nrs.reset-timeout" -> "1 second",
      "microservice.services.nrs.call-timeout" -> "30 seconds",
      "microservice.services.nrs.max-reset-timeout" -> "30 seconds",
      "microservice.services.nrs.exponential-backoff-factor" -> 2.0,
      "microservice.services.nrs.lock-service-ttl" -> "10 minutes",
      "microservice.services.nrs.nrs-throttle-duration" -> "0.01 second"
    )
    .build()


  val repository: NRSWorkItemRepository = app.injector.instanceOf[NRSWorkItemRepository]

  val lockRepository: MongoLockRepository = app.injector.instanceOf[MongoLockRepository]

  private val service = app.injector.instanceOf[NrsService]

  val url = "/submission"

  wireMockServer.stubFor(
    post(urlEqualTo(url))
      .willReturn(
        aResponse()
          .withStatus(ACCEPTED)
      )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetAll()
    repository.initialised.futureValue
    lockRepository.initialised.futureValue
  }

  "processAllWithLock" should {
    "when a lock is available" should {
      "call NRS once if there is one submission to process" in {

        repository.pushNew(NrsSubmissionWorkItem(nrsPayLoad1), availableAt = now.minusSeconds(20)).futureValue

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(1, postRequestedFor(urlMatching(url)))

      }

      "call NRS multiple times if there are more than one submission to process" in {

        repository.pushNew(NrsSubmissionWorkItem(nrsPayLoad1), availableAt = now.minusSeconds(20)).futureValue
        repository.pushNew(NrsSubmissionWorkItem(nrsPayLoad2), availableAt = now.minusSeconds(20)).futureValue

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(2, postRequestedFor(urlMatching(url)))
      }
      "not call NRS if there is nothing to process" in {

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(0, postRequestedFor(urlMatching(url)))
      }
    }

    "when the lock is already taken" should {
      "not do anything, and not call NRS even if there is an item to process" in {

        // Force the lock to be taken prior to running the test
        lockRepository.takeLock(lockId, "owner", 60.seconds)

        repository.pushNew(NrsSubmissionWorkItem(nrsPayLoad1), availableAt = now.minusSeconds(20)).futureValue

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(0, postRequestedFor(urlMatching(url)))
      }
      "not do anything, and not call NRS even if there are many items to process" in {

        // Force the lock to be taken prior to running the test
        lockRepository.takeLock(lockId, "owner", 60.seconds)

        repository.pushNew(NrsSubmissionWorkItem(nrsPayLoad1), availableAt = now.minusSeconds(20)).futureValue
        repository.pushNew(NrsSubmissionWorkItem(nrsPayLoad2), availableAt = now.minusSeconds(20)).futureValue

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(0, postRequestedFor(urlMatching(url)))
      }
      "not do anything, and not call NRS if there are zero items to process" in {

        // Force the lock to be taken prior to running the test
        lockRepository.takeLock(lockId, "owner", 60.seconds)

        service.processAllWithLock().futureValue mustBe Done

        wireMockServer.verify(0, postRequestedFor(urlMatching(url)))
      }
    }
  }
}
