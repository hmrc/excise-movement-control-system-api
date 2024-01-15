/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InternalError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{AuthTestSupport, StringSupport}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.{RepositoryTestStub, SubmitMessageTestSupport, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISSubmissionResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.NonRepudiationSubmissionAccepted
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumberWorkItem, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ExciseNumberQueueWorkItemRepository, MovementRepository}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.{Instant, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq


class SubmitMessageControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml
  with WireMockServerSpec
  with RepositoryTestStub
  with SubmitMessageTestSupport
  with StringSupport
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private def url(movementId: String) = s"http://localhost:$port/movements/$movementId/messages"

  private val eisUrl = "/emcs/digital-submit-new-message/v1"
  private val consignorId = "GBWK240176600"
  private val consigneeId = "GBWK002281023"
  private val movement = Movement("LRNQA20230909022221", consignorId, Some(consigneeId), Some("23GB00000000000377161"))

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val workItem: WorkItem[ExciseNumberWorkItem] = WorkItem(
    id = new ObjectId(),
    receivedAt = Instant.now,
    updatedAt = Instant.now,
    availableAt = Instant.now,
    status = ProcessingStatus.ToDo,
    failureCount = 0,
    item = ExciseNumberWorkItem("ern", 3)
  )

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())
    GuiceApplicationBuilder()
      .configure(configureWithNrsServer)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[MovementRepository].to(movementRepository),
        bind[ExciseNumberQueueWorkItemRepository].to(workItemRepository)
      )
      .build()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector, movementRepository)

    when(movementRepository.getMovementById(eqTo(movement._id)))
      .thenReturn(Future.successful(Some(movement)))

    when(workItemRepository.pushNew(any, any, any)).thenReturn(Future.successful(workItem))
    when(workItemRepository.getWorkItemForErn(any)).thenReturn(Future.successful(None))
    authorizeNrsWithIdentityData
    stubNrsResponse

  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "Submit IE810 Cancellation" should {

    "return 202 when submitted by consignor" in {
      when(movementRepository.getMovementByARC("23GB00000000000377161")).thenReturn(Future.successful(Seq(movement)))

      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE810)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

    "return 202 when submitted by consignee" in {
      when(movementRepository.getMovementByARC("23GB00000000000377161"))
        .thenReturn(Future.successful(Seq(movement)))

      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE810)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

  }

  "Submit IE813 Change of Destination" should {

    "return 202 when submitted by consignor" in {
      when(movementRepository.getMovementByARC("23GB00000000000378126"))
        .thenReturn(Future.successful(Seq(movement)))

      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE813)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

    "return 403 error when submitted by consignee" in {
      when(movementRepository.getMovementByARC("23GB00000000000378126"))
        .thenReturn(Future.successful(Seq(movement)))

      withAuthorizedTrader("consignee")
      stubEISSuccessfulRequest()

      val result = postRequest(IE813)

      result.status mustBe FORBIDDEN
      result.body.contains("Excise number in message does not match authenticated excise number") mustBe true
    }
  }

  "Submit IE818 Report of Receipt Movement" should {

    "return 403 when sent in by the consignor" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE818)

      result.status mustBe FORBIDDEN
      result.body.isEmpty mustBe false
    }

    "return 202 when sent in by the consignee" in {
      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE818)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

  }

  "Submit IE819 Alert or Rejection" should {

    "return 403 when sent in by the consignor" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE819)

      result.status mustBe FORBIDDEN
      result.body.isEmpty mustBe false
    }

    "return 202 when sent in by the consignee" in {
      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE819)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

  }

  "Submit IE837 Report of Receipt Movement" should {

    "return 202 when sent in by the consignor" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE837WithConsignor)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

    "return 202 when sent in by the consignee" in {
      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE837WithConsignee)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

  }

  "Submit IE871 Explanation On Shortage" should {

    "return 202 when sent in by the consignor" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE871)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

    "return 403 when sent in by the consignee" in {
      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(IE871)

      result.status mustBe FORBIDDEN
      result.body.isEmpty mustBe false
    }

  }

  "return not found if EIS returns not found" in {
    withAuthorizedTrader("GBWK002281023")
    val eisErrorResponse = createEISErrorResponseBodyAsJson("NOT_FOUND")
    stubEISErrorResponse(NOT_FOUND, eisErrorResponse.toString())

    val result = postRequest(IE818)

    result.status mustBe NOT_FOUND

    withClue("return the EIS error response") {
      result.json mustBe Json.toJson(eisErrorResponse)
    }
  }

  "return not found if database cannot find movement ID for ERN" in {
    withAuthorizedTrader(consigneeId)

    when(movementRepository.getMovementById(eqTo(movement._id)))
      .thenReturn(Future.successful(None))

    val result: WSResponse = postRequest(IE818)

    result.status mustBe NOT_FOUND
  }

  "return bad request if EIS returns BAD_REQUEST" in {
    withAuthorizedTrader(consigneeId)
    stubEISErrorResponse(BAD_REQUEST, createEISErrorResponseBodyAsJson("BAD_REQUEST").toString())

    postRequest(IE818).status mustBe BAD_REQUEST
  }

  "remove control document references in any paths for a BAD_REQUEST" in {
    withAuthorizedTrader(consigneeId)
    stubEISErrorResponse(BAD_REQUEST, rimValidationErrorResponse(messageWithControlDoc))

    val response = postRequest(IE818)

    clean(response.body) mustBe clean(rimValidationErrorResponse(messageWithoutControlDoc))

  }

  "return 500 if EIS return 500" in {
    withAuthorizedTrader(consigneeId)
    stubEISErrorResponse(INTERNAL_SERVER_ERROR, createEISErrorResponseBodyAsJson("INTERNAL_SERVER_ERROR").toString())

    postRequest(IE818).status mustBe INTERNAL_SERVER_ERROR
  }

  "return 500 if EIS return bad json" in {
    withAuthorizedTrader(consigneeId)
    stubEISErrorResponse(INTERNAL_SERVER_ERROR, """"{"json": "is-bad"}""")

    postRequest(IE818).status mustBe INTERNAL_SERVER_ERROR
  }

  "return forbidden (403) when there are no authorized ERN" in {
    withAnEmptyERN()

    postRequest(IE818).status mustBe FORBIDDEN
  }

  "return a Unauthorized (401) when no authorized trader" in {
    withUnauthorizedTrader(InternalError("A general auth failure"))

    postRequest(IE818).status mustBe UNAUTHORIZED
  }

  "return bad request (400) when xml cannot be parsed" in {
    withAuthorizedTrader("GBWK002281023")

    postRequest(<IE818></IE818>).status mustBe BAD_REQUEST
  }

  "return Unsupported Media Type (415)" in {
    withAuthorizedTrader("GBWK002281023")
    postRequest(contentType = """application/json""").status mustBe UNSUPPORTED_MEDIA_TYPE
  }

  "return bad request (400) when body is not xml" in {
    withAuthorizedTrader("GBWK002281023")

    //Can't use postRequest routine as test requires non-xml body
    val result = await(wsClient.url(url("lrn"))
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN",
        HeaderNames.CONTENT_TYPE -> """application/vnd.hmrc.1.0+xml"""
      ).post("test")
    )

    result.status mustBe BAD_REQUEST
  }

  "return forbidden (403) when consignor id cannot be validate" in {
    withAuthorizedTrader()

    postRequest(IE818).status mustBe FORBIDDEN
  }

  "submit to NRS" in {
    withAuthorizedTrader(consigneeId)
    stubEISSuccessfulRequest()

    postRequest(IE818)

    verify(postRequestedFor(urlEqualTo("/submission")))
  }

  private def createEISErrorResponseBodyAsJson(message: String): JsValue = {
    Json.toJson(EISErrorResponse(
      LocalDateTime.of(2023, 12, 5, 12, 5, 6),
      message,
      s"debug $message",
      "123"
    ))
  }

  private def postRequest(
                           xml: NodeSeq = IE818,
                           contentType: String = """application/vnd.hmrc.1.0+xml"""
                         ) = {
    await(wsClient.url(url(movement._id))
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN",
        HeaderNames.CONTENT_TYPE -> contentType
      ).post(xml)
    )
  }

  private def stubEISSuccessfulRequest() = {

    val response = EISSubmissionResponse("OK", "message", "123")
    wireMock.stubFor(
      post(eisUrl)
        .willReturn(ok().withBody(Json.toJson(response).toString()))
    )
  }

  private def stubEISErrorResponse(status: Int, body: String): Any = {

    wireMock.stubFor(
      post(urlEqualTo(eisUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
            .withHeader("Content-Type", "application/json")
        )
    )
  }

  private def stubNrsResponse = {
    wireMock.stubFor(
      post(urlEqualTo("/submission"))
        .willReturn(
          aResponse()
            .withStatus(ACCEPTED)
            .withBody(Json.toJson(NonRepudiationSubmissionAccepted("submissionId")).toString())
        )
    )
  }

}
