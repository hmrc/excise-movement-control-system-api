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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.Movement

class MessageValidationSpec extends PlaySpec with EitherValues {

  private val authorisedErns = Set("123", "456")
  private val movement = mock[Movement]

  "validateDraftMovement" should {
    val ie815 = mock[IE815Message]

    "return consignor id" when {
      "the consignor id is authorised" in {
        when(ie815.consignorId).thenReturn("123")

        MessageValidation.validateDraftMovement(authorisedErns, ie815) mustBe Right("123")
      }
    }

    "return consignor unauthorised error" when {
      "the consignor id is unauthorised" in {
        when(ie815.consignorId).thenReturn("789")

        val result = MessageValidation.validateDraftMovement(authorisedErns, ie815).left.value
        result mustBe a[MessageIdentifierIsUnauthorised]
        result.errorMessage mustBe "The Consignor is not authorised to submit this message for the movement"
      }
    }
  }

  "validateSubmittedMessage" when {

    "an IE810 message" should {
      val ie810 = mock[IE810Message]

      "return movement consignor id" when {
        "the consignor id is authorised and message ARC matches movement ARC" in {
          when(movement.consignorId).thenReturn("123")
          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie810.administrativeReferenceCode).thenReturn(Seq(Some("ARC1")))
          MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie810) mustBe Right("123")
        }
      }

