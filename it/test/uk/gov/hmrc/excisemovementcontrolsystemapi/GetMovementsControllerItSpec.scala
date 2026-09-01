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

import org.apache.pekko.stream.scaladsl.Source
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.MovementTestUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures.ApplicationBuilderSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class GetMovementsControllerItSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with ApplicationBuilderSupport
    with MovementTestUtils
    with BeforeAndAfterEach {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private lazy val wsClient: WSClient         = app.injector.instanceOf[WSClient]

  private val consignorId         = "GBWK002281023"
  private val consigneeId         = "GBWK002281027"
  private val lrn                 = "token"
  private val baseUrl             = s"http://localhost:$port/movements"
  private val timestampNow        = Instant.now()
  private val timestampTwoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS)

  private val movement1 = Movement(Some("boxId"), lrn, consignorId, Some(consigneeId), Some("arc1"), timestampNow)
  private val movement2 =
    Movement(Some("boxId"), "lrn1", consignorId, Some("consignee2"), Some("arc2"), timestampTwoDaysAgo)

  override lazy val app: Application = applicationBuilder.build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(dateTimeService)

    when(dateTimeService.timestamp()).thenReturn(Instant.now)
    when(ernRetrievalRepository.getLastRetrieved(any)).thenReturn(Future.successful(None))
  }

  "Get Movements" should {
    "return 200 and movements when logged in as consignor" in {
      withAuthorizedTrader(consignorId)
      when(
        movementRepository.streamMovementsByERN(Seq(consignorId))
      )
        .thenReturn(Source.fromIterator(() => Iterator(movement1, movement2)))

      val result = getRequest(baseUrl)

      result.status mustBe OK
      withClue("return an EIS response") {
        result.json mustBe Json.toJson(
          Seq(
            createMovementResponseFromMovement(movement1),
            createMovementResponseFromMovement(movement2)
          )
        )
      }
    }

    "return 200 with a large stream" in {
      val timestamp = Instant.parse("2024-10-05T12:12:12.12345678Z")
      val encodedMessage = "PGllODM3OklFODM3IHhtbG5zPSJodHRwOi8vd3d3LmdvdnRhbGsuZ292LnVrL3RheGF0aW9uL0ludGVybmF0aW9uYWxUcmFkZS9FeGNpc2UvTW92ZW1lbnRGb3JUcmFkZXJEYXRhLzMiIHhtbG5zOmRvYz0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpET0M6VjMuMjMiIHhtbG5zOmVtY3M9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6RU1DUzpWMy4yMyIgeG1sbnM6ZXVjPSJodHRwOi8vd3d3LmdvdnRhbGsuZ292LnVrL3RheGF0aW9uL0ludGVybmF0aW9uYWxUcmFkZS9FeGNpc2UvRW1jc1VrQ29kZXMvMyIgeG1sbnM6aWUwPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODgwOlYzLjIzIiB4bWxuczppZTE9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4MjU6VjMuMjMiIHhtbG5zOmllMj0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTcxNzpWMy4yMyIgeG1sbnM6aWUzPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODE1OlYzLjIzIiB4bWxuczppZT0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTkzNDpWMy4yMyIgeG1sbnM6aWU3MDR1az0iaHR0cDovL3d3dy5nb3Z0YWxrLmdvdi51ay90YXhhdGlvbi9JbnRlcm5hdGlvbmFsVHJhZGUvRXhjaXNlL2llNzA0dWsvMyIgeG1sbnM6aWU4MDE9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4MDE6VjMuMjMiIHhtbG5zOmllODAyPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODAyOlYzLjIzIiB4bWxuczppZTgwMz0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTgwMzpWMy4yMyIgeG1sbnM6aWU4MDc9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4MDc6VjMuMjMiIHhtbG5zOmllODEwPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODEwOlYzLjIzIiB4bWxuczppZTgxMz0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTgxMzpWMy4yMyIgeG1sbnM6aWU4MTg9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4MTg6VjMuMjMiIHhtbG5zOmllODE5PSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODE5OlYzLjIzIiB4bWxuczppZTgyOT0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTgyOTpWMy4yMyIgeG1sbnM6aWU4Mzc9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4Mzc6VjMuMjMiIHhtbG5zOmllODM5PSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODM5OlYzLjIzIiB4bWxuczppZTg0MD0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTg0MDpWMy4yMyIgeG1sbnM6aWU4NzE9InVybjpwdWJsaWNpZDotOkVDOkRHVEFYVUQ6RU1DUzpQSEFTRTQ6SUU4NzE6VjMuMjMiIHhtbG5zOmllODgxPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OklFODgxOlYzLjIzIiB4bWxuczppZTkwNT0idXJuOnB1YmxpY2lkOi06RUM6REdUQVhVRDpFTUNTOlBIQVNFNDpJRTkwNTpWMy4yMyIgeG1sbnM6dGNsPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OlRDTDpWMy4yMyIgeG1sbnM6dG1zPSJ1cm46cHVibGljaWQ6LTpFQzpER1RBWFVEOkVNQ1M6UEhBU0U0OlRNUzpWMy4yMyIgeG1sbnM6dG5zND0iaHR0cDovL3d3dy5nb3Z0YWxrLmdvdi51ay90YXhhdGlvbi9JbnRlcm5hdGlvbmFsVHJhZGUvQ29tbW9uL0NvbnRyb2xEb2N1bWVudCIgeG1sbnM6dG5zNT0iaHR0cDovL3d3dy5nb3Z0YWxrLmdvdi51ay90YXhhdGlvbi9JbnRlcm5hdGlvbmFsVHJhZGUvRXhjaXNlL01vdmVtZW50Rm9yVHJhZGVyRGF0YS8zIiB4bWxuczp0bnM9Imh0dHA6Ly93d3cuZ292dGFsay5nb3YudWsvdGF4YXRpb24vSW50ZXJuYXRpb25hbFRyYWRlL0V4Y2lzZS9OZXdNZXNzYWdlc0RhdGEvMyIgeG1sbnM6eHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hIiB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIj48aWU4Mzc6SGVhZGVyPjx0bXM6TWVzc2FnZVNlbmRlcj5OREVBLkdCPC90bXM6TWVzc2FnZVNlbmRlcj48dG1zOk1lc3NhZ2VSZWNpcGllbnQ+TkRFQS5HQjwvdG1zOk1lc3NhZ2VSZWNpcGllbnQ+PHRtczpEYXRlT2ZQcmVwYXJhdGlvbj4yMDI0LTA2LTI1PC90bXM6RGF0ZU9mUHJlcGFyYXRpb24+PHRtczpUaW1lT2ZQcmVwYXJhdGlvbj4xNzoyNjoxNS43NDk1MDA8L3RtczpUaW1lT2ZQcmVwYXJhdGlvbj48dG1zOk1lc3NhZ2VJZGVudGlmaWVyPjY5NDI0YzgyLTMxNmItNDExMi04MDAwLWRjNTcyMDk3MDBjNjwvdG1zOk1lc3NhZ2VJZGVudGlmaWVyPjx0bXM6Q29ycmVsYXRpb25JZGVudGlmaWVyPmQ5YzhiY2M0LTI4ZWEtNDI3OC04ZTllLWE0NWY5NjdjYmJmZjwvdG1zOkNvcnJlbGF0aW9uSWRlbnRpZmllcj48L2llODM3OkhlYWRlcj48aWU4Mzc6Qm9keT48aWU4Mzc6RXhwbGFuYXRpb25PbkRlbGF5Rm9yRGVsaXZlcnk+PGllODM3OkF0dHJpYnV0ZXM+PGllODM3OlN1Ym1pdHRlcklkZW50aWZpY2F0aW9uPkdCV0swMDIyODEwMjM8L2llODM3OlN1Ym1pdHRlcklkZW50aWZpY2F0aW9uPjxpZTgzNzpTdWJtaXR0ZXJUeXBlPjE8L2llODM3OlN1Ym1pdHRlclR5cGU+PGllODM3OkV4cGxhbmF0aW9uQ29kZT40PC9pZTgzNzpFeHBsYW5hdGlvbkNvZGU+PGllODM3Ok1lc3NhZ2VSb2xlPjI8L2llODM3Ok1lc3NhZ2VSb2xlPjxpZTgzNzpEYXRlQW5kVGltZU9mVmFsaWRhdGlvbk9mRXhwbGFuYXRpb25PbkRlbGF5PjIwMjQtMDYtMjVUMTc6MjY6MTcuNjIzWjwvaWU4Mzc6RGF0ZUFuZFRpbWVPZlZhbGlkYXRpb25PZkV4cGxhbmF0aW9uT25EZWxheT48L2llODM3OkF0dHJpYnV0ZXM+PGllODM3OkV4Y2lzZU1vdmVtZW50PjxpZTgzNzpBZG1pbmlzdHJhdGl2ZVJlZmVyZW5jZUNvZGU+MjRHQjAwMDAwMDAwMDAwMzkyODY3PC9pZTgzNzpBZG1pbmlzdHJhdGl2ZVJlZmVyZW5jZUNvZGU+PGllODM3OlNlcXVlbmNlTnVtYmVyPjE8L2llODM3OlNlcXVlbmNlTnVtYmVyPjwvaWU4Mzc6RXhjaXNlTW92ZW1lbnQ+PC9pZTgzNzpFeHBsYW5hdGlvbk9uRGVsYXlGb3JEZWxpdmVyeT48L2llODM3OkJvZHk+PC9pZTgzNzpJRTgzNz4="

      withAuthorizedTrader(consignorId)
      when(
        movementRepository.streamMovementsByERN(Seq(consignorId))
      ).thenReturn(Source.fromIterator(() => Iterator.fill(90000)(
        movement1.copy(messages = Seq.fill(6)(Message(
          encodedMessage,
          "IE801",
          "messageId",
          "ern",
          Set.empty,
          timestamp
        )))))
      )

      val requests = for(_ <- 0 to 19) yield clientRequest(baseUrl)

      await(Future.sequence(requests)).foreach(
        _.status mustBe OK
      )
    }

    def clientRequest(url: String) = {
      Future(getRequest(url))
    }

    "return an Unauthorized (401) when no authorized trader" in {
      withUnauthorizedTrader(InternalError("A general auth failure"))

      getRequest(baseUrl).status mustBe UNAUTHORIZED
    }

    "return a Bad Request (400) when not logged in as the filtering trader" in {

      withAuthorizedTrader(consignorId)

      getRequest(s"$baseUrl?ern=GBWK002281024").status mustBe BAD_REQUEST

    }
  }

  "Get Movement" should {

    val movementId = "cfdb20c7-d0b0-4b8b-a071-737d68dede5b"
    val movement   = Movement(
      movementId,
      Some("boxId"),
      "LRNQA20230909022221",
      consignorId,
      Some(consigneeId),
      Some("23GB00000000000377161"),
      timestampNow,
      Seq.empty
    )

    "return 200 Success and movement details when consignor" in {

      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(Some(movement)))

      withAuthorizedTrader(consignorId)

      val result = getRequest(getUrl(movementId))

      result.status mustBe OK

      result.json mustBe Json.toJson(createMovementResponseFromMovement(movement))
    }

    "return 200 Success and movement details when consignee" in {

      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(Some(movement)))

      withAuthorizedTrader(consigneeId)

      val result = getRequest(getUrl(movementId))

      result.status mustBe OK
      result.json mustBe Json.toJson(createMovementResponseFromMovement(movement))

    }

    "return 401 Unauthorised when not authorised trader" in {

      withUnauthorizedTrader(InternalError("A general auth failure"))

      val result = getRequest(getUrl(movementId))

      result.status mustBe UNAUTHORIZED
    }

    "return 404 Not Found when movement is not in database" in {
      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(None))

      withAuthorizedTrader(consigneeId)

      val result = getRequest(getUrl(movementId))

      result.status mustBe NOT_FOUND

    }

    "return 404 Not Found when movement is not valid for your login" in {
      when(movementRepository.getMovementById(eqTo(movementId)))
        .thenReturn(Future.successful(Some(movement)))

      withAuthorizedTrader("differentPerson")

      val result = getRequest(getUrl(movementId))

      result.status mustBe NOT_FOUND

    }

    "return 400 Bad Request when UUID is wrong format" in {
      withAuthorizedTrader(consignorId)

      val result = getRequest(getUrl("nfbfs78-432nfsd-4123"))

      result.status mustBe BAD_REQUEST

    }

    def getUrl(movementId: String) = s"$baseUrl/$movementId"

  }

  private def getRequest(url: String) =
    await(
      wsClient
        .url(url)
        .addHttpHeaders(
          HeaderNames.AUTHORIZATION -> "TOKEN"
        )
        .get()
    )
}
