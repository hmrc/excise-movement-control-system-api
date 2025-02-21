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

package uk.gov.hmrc.excisemovementcontrolsystemapi.conf

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.{DAYS, Duration}

class AppConfigSpec extends PlaySpec {

  private val validAppConfig =
    """
      |appName=excise-movement-control-system-api
      |mongodb.uri="mongodb://localhost:27017/plastic-packaging-tax-returns"
      |mongodb.movement.TTL=10days
      |microservice.services.nrs.api-key="test-key"
      |microservice.services.push-pull-notifications.host="notification"
      |microservice.services.push-pull-notifications.port="1111"
      |featureFlags.pushNotificationsEnabled=false
      |featureFlags.subscribeErnsEnabled=true
      |featureFlags.etdsPreValidateTraderEnabled=false
      |featureFlags.nrsNewEnabled=false
      |featureFlags.newAuditingEnabled=true
    """.stripMargin

  private def createAppConfig = {
    val config        = ConfigFactory.parseString(validAppConfig)
    val configuration = Configuration(config)
    new AppConfig(configuration, new ServicesConfig(configuration))
  }

  val configService: AppConfig = createAppConfig

  "AppConfig" should {
    "return config for TTL for Movement Mongo collection" in {
      configService.movementTTL mustBe Duration.create(10, DAYS)
    }

    "return config forApi Key" in {
      configService.nrsApiKey mustBe "test-key"
    }

    "return pushNotificationUrl" in {
      configService.pushPullNotificationsUri("boxid") mustBe "http://notification:1111/box/boxid/notifications"
    }

    "return feature flag for Push Pull Notifications" in {
      configService.pushNotificationsEnabled mustBe false
    }

    "return feature flag for ETDS PreValidateTrader" in {
      configService.etdsPreValidateTraderEnabled mustBe false
    }

    "return feature flag for new auditing" in {
      configService.newAuditingEnabled mustBe true
    }
  }
}
