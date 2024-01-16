package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import dispatch.Future
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{FORBIDDEN, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.{FakeAuthentication, FakeValidateMovementIdAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.ErrorResponse
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService

import java.time.Instant
import scala.concurrent.ExecutionContext

class GetMovementControllerSpec
  extends PlaySpec
    with FakeAuthentication
    with FakeValidateMovementIdAction
    with BeforeAndAfterEach {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()
  private val fakeRequest = FakeRequest("POST", "/foo")
  private val movementService = mock[MovementService]
  private val dateTimeService = mock[DateTimeService]
  private val timestamp = Instant.parse("2018-11-30T18:35:24.00Z")

  private val controller = new GetMovementController(
    FakeSuccessAuthentication,
    movementService,
    cc
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(movementService)
    when(dateTimeService.timestamp()).thenReturn(timestamp)
  }


  "Get movement controller" should {

    "return the movement when successful" in {

      when(movementService.getMovementById(eqTo("id123"))).thenReturn(Future.successful(Some(
        Movement("id123","lrn1", "consignor", Some("consignee"), Some("arc"),Instant.now(), Seq.empty)
      )))

      val result = controller.getMovement("id123")(fakeRequest)

      status(result) mustBe OK

    }

    "return Not Found error" when {
      "movement not found in database" in {

        when(movementService.getMovementById(eqTo("id123"))).thenReturn(Future.successful(None))
       val result = controller.getMovement("id123")(fakeRequest)

        status(result) mustBe NOT_FOUND

        contentAsJson(result) mustBe Json.toJson(
          ErrorResponse(timestamp, "Movement not found", "Movement id123 is not found")
        )

      }
    }

    "return authentication error" when {
      "authentication fails" in {
        val result = createWithAuthActionFailure.getMovement("id123")(fakeRequest)

        status(result) mustBe FORBIDDEN
      }
    }

  }

  private def createWithAuthActionFailure = new GetMovementController(
    FakeFailingAuthentication,
    movementService,
    cc
  )


}
