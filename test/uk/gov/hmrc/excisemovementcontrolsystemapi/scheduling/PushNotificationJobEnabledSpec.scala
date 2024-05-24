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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.MixedPlaySpec
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.PushNotificationService

class PushNotificationJobEnabledSpec extends MixedPlaySpec {

  private val movementRepository = mock[MovementRepository]
  private val pushNotificationService = mock[PushNotificationService]

  "push notifications job" when {
    def setupApp(pushNotificationsEnabled: Boolean): Application = {
      GuiceApplicationBuilder()
        .overrides(
          bind[MovementRepository].toInstance(movementRepository),
          bind[PushNotificationService].toInstance(pushNotificationService),
        )
        .configure(
          "featureFlags.pushNotificationsEnabled" -> pushNotificationsEnabled,
        ).build()
    }

    "initialised" should {
      "be enabled if push notifications is enabled" in new App(setupApp(pushNotificationsEnabled = true)) {
        app.injector.instanceOf[PushNotificationJob].enabled mustBe true
      }
      "be disabled if push notifications is disabled" in new App(setupApp(pushNotificationsEnabled = false)) {
        app.injector.instanceOf[PushNotificationJob].enabled mustBe false
      }
    }
  }
}
