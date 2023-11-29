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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, ok, post, urlEqualTo}
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, InternalError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.TestXml
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.{RepositoryTestStub, WireMockServerSpec}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ExciseMovementResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.eis.{EISErrorResponse, EISSubmissionResponse}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ExciseNumberQueueWorkItemRepository, MovementRepository}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.{Instant, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq


class DraftExciseMovementControllerItSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with AuthTestSupport
  with TestXml
  with WireMockServerSpec
  with RepositoryTestStub
  with BeforeAndAfterAll {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val url = s"http://localhost:$port/movements"
  private val eisUrl = "/emcs/digital-submit-new-message/v1"
  private val consignorId = "GBWK002281023"
  private val consigneeId = "GBWKQOZ8OVLYR"

  private val eisErrorResponse = "{" +
    "\"message\": \"Validation error(s) occurred\"," +
    "\"errors\": [" +
    "{\"errorCode\": 8084," +
    "\"errorMessage\": \"The Date of Dispatch you entered is incorrect. It must be today or later. Please amend your entry and resubmit.\"," +
    "\"location\": \"/con:Control[1]/con:OperationRequest[1]/con:Parameters[1]/con:Parameter[1]/urn:IE815[1]/urn:Body[1]/urn:SubmittedDraftOfEADESAD[1]/urn:EadEsadDraft[1]/urn:DateOfDispatch[1]\"," +
    "\"value\": \"2023-12-05\"}" +
    "]" +
    "}"

  private val apiErrorResponse = "{\"message\": \"Validation error(s) occurred\",\"errors\": [" +
    "{\"errorCode\": 8084," +
    "\"errorMessage\": \"The Date of Dispatch you entered is incorrect. It must be today or later. Please amend your entry and resubmit.\"," +
    "\"location\": \"/urn:IE815[1]/urn:Body[1]/urn:SubmittedDraftOfEADESAD[1]/urn:EadEsadDraft[1]/urn:DateOfDispatch[1]\"," +
    "\"value\": \"2023-12-05\"}]}"

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
      .configure(configureServer)
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

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "Draft Excise Movement" should {

    "return 202" in {
      withAuthorizedTrader(consignorId)
      stubEISSuccessfulRequest()
      when(movementRepository.saveMovement(any))
        .thenReturn(Future.successful(true))

      when(workItemRepository.pushNew(any, any, any)).thenReturn(Future.successful(workItem))
      when(workItemRepository.getWorkItemForErn(any)).thenReturn(Future.successful(None))

      when(movementRepository.getMovementByLRNAndERNIn(any, any))
        .thenReturn(Future.successful(Seq.empty))

      val result = postRequest(IE815)

      result.status mustBe ACCEPTED

      withClue("return the json response") {
        val responseBody = Json.parse(result.body).as[ExciseMovementResponse]
        responseBody mustBe ExciseMovementResponse("Accepted", "LRNQA20230909022221", consignorId, Some("GBWKQOZ8OVLYR"))
      }

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

    "return bad request if EIS returns BAD_REQUEST" in {
      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(BAD_REQUEST, createEISErrorResponseBodyAsJson("BAD_REQUEST").toString())

      postRequest(IE815).status mustBe BAD_REQUEST
    }

    "remove control document references in any paths for a BAD_REQUEST" in {

      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(BAD_REQUEST, eisErrorResponse)

      val response = postRequest(IE815)
      //Calling body directly is causing everything to be wrapped in extra strings which is confusing
      //Hence getting the json as a string directly
      cleanUpString(response.json.as[String]) mustBe cleanUpString(apiErrorResponse)

    }

    "return 500 if EIS returns 500" in {
      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(INTERNAL_SERVER_ERROR, createEISErrorResponseBodyAsJson("INTERNAL_SERVER_ERROR").toString())

      postRequest(IE815).status mustBe INTERNAL_SERVER_ERROR
    }

    "return 500 if EIS returns bad json" in {
      withAuthorizedTrader(consignorId)
      stubEISErrorResponse(INTERNAL_SERVER_ERROR, """"{"json": "is-bad"}""")

      postRequest(IE815).status mustBe INTERNAL_SERVER_ERROR
    }

    "return forbidden (403) when there are no authorized ERN" in {
      withUnAuthorizedERN()

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
      withAuthorizedTrader("123")

      postRequest(IE815).status mustBe FORBIDDEN
    }
  }

  private def createEISErrorResponseBodyAsJson(message: String): JsValue = {
    Json.toJson(EISErrorResponse(
      LocalDateTime.of(2023, 12, 5, 12, 5, 6),
      message,
      s"debug $message",
      "123"
    ))
  }

  private def postRequest(xml: NodeSeq = IE815, contentType: String = """application/vnd.hmrc.1.0+xml""") = {
    await(wsClient.url(url)
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

  private def cleanUpString(str: String): String = {
    str.replaceAll("[\\t\\n\\r\\s]+", "")
  }
}
