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
import org.mockito.MockitoSugar.{reset => MockitSugerReset}
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{when, reset => mockitoSugerReset}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Constants
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.NonRepudiationSubmissionAccepted
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, ExciseMovementResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}
import scala.concurrent.duration.MINUTES
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq


class DraftExciseMovementControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml
  with WireMockServerSpec
  with SubmitMessageTestSupport
  with RepositoryTestStub
  with StringSupport
  with Eventually
  with IntegrationPatience
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val url = s"http://localhost:$port/movements"
  private val eisUrl = "/emcs/digital-submit-new-message/v1"
  private val consignorId = "GBWK002281023"
  private val consigneeId = "GBWKQOZ8OVLYR"
  private val boxId = "testBoxId"
  private lazy val dateTimeService = mock[DateTimeService]
  private val timeStamp = Instant.now

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
      .configure(configureServices)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[MovementRepository].to(movementRepository),
        bind[DateTimeService].to(dateTimeService)
      )
      .build()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    mockitoSugerReset(dateTimeService)

    wireMock.resetAll()
    stubNrsResponse
    authorizeNrsWithIdentityData
    stubGetBoxIdSuccessRequest
    when(dateTimeService.timestamp()).thenReturn(timeStamp)
  }

  "Draft Excise Movement" should {

    "return 202" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest
      setupRepositories
      when(workItemRepository.pushNew(any, any, any)).thenReturn(Future.successful(workItem))
      when(workItemRepository.getWorkItemForErn(any)).thenReturn(Future.successful(None))

      val result = postRequest(IE815)

      result.status mustBe ACCEPTED
      withClue("return the json response") {
        assertValidResult(result)
      }

      withClue("submit to NRS") {
        verify(postRequestedFor(urlEqualTo("/submission")))
      }
    }

    "return an error" when {
      "notification service return a 400" in {
        withAuthorizedTrader(consignorId)
        stubEISSuccessfulRequest
        setupRepositories
        stubGetBoxIdFailureRequest(BAD_REQUEST, "Missing or incorrect query parameter")
        when(workItemRepository.pushNew(any, any, any)).thenReturn(Future.successful(workItem))
        when(workItemRepository.getWorkItemForErn(any)).thenReturn(Future.successful(None))

        val result = postRequest(IE815)

        result.status mustBe BAD_REQUEST
        withClue("return the json response") {
          val expectedJson = ErrorResponse(timeStamp, "Push Notification Error", "Missing or incorrect query parameter")
          val responseBody = Json.parse(result.body).as[ErrorResponse]
          responseBody mustBe expectedJson
        }

        withClue("should not submit to NRS") {
          verify(0, postRequestedFor(urlEqualTo("/submission")))
        }

        withClue("should not submit message") {
          verify(0, postRequestedFor(urlEqualTo(eisUrl)))
        }
      }
    }

    "clientId is missing in the header" in {
      val result = postRequestWithoutClientId

      result.status mustBe BAD_REQUEST
      withClue("return the json response") {
        val responseBody = result.json.as[ErrorResponse]
        responseBody.dateTime.truncatedTo(ChronoUnit.MINUTES) mustBe Instant.now.truncatedTo(ChronoUnit.MINUTES)
        responseBody.message mustBe "ClientId error"
        responseBody.debugMessage mustBe "Request header is missing clientId"
      }
    }

    "return success also if NRS fails" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest
      stubNrsErrorResponse
      setupRepositories

      val result = postRequest(IE815)

      result.status mustBe ACCEPTED
      withClue("return the json response") {
        assertValidResult(result)
      }
    }

    "return success also if NRS throws" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest
      stubNrsErrorResponse
      setupRepositories

      // no Authorization header added
      val result = await(wsClient.url(url).addHttpHeaders("X-Client-Id" -> "clientId").post(IE815))

      result.status mustBe ACCEPTED
    }

    "return not found if EIS returns not found" in {
      withAuthorizedTrader(consignorId)
      val eisErrorResponse = createEISErrorResponseBodyAsJson("NOT_FOUND")
      stubEISErrorResponse(NOT_FOUND, eisErrorResponse.toString())

      val result = postRequest(IE815)

      result.status mustBe NOT_FOUND

      withClue("return the EIS error response") {
        result.json mustBe Json.toJson(eisErrorResponse)
      }
    }

    "return bad request (400) if EIS returns BAD_REQUEST" in {
      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(BAD_REQUEST, createEISErrorResponseBodyAsJson("BAD_REQUEST").toString())

      postRequest(IE815).status mustBe BAD_REQUEST
    }

    "remove control document references in any paths for a BAD_REQUEST" in {

      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(BAD_REQUEST, rimValidationErrorResponse(messageWithControlDoc))

      val response = postRequest(IE815)
      clean(response.body) mustBe clean(rimValidationErrorResponse(messageWithoutControlDoc))
    }

    "return unprocessable entity (422) if EIS returns UNPROCESSABLE_ENTRY" in {
      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(UNPROCESSABLE_ENTITY, createEISErrorResponseBodyAsJson("Unprocessable_Entity").toString())

      postRequest(IE815).status mustBe UNPROCESSABLE_ENTITY
    }

    "return internal server error (500) if EIS returns 500" in {
      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(INTERNAL_SERVER_ERROR, createEISErrorResponseBodyAsJson("INTERNAL_SERVER_ERROR").toString())

      postRequest(IE815).status mustBe INTERNAL_SERVER_ERROR
    }

    "return internal server error (500) if EIS returns bad json" in {
      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(INTERNAL_SERVER_ERROR, """"{"json": "is-bad"}""")

      postRequest(IE815).status mustBe INTERNAL_SERVER_ERROR
    }

    "return forbidden (403) when there are no authorized ERN" in {
      withAnEmptyERN()

      postRequest(IE815).status mustBe FORBIDDEN
    }

    "return forbidden (403) when the consignee is trying to send in an IE815" in {
      withAuthorizedTrader(consigneeId)

      postRequest(IE815).status mustBe FORBIDDEN
    }

    "return forbidden (403) when the consignor is empty" in {
      withAuthorizedTrader(consignorId)

      postRequest(IE815WithNoCosignor).status mustBe FORBIDDEN
    }

    "return a Unauthorized (401) when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      postRequest(IE815).status mustBe UNAUTHORIZED
    }

    "return bad request (400) when xml cannot be parsed" in {
      withAuthorizedTrader("GBWK002281023")

      postRequest(<IE815></IE815>).status mustBe BAD_REQUEST
    }

    "return Unsupported Media Type (415)" in {
      withAuthorizedTrader("GBWK002281023")
      postRequest(contentType = """application/json""").status mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "return bad request (400) when body is not xml" in {
      withAuthorizedTrader("GBWK002281023")

      val result = await(wsClient.url(url)
        .addHttpHeaders(
          HeaderNames.AUTHORIZATION -> "TOKEN",
          HeaderNames.CONTENT_TYPE -> """application/vnd.hmrc.1.0+xml"""
        ).post("test")
      )

      result.status mustBe BAD_REQUEST
    }

    "return forbidden (403) when consignor id cannot be validate" in {
      withAuthorizedTrader()

      postRequest(IE815).status mustBe FORBIDDEN
    }
  }

  private def setupRepositories = {
    when(movementRepository.saveMovement(any))
      .thenReturn(Future.successful(true))

    when(movementRepository.getMovementByLRNAndERNIn(any, any))
      .thenReturn(Future.successful(Seq.empty))
  }

  private def assertValidResult(result: WSResponse) = {
    val responseBody = Json.parse(result.body).as[ExciseMovementResponse]
    responseBody.status mustBe "Accepted"
    responseBody.boxId mustBe boxId
    UUID.fromString(responseBody.movementId).toString must not be empty //mustNot Throw An Exception
    responseBody.consignorId mustBe consignorId
    responseBody.localReferenceNumber mustBe "LRNQA20230909022221"
    responseBody.consigneeId mustBe Some("GBWKQOZ8OVLYR")
  }

  private def createEISErrorResponseBodyAsJson(message: String): JsValue = {
    Json.toJson(EISErrorResponse(
      Instant.parse("2023-12-05T12:05:06Z"),
      message,
      s"debug $message",
      "123"
    ))
  }

  private def postRequest(xml: NodeSeq = IE815, contentType: String = """application/vnd.hmrc.1.0+xml""") = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN",
        HeaderNames.CONTENT_TYPE -> contentType,
        "X-Client-Id" -> "clientId"
      ).post(xml)
    )
  }

  private def postRequestWithoutClientId = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN",
        HeaderNames.CONTENT_TYPE ->  "application/vnd.hmrc.1.0+xml",
      ).post(IE815)
    )
  }

  private def stubEISSuccessfulRequest = {

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

  private def stubNrsErrorResponse = {
    wireMock.stubFor(
      post(urlEqualTo("/submission"))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
        )
    )
  }

  private def stubGetBoxIdSuccessRequest = {
    wireMock.stubFor {
      get(urlPathEqualTo(s"""/box"""))
        .withQueryParam("boxName",equalTo(Constants.BoxName))
        .withQueryParam("clientId", equalTo("clientId"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(
              s"""
                {
                  "boxId": "${boxId}",
                  "boxName":"customs/excise##1.0##notificationUrl",
                  "boxCreator":{
                      "clientId": "testClientId"
                  },
                  "subscriber": {
                      "subscribedDateTime": "2020-06-01T10:27:33.613+0000",
                      "callBackUrl": "https://www.example.com/callback",
                      "subscriptionType": "API_PUSH_SUBSCRIBER"
                  }
                }
              """)
        )
    }
  }

  private def stubGetBoxIdFailureRequest(status: Int, body: String) = {
    wireMock.stubFor {
      get(urlPathEqualTo(s"""/box"""))
        .withQueryParam("boxName",equalTo(Constants.BoxName))
        .withQueryParam("clientId", equalTo("clientId"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    }
  }
}
