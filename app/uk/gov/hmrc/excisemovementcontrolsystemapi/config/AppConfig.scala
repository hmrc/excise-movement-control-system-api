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
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DAYS, Duration, FiniteDuration}

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")

  lazy val eisHost: String = servicesConfig.baseUrl("eis")
  lazy val systemApplication: String = config.get[String]("system.application")

  lazy val intervalInMinutes: FiniteDuration = config.getOptional[Long]("pollingNewMessageJob.intervalInMinutes")
    .map(Duration.create(_, MINUTES))
    .getOrElse(FiniteDuration(5, MINUTES))

  lazy val initialDelayInMinutes: FiniteDuration = config.getOptional[Long]("pollingNewMessageJob.initialDelayInMinutes")
    .map(Duration.create(_, MINUTES))
    .getOrElse(Duration.create(5, MINUTES))

  lazy val retryAfterMinutes: JavaDuration = config.getOptional[Long]("queue.retryAfterMinutes")
    .fold(JavaDuration.ofMinutes(5L))(JavaDuration.ofMinutes(_))


  def getMovementTTLInDays: Duration = config.getOptional[Long]("mongodb.movement.TTLInDays")
    .fold(Duration.create(30, DAYS))(Duration.create(_, DAYS))

  def emcsReceiverMessageUrl: String = s"$eisHost/emcs/digital-submit-new-message/v1"
  def showNewMessageUrl: String = s"$eisHost/apip-emcs/messages/v1/show-new-messages"
  def messageReceiptUrl(ern: String): String =
    s"$eisHost/apip-emcs/messages/v1/message-receipt?exciseregistrationnumber=$ern"

  def traderMovementUrl = s"$eisHost/emcs/movements/v1/trader-movements"

}
