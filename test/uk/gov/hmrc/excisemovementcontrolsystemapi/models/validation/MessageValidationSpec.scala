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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation

import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE810Message, IE815Message}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

class MessageValidationSpec extends PlaySpec with EitherValues {
  "validateDraftMovement" should {
    val ie815 = mock[IE815Message]
    val authorisedErns = Set("123", "456")
    "return Consignor Id" when {
      "the consignor Id is authorised" in {
        when(ie815.consignorId).thenReturn("123")
        MessageValidation.validateDraftMovement(authorisedErns, ie815) mustBe Right("123")
      }
    }
    "return consignor unauthorised error" when {
      "the consignor Id is unauthorised" in {
        when(ie815.consignorId).thenReturn("789")
        val result = MessageValidation.validateDraftMovement(authorisedErns, ie815).left.value
        result mustBe a [MessageIdentifierIsUnauthorised]
        result.errorMessage mustBe "The Consignor is not authorised to submit this message for the movement"
      }
    }
  }

  "validateSubmittedMessage for an IE810 message" should {
    val ie810 = mock[IE810Message]
    val movement = mock[Movement]
    val authorisedErns = Set("123", "456")
    "return movement consignor Id" when {
      "the consignor Id is authorised and message ARC matches movement ARC" in {
        when(movement.consignorId).thenReturn("123")
        when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
        when(ie810.administrativeReferenceCode).thenReturn(Seq(Some("ARC1")))
        MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie810) mustBe Right("123")
      }
    }
    "return ARC does not match" when {
      "the message arc does not match the movement ARC" in {
        when(movement.consignorId).thenReturn("123")
        when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
        when(ie810.administrativeReferenceCode).thenReturn(Seq(Some("ARC2")))
        val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie810).left.value
        result mustBe a[MessageDoesNotMatchMovement]
        result.errorMessage mustBe "The ARC in the message does not match the ARC in the movement"
      }
    }
    "return consignor unauthorised error" when {
      "the movement consignor Id is unauthorised" in {
        when(movement.consignorId).thenReturn("789")
        val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie810).left.value
        result mustBe a[MessageIdentifierIsUnauthorised]
        result.errorMessage mustBe "The Consignor is not authorised to submit this message for the movement"
      }
    }
  }
  // TODO write more tests for validateSubmittedMessage
}