      "return ARC does not match" when {
        "the message ARC does not match the movement ARC" in {
          when(movement.consignorId).thenReturn("123")
          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie810.administrativeReferenceCode).thenReturn(Seq(Some("ARC2")))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie810).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The ARC in the message does not match the ARC in the movement"
        }
      }

      "return consignor unauthorised error" when {
        "the movement consignor id is unauthorised" in {
          when(movement.consignorId).thenReturn("789")

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie810).left.value
          result mustBe a[MessageIdentifierIsUnauthorised]
          result.errorMessage mustBe "The Consignor is not authorised to submit this message for the movement"
        }
      }

    }

    "an IE813 message" should {
      val ie813 = mock[IE813Message]

      "return movement consignor id" when {
        "the consignor id is authorised and message ARC matches movement ARC" in {
          when(movement.consignorId).thenReturn("123")
          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie813.administrativeReferenceCode).thenReturn(Seq(Some("ARC1")))

          MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie813) mustBe Right("123")
        }
      }

      "return ARC does not match" when {
        "the message ARC does not match the movement ARC" in {
          when(movement.consignorId).thenReturn("123")
          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie813.administrativeReferenceCode).thenReturn(Seq(Some("ARC2")))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie813).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The ARC in the message does not match the ARC in the movement"
        }
      }

      "return consignor unauthorised error" when {
        "the movement consignor id is unauthorised" in {
          when(movement.consignorId).thenReturn("789")

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie813).left.value
          result mustBe a[MessageIdentifierIsUnauthorised]
          result.errorMessage mustBe "The Consignor is not authorised to submit this message for the movement"
        }
      }

    }

    "an IE818 message" should {
      val ie818 = mock[IE818Message]

      "return movement consignee id" when {
        "the consignee id is authorised and message consignee matches the movement consignee and message ARC matches movement ARC" in {
          when(movement.consigneeId).thenReturn(Some("123"))
          when(ie818.consigneeId).thenReturn(Some("123"))
          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie818.administrativeReferenceCode).thenReturn(Seq(Some("ARC1")))

          MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie818) mustBe Right("123")
        }
      }

      "return ARC does not match" when {
        "the message ARC does not match the movement ARC" in {
          when(movement.consigneeId).thenReturn(Some("123"))
          when(ie818.consigneeId).thenReturn(Some("123"))

          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie818.administrativeReferenceCode).thenReturn(Seq(Some("ARC2")))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie818).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The ARC in the message does not match the ARC in the movement"
        }
      }

      "return consignee does not match" when {
        "the message consignee does not match the movement consignee" in {
          when(movement.consigneeId).thenReturn(Some("456"))
          when(ie818.consigneeId).thenReturn(Some("123"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie818).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The Consignee in the message does not match the Consignee in the movement"
        }
      }

      "return consignee unauthorised" when {
        "the message consignee id is unauthorised" in {
          when(ie818.consigneeId).thenReturn(Some("124"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie818).left.value
          result mustBe a[MessageIdentifierIsUnauthorised]
          result.errorMessage mustBe "The Consignee is not authorised to submit this message for the movement"
        }
      }

      "return consignee is missing" when {
        "the message consignee id field is empty" in {
          when(ie818.consigneeId).thenReturn(None)

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie818).left.value
          result mustBe a[MessageMissingKeyInformation]
          result.errorMessage mustBe "The Consignee in the message should not be empty"
        }
      }

    }

    "an IE819 message" should {
      val ie819 = mock[IE819Message]

      "return movement consignee id" when {
        "the consignee id is authorised and message consignee matches the movement consignee and message ARC matches movement ARC" in {
          when(movement.consigneeId).thenReturn(Some("123"))
          when(ie819.consigneeId).thenReturn(Some("123"))
          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie819.administrativeReferenceCode).thenReturn(Seq(Some("ARC1")))

          MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie819) mustBe Right("123")
        }
      }

      "return ARC does not match" when {
        "the message ARC does not match the movement ARC" in {
          when(movement.consigneeId).thenReturn(Some("123"))
          when(ie819.consigneeId).thenReturn(Some("123"))

          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie819.administrativeReferenceCode).thenReturn(Seq(Some("ARC2")))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie819).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The ARC in the message does not match the ARC in the movement"
        }
      }

      "return consignee does not match" when {
        "the message consignee does not match the movement consignee" in {
          when(movement.consigneeId).thenReturn(Some("456"))
          when(ie819.consigneeId).thenReturn(Some("123"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie819).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The Consignee in the message does not match the Consignee in the movement"
        }
      }

      "return consignee unauthorised" when {
        "the message consignee id is unauthorised" in {
          when(ie819.consigneeId).thenReturn(Some("124"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie819).left.value
          result mustBe a[MessageIdentifierIsUnauthorised]
          result.errorMessage mustBe "The Consignee is not authorised to submit this message for the movement"
        }
      }

      "return consignee is missing" when {
        "the message consignee id field is empty" in {
          when(ie819.consigneeId).thenReturn(None)

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie819).left.value
          result mustBe a[MessageMissingKeyInformation]
          result.errorMessage mustBe "The Consignee in the message should not be empty"
        }
      }

    }

    "an IE837 message" should {
      val ie837 = mock[IE837Message]

      "return movement consignor id" when {
        "the consignor is being used and the consignor id is authorised and message consignor matches the movement consignor and message ARC matches movement ARC" in {

          when(ie837.submitter).thenReturn(Consignor)
          when(movement.consignorId).thenReturn("123")
          when(ie837.consignorId).thenReturn(Some("123"))

          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie837.administrativeReferenceCode).thenReturn(Seq(Some("ARC1")))

          MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837) mustBe Right("123")
        }
      }

      "return movement consignee id" when {
        "the consignee is being used and the consignee id is authorised and message consignee matches the movement consignee and message ARC matches movement ARC" in {

          when(ie837.submitter).thenReturn(Consignee)
          when(movement.consigneeId).thenReturn(Some("123"))
          when(ie837.consigneeId).thenReturn(Some("123"))

          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie837.administrativeReferenceCode).thenReturn(Seq(Some("ARC1")))

          MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837) mustBe Right("123")
        }
      }

      "return ARC does not match" when {
        "the message ARC does not match the movement ARC" when {
          "sending the message as the consignor" in {
            when(ie837.submitter).thenReturn(Consignor)
            when(movement.consignorId).thenReturn("123")
            when(ie837.consignorId).thenReturn(Some("123"))

            when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
            when(ie837.administrativeReferenceCode).thenReturn(Seq(Some("ARC2")))

            val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837).left.value
            result mustBe a[MessageDoesNotMatchMovement]
            result.errorMessage mustBe "The ARC in the message does not match the ARC in the movement"
          }

          "sending the message as the consignee" in {
            when(ie837.submitter).thenReturn(Consignee)
            when(movement.consigneeId).thenReturn(Some("123"))
            when(ie837.consigneeId).thenReturn(Some("123"))

            when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
            when(ie837.administrativeReferenceCode).thenReturn(Seq(Some("ARC2")))

            val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837).left.value
            result mustBe a[MessageDoesNotMatchMovement]
            result.errorMessage mustBe "The ARC in the message does not match the ARC in the movement"
          }
        }
      }

      "return consignor does not match" when {
        "the message consignor does not match the movement consignor" in {
          when(ie837.submitter).thenReturn(Consignor)
          when(movement.consignorId).thenReturn("123")
          when(ie837.consignorId).thenReturn(Some("456"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The Consignor in the message does not match the Consignor in the movement"
        }
      }

      "return consignee does not match" when {
        "the message consignee does not match the movement consignee" in {
          when(ie837.submitter).thenReturn(Consignee)
          when(movement.consigneeId).thenReturn(Some("123"))
          when(ie837.consigneeId).thenReturn(Some("456"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The Consignee in the message does not match the Consignee in the movement"
        }
      }

      "return consignor unauthorised" when {
        "the message consignor id is unauthorised" in {
          when(ie837.submitter).thenReturn(Consignor)
          when(ie837.consignorId).thenReturn(Some("124"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837).left.value
          result mustBe a[MessageIdentifierIsUnauthorised]
          result.errorMessage mustBe "The Consignor is not authorised to submit this message for the movement"
        }
      }

      "return consignee unauthorised" when {
        "the message consignee id is unauthorised" in {
          when(ie837.submitter).thenReturn(Consignee)
          when(ie837.consigneeId).thenReturn(Some("124"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837).left.value
          result mustBe a[MessageIdentifierIsUnauthorised]
          result.errorMessage mustBe "The Consignee is not authorised to submit this message for the movement"
        }
      }

      "return consignor is missing" when {
        "using consignor and the message consignor id field is empty" in {
          when(ie837.submitter).thenReturn(Consignor)
          when(ie837.consignorId).thenReturn(None)

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837).left.value
          result mustBe a[MessageMissingKeyInformation]
          result.errorMessage mustBe "The Consignor in the message should not be empty"
        }
      }

      "return consignee is missing" when {
        "using consignee and the message consignee id field is empty" in {
          when(ie837.submitter).thenReturn(Consignee)
          when(ie837.consigneeId).thenReturn(None)

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie837).left.value
          result mustBe a[MessageMissingKeyInformation]
          result.errorMessage mustBe "The Consignee in the message should not be empty"
        }
      }

    }

    "an IE871 message" should {
      val ie871 = mock[IE871Message]

      "return movement consignor id" when {
        "the consignor is being used and the consignor id is authorised and message consignor matches the movement consignor and message ARC matches movement ARC" in {

          when(ie871.submitter).thenReturn(Consignor)
          when(movement.consignorId).thenReturn("123")
          when(ie871.consignorId).thenReturn(Some("123"))

          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie871.administrativeReferenceCode).thenReturn(Seq(Some("ARC1")))

          MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871) mustBe Right("123")
        }
      }

      "return movement consignee id" when {
        "the consignee is being used and the consignee id is authorised and message consignee matches the movement consignee and message ARC matches movement ARC" in {

          when(ie871.submitter).thenReturn(Consignee)
          when(movement.consigneeId).thenReturn(Some("123"))
          when(ie871.consigneeId).thenReturn(Some("123"))

          when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
          when(ie871.administrativeReferenceCode).thenReturn(Seq(Some("ARC1")))

          MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871) mustBe Right("123")
        }
      }

      "return ARC does not match" when {
        "the message ARC does not match the movement ARC" when {
          "sending the message as the consignor" in {
            when(ie871.submitter).thenReturn(Consignor)
            when(movement.consignorId).thenReturn("123")
            when(ie871.consignorId).thenReturn(Some("123"))

            when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
            when(ie871.administrativeReferenceCode).thenReturn(Seq(Some("ARC2")))

            val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871).left.value
            result mustBe a[MessageDoesNotMatchMovement]
            result.errorMessage mustBe "The ARC in the message does not match the ARC in the movement"
          }

          "sending the message as the consignee" in {
            when(ie871.submitter).thenReturn(Consignee)
            when(movement.consigneeId).thenReturn(Some("123"))
            when(ie871.consigneeId).thenReturn(Some("123"))

            when(movement.administrativeReferenceCode).thenReturn(Some("ARC1"))
            when(ie871.administrativeReferenceCode).thenReturn(Seq(Some("ARC2")))

            val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871).left.value
            result mustBe a[MessageDoesNotMatchMovement]
            result.errorMessage mustBe "The ARC in the message does not match the ARC in the movement"
          }
        }
      }

      "return consignor does not match" when {
        "the message consignor does not match the movement consignor" in {
          when(ie871.submitter).thenReturn(Consignor)
          when(movement.consignorId).thenReturn("123")
          when(ie871.consignorId).thenReturn(Some("456"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The Consignor in the message does not match the Consignor in the movement"
        }
      }

      "return consignee does not match" when {
        "the message consignee does not match the movement consignee" in {
          when(ie871.submitter).thenReturn(Consignee)
          when(movement.consigneeId).thenReturn(Some("123"))
          when(ie871.consigneeId).thenReturn(Some("456"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871).left.value
          result mustBe a[MessageDoesNotMatchMovement]
          result.errorMessage mustBe "The Consignee in the message does not match the Consignee in the movement"
        }
      }

      "return consignor unauthorised" when {
        "the message consignor id is unauthorised" in {
          when(ie871.submitter).thenReturn(Consignor)
          when(ie871.consignorId).thenReturn(Some("124"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871).left.value
          result mustBe a[MessageIdentifierIsUnauthorised]
          result.errorMessage mustBe "The Consignor is not authorised to submit this message for the movement"
        }
      }

      "return consignee unauthorised" when {
        "the message consignee id is unauthorised" in {
          when(ie871.submitter).thenReturn(Consignee)
          when(ie871.consigneeId).thenReturn(Some("124"))

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871).left.value
          result mustBe a[MessageIdentifierIsUnauthorised]
          result.errorMessage mustBe "The Consignee is not authorised to submit this message for the movement"
        }
      }

      "return consignor is missing" when {
        "using consignor and the message consignor id field is empty" in {
          when(ie871.submitter).thenReturn(Consignor)
          when(ie871.consignorId).thenReturn(None)

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871).left.value
          result mustBe a[MessageMissingKeyInformation]
          result.errorMessage mustBe "The Consignor in the message should not be empty"
        }
      }

      "return consignee is missing" when {
        "using consignee and the message consignee id field is empty" in {
          when(ie871.submitter).thenReturn(Consignee)
          when(ie871.consigneeId).thenReturn(None)

          val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie871).left.value
          result mustBe a[MessageMissingKeyInformation]
          result.errorMessage mustBe "The Consignee in the message should not be empty"
        }
      }

    }

    "return invalid message type" when {
      "message type is not supported by the SubmitMessage endpoint" in {

        val ie801 = mock[IE801Message]

        when(ie801.messageType).thenReturn("IE801")

        val result = MessageValidation.validateSubmittedMessage(authorisedErns, movement, ie801).left.value
        result mustBe a[MessageTypeInvalid]
        result.errorMessage mustBe "The supplied message type IE801 is not supported"
      }
    }
  }
}
