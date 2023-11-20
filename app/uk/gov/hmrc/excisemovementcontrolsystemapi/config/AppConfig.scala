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

import java.time.{Duration => JavaDuration}
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DAYS, Duration, FiniteDuration, SECONDS}
import java.util.concurrent.TimeUnit.{MINUTES, SECONDS}

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")

  lazy val eisHost: String = servicesConfig.baseUrl("eis")
  lazy val systemApplication: String = config.get[String]("system.application")
  lazy val interval: FiniteDuration = config.getOptional[String]("scheduler.pollingNewMessageJob.interval")
    .map(Duration.create(_).asInstanceOf[FiniteDuration])
    .getOrElse(FiniteDuration(5, MINUTES))

  lazy  val initialDelay: FiniteDuration = config.getOptional[String]("scheduler.pollingNewMessageJob.initialDelay")
    .map(Duration.create(_).asInstanceOf[FiniteDuration])
    .getOrElse(FiniteDuration(60, SECONDS))

  lazy val retryAfterMinutes: JavaDuration = config.getOptional[Long]("scheduler.queue.retryAfterMinutes")
    .fold(JavaDuration.ofMinutes(5L))(JavaDuration.ofMinutes)

  lazy val maxRetryAttempts: Int = config.getOptional[Int]("scheduler.queue.retryAttempt").getOrElse(3)

  lazy val runSubmissionWorkItemAfter: FiniteDuration = config.getOptional[String]("scheduler.submissionWorkItems.runWorkItemAfter")
    .map(Duration.create(_).asInstanceOf[FiniteDuration])
    .getOrElse(FiniteDuration(5, MINUTES))

  def getMovementTTL: Duration = config.getOptional[String]("mongodb.movement.TTL")
    .fold(Duration.create(30, DAYS))(Duration.create(_).asInstanceOf[FiniteDuration])

  def emcsReceiverMessageUrl: String = s"$eisHost/emcs/digital-submit-new-message/v1"
  def showNewMessageUrl: String = s"$eisHost/apip-emcs/messages/v1/show-new-messages"
  def messageReceiptUrl(ern: String): String =
    s"$eisHost/apip-emcs/messages/v1/message-receipt?exciseregistrationnumber=$ern"

  def traderMovementUrl = s"$eisHost/emcs/movements/v1/trader-movements"

}
