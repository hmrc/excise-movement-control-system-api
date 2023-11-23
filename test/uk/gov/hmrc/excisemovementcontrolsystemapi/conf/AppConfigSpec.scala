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

import java.time.{Duration => JavaDuration}
import scala.concurrent.duration.{DAYS, Duration, MINUTES}

class AppConfigSpec extends PlaySpec {

  private val validAppConfig =
    """
      |appName=excise-movement-control-system-api
      |mongodb.uri="mongodb://localhost:27017/plastic-packaging-tax-returns"
      |mongodb.movement.TTL=10days
      |scheduler.pollingNewMessageJob.interval=4 minutes
      |scheduler.pollingNewMessageJob.initialDelay=4 minutes
      |scheduler.workItems.inProgressTimeOut=4 minutes
      |scheduler.workItems.failureRetryAttempts=2
      |scheduler.workItems.fastInterval=3 minutes
      |scheduler.workItems.slowInterval=32 minutes
      |mongodb.workItem.TTL = 10 days
    """.stripMargin

  private def createAppConfig = {
    val config = ConfigFactory.parseString(validAppConfig)
    val configuration = Configuration(config)
    new AppConfig(configuration, new ServicesConfig(configuration))
  }

  val configService: AppConfig = createAppConfig

  "AppConfig" should {
    "return config for TTL for Movement Mongo collection" in {
      configService.movementTTL mustBe Duration.create(10, DAYS)
    }

    "return config for TTL for Work Item Mongo collection" in {
      configService.workItemTTL mustBe Duration.create(10, DAYS)
    }

    "return config for PollingNewMessageJob interval" in {
      configService.interval mustBe Duration.create(4, MINUTES)
    }

    "return config for PollingNewMessageJob initialDelay" in {
      configService.initialDelay mustBe Duration.create(4, MINUTES)
    }

    "return config for the Work Item in progress time out" in {
      configService.workItemInProgressTimeOut mustBe Duration.create(4, MINUTES)
    }

    "return config for the Work Item failure retry attempts" in {
      configService.maxFailureRetryAttempts mustBe 2
    }

    "return config for Work Item fast interval" in {
      configService.workItemFastInterval mustBe Duration.create(3, MINUTES)
    }

    "return config for Work Item slow interval" in {
      configService.workItemSlowInterval mustBe Duration.create(32, MINUTES)
    }
  }
}
