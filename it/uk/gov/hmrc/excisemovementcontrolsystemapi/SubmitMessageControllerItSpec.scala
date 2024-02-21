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
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.StringSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.{ApplicationBuilderSupport, SubmitMessageTestSupport, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISSubmissionResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{Consignee, Consignor}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.NonRepudiationSubmissionAccepted
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumberWorkItem, Movement}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq


class SubmitMessageControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with ApplicationBuilderSupport
  with TestXml
  with WireMockServerSpec
  with SubmitMessageTestSupport
  with StringSupport
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  private def url(movementId: String) = s"http://localhost:$port/movements/$movementId/messages"

  private val eisUrl = "/emcs/digital-submit-new-message/v1"
  private val consignorId = "GBWK240176600"
  private val consigneeId = "GBWK002281023"

  //This matches the data from the IE818 test message
  private val movement = Movement(Some("boxId"), "LRNQA20230909022221", consignorId, Some(consigneeId), Some("23GB00000000000378553"))

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
    applicationBuilder(configureServices).build()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector, movementRepository, dateTimeService, workItemRepository)

    when(movementRepository.getMovementById(eqTo(movement._id)))
      .thenReturn(Future.successful(Some(movement)))

    when(workItemRepository.pushNew(any, any, any)).thenReturn(Future.successful(workItem))
    when(workItemRepository.getWorkItemForErn(any)).thenReturn(Future.successful(None))
    when(dateTimeService.timestamp()).thenReturn(Instant.now)
    authorizeNrsWithIdentityData
    stubNrsResponse

  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "Submit IE810 Cancellation" should {

    "return 202 when submitted by consignor" in {
      val arcInMessage = "23GB00000000000377161"
      val movementForIE810 = movement.copy(administrativeReferenceCode = Some(arcInMessage))
      when(movementRepository.getMovementById(eqTo(movementForIE810._id)))
        .thenReturn(Future.successful(Some(movementForIE810)))

      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(movement._id, IE810)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

    "return 403 error when submitted by consignee" in {

      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(movement._id, IE810)

      result.status mustBe FORBIDDEN
    }

  }

  "Submit IE813 Change of Destination" should {

    "return 202 when submitted by consignor" in {

      val arcInMessage = "23GB00000000000378126"
      val movementForIE813 = movement.copy(administrativeReferenceCode = Some(arcInMessage))
      when(movementRepository.getMovementById(eqTo(movementForIE813._id)))
        .thenReturn(Future.successful(Some(movementForIE813)))

      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(movementForIE813._id, IE813)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

    "return 403 error when submitted by consignee" in {

      withAuthorizedTrader("consignee")
      stubEISSuccessfulRequest()

      val result = postRequest(movement._id, IE813)

      result.status mustBe FORBIDDEN
      result.body.isEmpty mustBe false
    }
  }

  "Submit IE818 Report of Receipt Movement" should {

    "return 403 when sent in by the consignor" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(movement._id, IE818)

      result.status mustBe FORBIDDEN
      result.body.isEmpty mustBe false
    }

    "return 202 when sent in by the consignee" in {
      val movementForIE818 = movement.copy(administrativeReferenceCode = Some("23GB00000000000378553"))
      when(movementRepository.getMovementById(eqTo(movementForIE818._id)))
        .thenReturn(Future.successful(Some(movementForIE818)))

      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(movementForIE818._id, IE818)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

  }

  "Submit IE819 Alert or Rejection" should {

    "return 403 when sent in by the consignor" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(movement._id, IE819)

      result.status mustBe FORBIDDEN
      result.body.isEmpty mustBe false
    }

    "return 202 when sent in by the consignee" in {
      val arcInMessage = "23GB00000000000378574"
      val movementForIE819 = movement.copy(administrativeReferenceCode = Some(arcInMessage))
      when(movementRepository.getMovementById(eqTo(movementForIE819._id)))
        .thenReturn(Future.successful(Some(movementForIE819)))

      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(movementForIE819._id, IE819)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

  }

  "Submit IE837 Report of Receipt Movement" should {

    val arcInMessage = "16GB00000000000192223"
    val movementForIE837 = movement.copy(administrativeReferenceCode = Some(arcInMessage))

    "return 202 when sent in by the consignor" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      when(movementRepository.getMovementById(eqTo(movementForIE837._id)))
        .thenReturn(Future.successful(Some(movementForIE837)))

      val result = postRequest(movementForIE837._id, IE837WithConsignor)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

    "return 202 when sent in by the consignee" in {
      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      when(movementRepository.getMovementById(eqTo(movementForIE837._id)))
        .thenReturn(Future.successful(Some(movementForIE837)))

      val result = postRequest(movementForIE837._id, IE837WithConsignee)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

    "return 400 when consignor says they are consignee" in {

      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(movement._id, ie837Template(Consignee, consignorId))

      result.status mustBe BAD_REQUEST

    }

    "return 400 when consignee says they are consignor" in {

      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(movement._id, ie837Template(Consignor, consigneeId))

      result.status mustBe BAD_REQUEST

    }

  }

  "Submit IE871 Explanation On Shortage" should {

    val arcInMessage = "23GB00000000000377768"
    val movementForIE871 = movement.copy(administrativeReferenceCode = Some(arcInMessage))

    "return 202 when sent in by the consignor" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      when(movementRepository.getMovementById(eqTo(movementForIE871._id)))
        .thenReturn(Future.successful(Some(movementForIE871)))

      val result = postRequest(movementForIE871._id, IE871WithConsignor)

      result.status mustBe ACCEPTED
      result.body.isEmpty mustBe true
    }

    "return 202 when sent in by the consignee" in {
      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      when(movementRepository.getMovementById(eqTo(movementForIE871._id)))
        .thenReturn(Future.successful(Some(movementForIE871)))

      val result = postRequest(movementForIE871._id, IE871WithConsignee)

      result.status mustBe FORBIDDEN
      result.body.isEmpty mustBe false
    }

    "return 400 when consignor says they are consignee" in {

      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()

      val result = postRequest(movement._id, ie871ForConsignor(Consignee))

      result.status mustBe BAD_REQUEST

    }

    "return 400 when consignee says they are consignor" in {
      withAuthorizedTrader(consigneeId)
      stubEISSuccessfulRequest()

      val result = postRequest(movement._id, ie871ForConsignee(Consignor))

      result.status mustBe BAD_REQUEST

    }

  }

  "return not found if EIS returns not found" in {
    withAuthorizedTrader("GBWK002281023")
    val eisErrorResponse = createEISErrorResponseBodyAsJson("NOT_FOUND")
    stubEISErrorResponse(NOT_FOUND, eisErrorResponse.toString())

    val result = postRequest(movement._id, IE818)

    result.status mustBe NOT_FOUND

    withClue("return the EIS error response") {
      result.json mustBe Json.toJson(eisErrorResponse)
    }
  }

  "return not found if database cannot find movement ID for ERN" in {
    withAuthorizedTrader(consigneeId)

    when(movementRepository.getMovementById(eqTo(movement._id)))
      .thenReturn(Future.successful(None))

    val result: WSResponse = postRequest(movement._id, IE818)

    result.status mustBe NOT_FOUND
  }

  "return bad request if EIS returns BAD_REQUEST" in {
    withAuthorizedTrader(consigneeId)
    stubEISErrorResponse(BAD_REQUEST, createEISErrorResponseBodyAsJson("BAD_REQUEST").toString())

    postRequest(movement._id, IE818).status mustBe BAD_REQUEST
  }

  "remove control document references in any paths for a BAD_REQUEST" in {
    withAuthorizedTrader(consigneeId)
    stubEISErrorResponse(BAD_REQUEST, rimValidationErrorResponse(messageWithControlDoc))

    val response = postRequest(movement._id, IE818)

    clean(response.body) mustBe clean(rimValidationErrorResponse(messageWithoutControlDoc))

  }

  "return 500 if EIS return 500" in {
    withAuthorizedTrader(consigneeId)
    stubEISErrorResponse(INTERNAL_SERVER_ERROR, createEISErrorResponseBodyAsJson("INTERNAL_SERVER_ERROR").toString())

    postRequest(movement._id, IE818).status mustBe INTERNAL_SERVER_ERROR
  }

  "return 500 if EIS return bad json" in {
    withAuthorizedTrader(consigneeId)
    stubEISErrorResponse(INTERNAL_SERVER_ERROR, """"{"json": "is-bad"}""")

    postRequest(movement._id, IE818).status mustBe INTERNAL_SERVER_ERROR
  }

  "return forbidden (403) when there are no authorized ERN" in {
    withAnEmptyERN()

    postRequest(movement._id, IE818).status mustBe FORBIDDEN
  }

  "return a Unauthorized (401) when no authorized trader" in {
    withUnauthorizedTrader(InternalError("A general auth failure"))

    postRequest(movement._id, IE818).status mustBe UNAUTHORIZED
  }

  "return bad request (400) when xml cannot be parsed" in {
    withAuthorizedTrader("GBWK002281023")

    postRequest(movement._id, xml = <IE818></IE818>).status mustBe BAD_REQUEST
  }

  "return Unsupported Media Type (415)" in {
    withAuthorizedTrader("GBWK002281023")
    postRequest(movement._id, contentType = """application/json""").status mustBe UNSUPPORTED_MEDIA_TYPE
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

    postRequest(movement._id, IE818).status mustBe FORBIDDEN
  }

  "submit to NRS" in {
    withAuthorizedTrader(consigneeId)
    stubEISSuccessfulRequest()

    postRequest(movement._id, IE818)

    verify(postRequestedFor(urlEqualTo("/submission")))
  }

  private def createEISErrorResponseBodyAsJson(message: String): JsValue = {
    Json.toJson(EISErrorResponse(
      Instant.parse("2023-12-05T12:05:06Z"),
      message.toUpperCase,
      message.toLowerCase,
      s"debug $message",
      "123"
    ))
  }

  private def postRequest(
                           movementId: String,
                           xml: NodeSeq = IE818,
                           contentType: String = """application/vnd.hmrc.1.0+xml"""
                         ) = {
    await(wsClient.url(url(movementId))
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
