package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import dispatch.Future
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.FakeAuthentication
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.MovementService

import java.time.Instant
import scala.concurrent.ExecutionContext

class GetMovementControllerSpec
  extends PlaySpec
    with FakeAuthentication {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val cc = stubControllerComponents()
  private val fakeRequest = FakeRequest("POST", "/foo")
  private val movementService = mock[MovementService]

  private val controller = new GetMovementController(FakeSuccessAuthentication, movementService, cc)

  "Get movement controller" should {

    "return the movement when successful" in {

      when(movementService.getMovementById(eqTo("id123"))).thenReturn(Future.successful(Some(
        Movement("id123","lrn1", "consignor", Some("consignee"), Some("arc"),Instant.now(), Seq.empty)
      )))

      val result = controller.getMovement("id123")(fakeRequest)

      status(result) mustBe OK

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
