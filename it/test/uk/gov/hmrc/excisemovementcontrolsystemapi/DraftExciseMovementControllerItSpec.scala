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
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.{MatchResult, RequestMatcherExtension}
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{verify, when, reset => mockitoSugerReset}
import org.mockito.captor.ArgCaptor
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISSubmissionRequest, EISSubmissionResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.notification.Constants
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.nrs.NonRepudiationSubmissionAccepted
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, ExciseMovementResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumberWorkItem, Movement}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq


class DraftExciseMovementControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with ApplicationBuilderSupport
  with TestXml
  with WireMockServerSpec
  with SubmitMessageTestSupport
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
  private val defaultBoxId = "testBoxId"
  private val clientBoxId = UUID.randomUUID().toString
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

    applicationBuilder(configureServices).build()
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
    mockitoSugerReset(
      dateTimeService,
      movementRepository,
      authConnector,
      workItemRepository
    )

    wireMock.resetAll()
    stubNrsResponse
    authorizeNrsWithIdentityData
    stubGetBoxIdSuccessRequest
    when(workItemRepository.pushNew(any, any, any)).thenReturn(Future.successful(workItem))
    when(workItemRepository.getWorkItemForErn(any)).thenReturn(Future.successful(None))
    when(dateTimeService.timestamp()).thenReturn(timeStamp)
    when(dateTimeService.timestampToMilliseconds()).thenReturn(timeStamp.truncatedTo(ChronoUnit.MILLIS))

  }

  "Draft Excise Movement" should {

    "return 202" when {

      "all is successful" in {
        withAuthorizedTrader(consignorId)
        stubEISSuccessfulRequest
        setupRepositories

        val result = postRequest(IE815)

        result.status mustBe ACCEPTED
        withClue("return the json response") {
          assertValidResult(result, defaultBoxId)
        }

        withClue("submit to NRS") {
          wireMock.verify(postRequestedFor(urlEqualTo("/submission")))
        }
      }

      "should get the default box Id when X-Callback-Box-Id is not in the header" in {
        withAuthorizedTrader(consignorId)
        stubEISSuccessfulRequest
        setupRepositories

        val result = postRequest(IE815)

        result.status mustBe ACCEPTED

        withClue("get the default boxId") {
          wireMock.verify(getRequestedFor(urlMatching("^/box.*"))
            .withQueryParam("boxName", equalTo("customs/excise##1.0##notificationUrl"))
            .withQueryParam("clientId", matching("clientId"))
          )
        }
        withClue("should save the default box id to db") {
          verifyBoxIdIsSavedToDB(defaultBoxId)
        }
      }

      "return the client box id when X-Callback-Box-Id is present in the header" in {
        withAuthorizedTrader(consignorId)
        stubEISSuccessfulRequest
        setupRepositories

        val result = postRequestWithClientBoxId

        assertValidResult(result, clientBoxId)
        withClue("should not send a request to the push notification service") {
          wireMock.verify(0, getRequestedFor(urlMatching("^/box.*")))
        }

        withClue("should save the client box id to db") {
          verifyBoxIdIsSavedToDB(clientBoxId)
        }
      }

      "NRS fails" in {
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

      "NRS throws an exception" in {
        withAuthorizedTrader(consignorId)
        stubEISSuccessfulRequest
        stubNrsErrorResponse
        setupRepositories

        // no Authorization header added
        val result = await(wsClient.url(url).addHttpHeaders("X-Client-Id" -> "clientId").post(IE815))

        result.status mustBe ACCEPTED
      }

    }

    "return a 400 Bad Request error" when {

      "notification service returns a 400" in {
        withAuthorizedTrader(consignorId)
        stubEISSuccessfulRequest
        setupRepositories
        stubGetBoxIdFailureRequest(BAD_REQUEST, "Missing or incorrect query parameter")

        val result = postRequest(IE815)

        result.status mustBe BAD_REQUEST
        withClue("return the json response") {
          val expectedJson =
            s"""{"dateTime":"$timeStamp",
               |"message":"Box Id error",
               |"debugMessage":"Missing or incorrect query parameter"}""".stripMargin
          Json.parse(result.body) mustBe Json.parse(expectedJson)
        }

        withClue("should not submit to NRS") {
          wireMock.verify(0, postRequestedFor(urlEqualTo("/submission")))
        }

        withClue("should not submit message") {
          wireMock.verify(0, postRequestedFor(urlEqualTo(eisUrl)))
        }
      }

      "clientId is missing in the header" in {
        withAuthorizedTrader(consignorId)

        val result = postRequestWithoutClientId

        result.status mustBe BAD_REQUEST
        withClue("return the json response") {
          val responseBody = result.json.as[ErrorResponse]
          responseBody.dateTime.truncatedTo(ChronoUnit.MINUTES) mustBe timeStamp.truncatedTo(ChronoUnit.MINUTES)
          responseBody.message mustBe "ClientId error"
          responseBody.debugMessage mustBe "Request header is missing X-Client-Id"
        }
      }

      "EIS returns BAD_REQUEST" in {
        withAuthorizedTrader(consignorId)
        stubEISErrorResponse(BAD_REQUEST, createEISErrorResponseBodyAsJson("BAD_REQUEST").toString())

        postRequest(IE815).status mustBe BAD_REQUEST
      }

      "user sending the wrong message" in {
        withAuthorizedTrader(consignorId)
        stubEISSuccessfulRequest
        setupRepositories

        val result = postRequest(IE818)

        result.status mustBe BAD_REQUEST
        val expectedJson =
          s"""{"dateTime":"$timeStamp",
             |"message":"Invalid message type",
             |"debugMessage":"Message type IE818 cannot be sent to the draft excise movement endpoint"}""".stripMargin

        Json.parse(result.body) mustBe Json.parse(expectedJson)
      }

      "xml cannot be parsed" in {
        withAuthorizedTrader("GBWK002281023")

        postRequest(<IE815></IE815>).status mustBe BAD_REQUEST
      }

      "body is not xml" in {
        withAuthorizedTrader("GBWK002281023")

        val result = await(wsClient.url(url)
          .addHttpHeaders(
            HeaderNames.AUTHORIZATION -> "TOKEN",
            HeaderNames.CONTENT_TYPE -> """application/vnd.hmrc.1.0+xml"""
          ).post("test")
        )

        result.status mustBe BAD_REQUEST
      }

      "rim validation error" in {

        withAuthorizedTrader(consignorId)
        stubEISErrorResponse(BAD_REQUEST, rimValidationErrorResponse(messageWithControlDoc))

        val response = postRequest(IE815)

        withClue("must remove control document references in any paths") {
          clean(response.body) mustBe clean(rimValidationErrorResponse(messageWithoutControlDoc))
        }
      }

      "supplied with message that is not an IE815" in {
        withAuthorizedTrader(consignorId)

        postRequest(IE818).status mustBe BAD_REQUEST

      }
    }

    "return a 401 Unauthorised" when {
      "no authorized trader" in {
        withUnauthorizedTrader(InternalError("A general auth failure"))

        postRequest(IE815).status mustBe UNAUTHORIZED
      }
    }

    "return a 403 Forbidden" when {
      "there are no authorized ERN" in {
        withAnEmptyERN()

        postRequest(IE815).status mustBe FORBIDDEN
      }

      "the consignee is trying to send in an IE815" in {
        withAuthorizedTrader(consigneeId)

        postRequest(IE815).status mustBe FORBIDDEN
      }

      "the consignor is empty" in {
        withAuthorizedTrader(consignorId)

        postRequest(IE815WithNoConsignor).status mustBe FORBIDDEN
      }

      "consignor id cannot be validate" in {
        withAuthorizedTrader()

        postRequest(IE815).status mustBe FORBIDDEN
      }
    }

    "return a 404 Not Found error" when {
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
    }

    "return a 415 Unsupported Media Type" when {
      "supplied with Json" in {
        withAuthorizedTrader("GBWK002281023")
        postRequest(contentType = """application/json""").status mustBe UNSUPPORTED_MEDIA_TYPE
      }
    }

    "return a 422 Unprocessable Entity" when {
      "EIS returns UNPROCESSABLE_ENTRY" in {
        withAuthorizedTrader(consignorId)
        stubEISErrorResponse(UNPROCESSABLE_ENTITY, createEISErrorResponseBodyAsJson("Unprocessable_Entity").toString())

        postRequest(IE815).status mustBe UNPROCESSABLE_ENTITY
      }

    }

    "return a 500 Internal Server Error" when {
      "EIS returns 500" in {
        withAuthorizedTrader(consignorId)
        stubEISErrorResponse(INTERNAL_SERVER_ERROR, createEISErrorResponseBodyAsJson("INTERNAL_SERVER_ERROR").toString())

        postRequest(IE815).status mustBe INTERNAL_SERVER_ERROR
      }

      "EIS returns bad json" in {
        withAuthorizedTrader(consignorId)
        stubEISErrorResponse(INTERNAL_SERVER_ERROR, """"{"json": "is-bad"}""")

        postRequest(IE815).status mustBe INTERNAL_SERVER_ERROR
      }

    }
  }

  private def verifyBoxIdIsSavedToDB(boxId: String) = {
    val captor = ArgCaptor[Movement]
    verify(movementRepository).saveMovement(captor.capture)
    captor.value.boxId mustBe Some(boxId)
  }

  private def setupRepositories = {
    when(movementRepository.saveMovement(any))
      .thenReturn(Future.successful(true))

    when(movementRepository.getMovementByLRNAndERNIn(any, any))
      .thenReturn(Future.successful(Seq.empty))
  }

  private def assertValidResult(result: WSResponse, expectedBoxId: String = defaultBoxId) = {
    val responseBody = Json.parse(result.body).as[ExciseMovementResponse]
    responseBody.status mustBe "Accepted"
    responseBody.boxId mustBe Some(expectedBoxId)
    UUID.fromString(responseBody.movementId).toString must not be empty //mustNot Throw An Exception
    responseBody.consignorId mustBe consignorId
    responseBody.localReferenceNumber mustBe "LRNQA20230909022221"
    responseBody.consigneeId mustBe Some("GBWKQOZ8OVLYR")
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

  private def postRequestWithClientBoxId = {
    await(wsClient.url(url)
      .addHttpHeaders(
        HeaderNames.AUTHORIZATION -> "TOKEN",
        HeaderNames.CONTENT_TYPE -> """application/vnd.hmrc.1.0+xml""",
        "X-Client-Id" -> "clientId",
        "X-Callback-Box-Id" -> clientBoxId
      ).post(IE815)
    )
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
        HeaderNames.CONTENT_TYPE -> "application/vnd.hmrc.1.0+xml",
      ).post(IE815)
    )
  }

  private def stubEISSuccessfulRequest = {

    val response = EISSubmissionResponse("OK", "message", "123")

    wireMock.stubFor(
      post(eisUrl)
        .andMatching(new Base64DecodedMatcher)
        .willReturn(ok().withBody(Json.toJson(response).toString()))
    )

  }

  private def stubEISErrorResponse(status: Int, body: String) = {

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
        .withQueryParam("boxName", equalTo(Constants.BoxName))
        .withQueryParam("clientId", equalTo("clientId"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(
              s"""
                {
                  "boxId": "$defaultBoxId",
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
        .withQueryParam("boxName", equalTo(Constants.BoxName))
        .withQueryParam("clientId", equalTo("clientId"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    }
  }

  private class Base64DecodedMatcher extends RequestMatcherExtension {

    override def `match`(request: Request, parameters: Parameters): MatchResult = {

      if (request.getUrl == eisUrl) {
        // Only want to check the base 64 body if the request is for submitting the message

        Try(Json.parse(request.getBodyAsString).as[EISSubmissionRequest].message) match {
          case Success(message) =>
            val decodedBody = Base64.getDecoder.decode(message).map(_.toChar).mkString("")

            MatchResult.aggregate(
              MatchResult.of(decodedBody.contains("con:Parameter Name=\"ExciseRegistrationNumber\"")),
              MatchResult.of(decodedBody.contains("con:Parameter Name=\"message\""))
            )

          case Failure(_) =>
            MatchResult.noMatch()
        }

      } else {
        // If it is here because it is trying to match say GetBoxId then nothing to check
        MatchResult.exactMatch()
      }

    }

  }

}
