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

package uk.gov.hmrc.excisemovementcontrolsystemapi.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DAYS, Duration, FiniteDuration, HOURS, MINUTES, SECONDS}

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")

  lazy val eisHost: String = servicesConfig.baseUrl("eis")
  lazy val systemApplication: String = config.get[String]("system.application")

  lazy val interval: FiniteDuration = config.getOptional[String]("scheduler.pollingNewMessageJob.interval")
    .map(Duration.create(_).asInstanceOf[FiniteDuration])
    .getOrElse(FiniteDuration(1, MINUTES))

  lazy val initialDelay: FiniteDuration = config.getOptional[String]("scheduler.pollingNewMessageJob.initialDelay")
    .map(Duration.create(_).asInstanceOf[FiniteDuration])
    .getOrElse(FiniteDuration(60, SECONDS))

  lazy val workItemInProgressTimeOut: FiniteDuration = config.getOptional[String]("scheduler.workItems.inProgressTimeOut")
    .map(Duration.create(_).asInstanceOf[FiniteDuration])
    .getOrElse(FiniteDuration(5, MINUTES))

  // When a work item fails, this is how many times to retry it before permanently failing
  lazy val maxFailureRetryAttempts: Int = config.getOptional[Int]("scheduler.workItems.failureRetryAttempts").getOrElse(3)

  //How long after a failure should we wait before retrying it?
  // (It will only be retried if it was BOTH last updated before Now-failureRetryAfter and availableAt before Now)
  lazy val failureRetryAfter: FiniteDuration = config.getOptional[String]("scheduler.workItems.failureRetryAfter")
    .map(Duration.create(_).asInstanceOf[FiniteDuration])
    .getOrElse(FiniteDuration(5, MINUTES))


  // When on the fast interval, how many times to fast poll before switching to slow poll
  lazy val fastIntervalRetryAttempts: Int = config.getOptional[Int]("scheduler.workItems.fastIntervalRetryAttempts").getOrElse(3)

  // The fast interval is used after submitting a message
  lazy val workItemFastInterval: FiniteDuration = config.getOptional[String]("scheduler.workItems.fastInterval")
    .map(Duration.create(_).asInstanceOf[FiniteDuration])
    .getOrElse(FiniteDuration(5, MINUTES))

  // The slow interval is used once fastInterval has been used for fastIntervalRetryAttempts times
  lazy val workItemSlowInterval: FiniteDuration = config.getOptional[String]("scheduler.workItems.slowInterval")
    .map(Duration.create(_).asInstanceOf[FiniteDuration])
    .getOrElse(FiniteDuration(1, HOURS))

  lazy val movementTTL: Duration = config.getOptional[String]("mongodb.movement.TTL")
    .fold(Duration.create(30, DAYS))(Duration.create(_).asInstanceOf[FiniteDuration])

  lazy val workItemTTL: Duration = config.getOptional[String]("mongodb.workItem.TTL")
    .fold(Duration.create(30, DAYS))(Duration.create(_).asInstanceOf[FiniteDuration])

  def emcsReceiverMessageUrl: String = s"$eisHost/emcs/digital-submit-new-message/v1"

  def showNewMessageUrl: String = s"$eisHost/apip-emcs/messages/v1/show-new-messages"

  def messageReceiptUrl(ern: String): String =
    s"$eisHost/apip-emcs/messages/v1/message-receipt?exciseregistrationnumber=$ern"

  def traderMovementUrl = s"$eisHost/emcs/movements/v1/trader-movements"

}
