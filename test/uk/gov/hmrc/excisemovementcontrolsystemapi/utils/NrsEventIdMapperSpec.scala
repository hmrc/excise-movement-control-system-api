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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.UnsupportedTestMessage
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._

class NrsEventIdMapperSpec extends PlaySpec {

  val sut = new NrsEventIdMapper()

  "mapMessageToEventClient" should {
    "map IE815 to emcs-create-a-movement-ui" in {
      sut.mapMessageToEventId(mock[IE815Message]) mustBe "emcs-create-a-movement-ui"
    }

    "map IE810 to emcs-cancel-a-movement-api" in {
      sut.mapMessageToEventId(mock[IE810Message]) mustBe "emcs-cancel-a-movement-api"
    }

    "map IE813 to mcs-change-a-destination-api" in {
      sut.mapMessageToEventId(mock[IE813Message]) mustBe "mcs-change-a-destination-api"
    }

    "map IE818 to emcs-report-a-receipt-api" in {
      sut.mapMessageToEventId(mock[IE818Message]) mustBe "emcs-report-a-receipt-api"
    }

    "map IE819 to emcs-submit-alert-or-rejection-api" in {
      sut.mapMessageToEventId(mock[IE819Message]) mustBe "emcs-submit-alert-or-rejection-api"
    }

    "map IE837 to emcs-explain-a-delay-api" in {
      sut.mapMessageToEventId(mock[IE837Message]) mustBe "emcs-explain-a-delay-api"
    }

    "map IE871 to emcs-explain-a-shortage-api" in {
      sut.mapMessageToEventId(mock[IE871Message]) mustBe "emcs-explain-a-shortage-api"
    }

    "return an error if cannot match a message to aan event client id" in {

      the[RuntimeException] thrownBy
        sut.mapMessageToEventId(UnsupportedTestMessage) must
        have message "[NrsEventClientMapper] - Unsupported message: any-type"
    }
  }

}
