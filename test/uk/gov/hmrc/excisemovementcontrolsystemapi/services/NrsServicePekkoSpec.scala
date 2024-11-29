/*
 * Copyright 2024 HM Revenue & Customs
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

///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.excisemovementcontrolsystemapi.services
//
//import org.apache.pekko.actor.ActorSystem
//import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
//import org.apache.pekko.testkit.{ImplicitSender, TestActors, TestKit}
//import org.apache.pekko.{Done, actor}
//import org.bson.types.ObjectId
//import org.mockito.ArgumentMatchers.any
//import org.mockito.MockitoSugar.{reset, times, verify, when}
//import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpecLike
//import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, EitherValues}
//import org.scalatestplus.mockito.MockitoSugar.mock
//import org.scalatestplus.play.PlaySpec
//import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, SEE_OTHER}
//import play.api.test.Helpers.{await, defaultAwaitTimeout}
//import play.api.test.{FakeHeaders, FakeRequest}
//import uk.gov.hmrc.auth.core.AuthConnector
//import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnectorNew
//import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnectorNew.UnexpectedResponseException
//import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.NrsTestData
//import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{EnrolmentRequest, ParsedXmlRequest}
//import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE815Message, IEMessage}
//import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs._
//import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.NRSWorkItemRepository
//import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.NrsSubmissionWorkItem
//import uk.gov.hmrc.excisemovementcontrolsystemapi.services.NrsService.NonRepudiationIdentityRetrievals
//import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils, NrsEventIdMapper}
//import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
//import uk.gov.hmrc.mongo.TimestampSupport
//import uk.gov.hmrc.mongo.lock.MongoLockRepository
//import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Failed, PermanentlyFailed, Succeeded, ToDo}
//import uk.gov.hmrc.mongo.workitem.WorkItem
//
//import java.nio.charset.StandardCharsets
//import java.time.Instant
//import java.util.Base64
//import scala.concurrent.{ExecutionContext, Future}
//
//class MySpec()
//  extends TestKit(ActorSystem("MySpec"))
//    with ImplicitSender
//    with AnyWordSpecLike
//    with Matchers
//    with BeforeAndAfterAll
//    with NrsTestData {
//
//  override def afterAll(): Unit = {
//    TestKit.shutdownActorSystem(system)
//  }
//
//  "An Echo actor" must {
//
//    "send back messages unchanged" in {
//      val echo = system.actorOf(TestActors.echoActorProps)
//      echo ! "hello world"
//      expectMsg("hello world")
//    }
//
//  }
//
//  "submitNrsThrottled" should {
//    implicit val ec: ExecutionContext                   = ExecutionContext.global
//    implicit val hc: HeaderCarrier                      = HeaderCarrier(authorization = Some(Authorization(testAuthToken)))
//
//    val timeStamp = Instant.now()
//    val mockNrsConnectorNew                     = mock[NrsConnectorNew]
//    val mockNrsWorkItemRepository               = mock[NRSWorkItemRepository]
//    val mockCorrelationIdService                = mock[CorrelationIdService]
//    val mockDateTimeService                     = mock[DateTimeService]
//    val mockAuthConnector: AuthConnector        = mock[AuthConnector]
//    val mockLockRepository: MongoLockRepository = mock[MongoLockRepository]
//    val mockTimeStampSupport: TimestampSupport  = mock[TimestampSupport]
//
//    lazy val testKit: ActorTestKit              = ActorTestKit()
//    lazy val testActorSystem: actor.ActorSystem = testKit.system.classicSystem
//
//    val service = new NrsServiceNew(
//      mockAuthConnector,
//      mockNrsConnectorNew,
//      mockNrsWorkItemRepository,
//      mockDateTimeService,
//      new EmcsUtils,
//      new NrsEventIdMapper,
//      mockCorrelationIdService,
//      mockLockRepository,
//      mockTimeStampSupport,
//      testActorSystem
//    )
//
//    val testNrsMetadata = NrsMetadata(
//      businessId = "emcs",
//      notableEvent = "excise-movement-control-system",
//      payloadContentType = "application/json",
//      payloadSha256Checksum = sha256Hash("payload for NRS"),
//      userSubmissionTimestamp = timeStamp.toString,
//      identityData = testNrsIdentityData,
//      userAuthToken = testAuthToken,
//      headerData = Map(),
//      searchKeys = Map("ern" -> "123")
//    )
//
//    val encodedMessage  = Base64.getEncoder.encodeToString("<IE815>test</IE815>".getBytes(StandardCharsets.UTF_8))
//    val testNrsPayload  = NrsPayload(encodedMessage, testNrsMetadata)
//    val testNrsWorkItem = NrsSubmissionWorkItem(testNrsPayload)
//    val testWorkItem    = WorkItem(new ObjectId(), timeStamp, timeStamp, timeStamp, ToDo, 0, testNrsWorkItem)
//
//    "take at least x time to complete" in {
//
//      when(mockNrsConnectorNew.sendToNrs(any(), any())(any())).thenReturn(Future.successful(Done))
//      when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))
//
////      testActorSystem.expe
////      expectNoMessage(d: Duration)
//
//      val timeBefore = System.currentTimeMillis
//      //      await(service.submitNrsThrottled(testWorkItem))
//      service.submitNrsThrottled(testWorkItem).futureValue
//      val timeAfter = System.currentTimeMillis
//
//      val timeTaken: Long = timeAfter - timeBefore
//
//      timeTaken.toInt must be >= 1000
//    }
//  }
//}
//
//class NrsServicePekkoSpec extends TestKit(ActorSystem("MySpec"))
//  with ScalaFutures
//  with NrsTestData
//  with EitherValues
//  with BeforeAndAfterEach
//  with IntegrationPatience {
//
//  implicit val ec: ExecutionContext                   = ExecutionContext.global
//  implicit val hc: HeaderCarrier                      = HeaderCarrier(authorization = Some(Authorization(testAuthToken)))
//  private val mockNrsConnectorNew                     = mock[NrsConnectorNew]
//  private val mockNrsWorkItemRepository               = mock[NRSWorkItemRepository]
//  private val mockCorrelationIdService                = mock[CorrelationIdService]
//  private val mockDateTimeService                     = mock[DateTimeService]
//  private val mockAuthConnector: AuthConnector        = mock[AuthConnector]
//  private val mockLockRepository: MongoLockRepository = mock[MongoLockRepository]
//  private val mockTimeStampSupport: TimestampSupport  = mock[TimestampSupport]
//  private val timeStamp                               = Instant.now()
//
//  lazy val testKit: ActorTestKit              = ActorTestKit()
//  lazy val testActorSystem: actor.ActorSystem = testKit.system.classicSystem
//
//  private val service = new NrsServiceNew(
//    mockAuthConnector,
//    mockNrsConnectorNew,
//    mockNrsWorkItemRepository,
//    mockDateTimeService,
//    new EmcsUtils,
//    new NrsEventIdMapper,
//    mockCorrelationIdService,
//    mockLockRepository,
//    mockTimeStampSupport,
//    testActorSystem
//  )
//
//  private val message           = mock[IE815Message]
//  private val testCorrelationId = "testCorrelationId"
//
//  override def beforeEach(): Unit = {
//    super.beforeEach()
//    reset(
//      mockAuthConnector,
//      mockNrsConnectorNew,
//      mockDateTimeService,
//      mockCorrelationIdService,
//      mockNrsWorkItemRepository,
//      mockLockRepository,
//      mockTimeStampSupport
//    )
//
//    when(mockDateTimeService.timestamp()).thenReturn(timeStamp)
//    when(mockCorrelationIdService.generateCorrelationId()).thenReturn(testCorrelationId)
//    when(mockAuthConnector.authorise[NonRepudiationIdentityRetrievals](any, any)(any, any)) thenReturn
//      Future.successful(testAuthRetrievals)
//    when(mockNrsConnectorNew.sendToNrs(any, any)(any))
//      .thenReturn(Future.successful(Done))
//    when(message.consignorId).thenReturn("ern")
//  }
//
//  private val testRequest     = createRequest(message)
//  private val testNrsMetadata = NrsMetadata(
//    businessId = "emcs",
//    notableEvent = "excise-movement-control-system",
//    payloadContentType = "application/json",
//    payloadSha256Checksum = sha256Hash("payload for NRS"),
//    userSubmissionTimestamp = timeStamp.toString,
//    identityData = testNrsIdentityData,
//    userAuthToken = testAuthToken,
//    headerData = Map(),
//    searchKeys = Map("ern" -> "123")
//  )
//
//  private val encodedMessage  = Base64.getEncoder.encodeToString("<IE815>test</IE815>".getBytes(StandardCharsets.UTF_8))
//  private val testNrsPayload  = NrsPayload(encodedMessage, testNrsMetadata)
//  private val testNrsWorkItem = NrsSubmissionWorkItem(testNrsPayload)
//  private val testWorkItem    = WorkItem(new ObjectId(), timeStamp, timeStamp, timeStamp, ToDo, 0, testNrsWorkItem)
//
//  "submitNrsThrottled" should {
//
//    val testNrsMetadata = NrsMetadata(
//      businessId = "emcs",
//      notableEvent = "excise-movement-control-system",
//      payloadContentType = "application/json",
//      payloadSha256Checksum = sha256Hash("payload for NRS"),
//      userSubmissionTimestamp = timeStamp.toString,
//      identityData = testNrsIdentityData,
//      userAuthToken = testAuthToken,
//      headerData = Map(),
//      searchKeys = Map("ern" -> "123")
//    )
//
//    val encodedMessage  = Base64.getEncoder.encodeToString("<IE815>test</IE815>".getBytes(StandardCharsets.UTF_8))
//    val testNrsPayload  = NrsPayload(encodedMessage, testNrsMetadata)
//    val testNrsWorkItem = NrsSubmissionWorkItem(testNrsPayload)
//    val testWorkItem    = WorkItem(new ObjectId(), timeStamp, timeStamp, timeStamp, ToDo, 0, testNrsWorkItem)
//
//    "take at least x time to complete" in {
//
//      when(mockNrsConnectorNew.sendToNrs(any(), any())(any())).thenReturn(Future.successful(Done))
//      when(mockNrsWorkItemRepository.complete(any, any())).thenReturn(Future(true))
//
//      testActorSystem.expe
//      expectNoMessage(d: Duration)
//
//      val timeBefore = System.currentTimeMillis
////      await(service.submitNrsThrottled(testWorkItem))
//      service.submitNrsThrottled(testWorkItem).futureValue
//      val timeAfter = System.currentTimeMillis
//
//      val timeTaken: Long = timeAfter - timeBefore
//
//      timeTaken.toInt must be >= 1000
//    }
//  }
//
//  private def createRequest(message: IEMessage): ParsedXmlRequest[_] = {
//    val fakeRequest = FakeRequest()
//      .withBody("<IE815>test</IE815>")
//      .withHeaders(
//        FakeHeaders(Seq("header" -> "test"))
//      )
//
//    val enrolmentRequest = EnrolmentRequest(fakeRequest, Set("ern"), "123")
//    ParsedXmlRequest(enrolmentRequest, message, Set("ern"), "123")
//  }
//}
