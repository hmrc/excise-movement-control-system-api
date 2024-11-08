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
import scala.concurrent.duration.{DAYS, Duration, FiniteDuration}
import java.time.{Duration => JavaDuration}

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  lazy val appName: String = config.get[String]("appName")

  lazy val eisHost: String                   = servicesConfig.baseUrl("eis")
  lazy val nrsHost: String                   = servicesConfig.baseUrl("nrs")
  lazy val pushPullNotificationsHost: String = servicesConfig.baseUrl("push-pull-notifications")

  lazy val nrsApiKey: String                   = servicesConfig.getConfString("nrs.api-key", "dummyNrsApiKey")
  lazy val nrsRetryDelays: Seq[FiniteDuration] =
    config.get[Seq[FiniteDuration]]("microservice.services.nrs.retryDelays")

  lazy val movementTTL: Duration = config
    .getOptional[String]("mongodb.movement.TTL")
    .fold(Duration.create(30, DAYS))(Duration.create(_).asInstanceOf[FiniteDuration])

  lazy val movementArchiveTTL: Duration = config
    .getOptional[String]("mongodb.movementArchive.TTL")
    .fold(Duration.create(40, DAYS))(Duration.create(_).asInstanceOf[FiniteDuration])

  lazy val miscodedMovementArchiveTTL: Duration = config
    .getOptional[String]("mongodb.miscodedMovementArchive.TTL")
    .fold(Duration.create(40, DAYS))(Duration.create(_).asInstanceOf[FiniteDuration])

  lazy val ernRetrievalTTL: Duration = config
    .getOptional[String]("mongodb.ernRetrieval.TTL")
    .fold(Duration.create(30, DAYS))(Duration.create(_).asInstanceOf[FiniteDuration])

  lazy val nrsWorkItemRepoTTL: Duration = config
    .getOptional[String]("mongodb.nrsSubmission.TTL")
    .fold(Duration.create(30, DAYS))(Duration.create(_).asInstanceOf[FiniteDuration])

  lazy val nrsRetryAfter: JavaDuration = config
    .getOptional[Long]("microservice.services.nrs.retryAfterMinutes")
    .map(JavaDuration.ofMinutes)
    .getOrElse(JavaDuration.ofMinutes(10))

  lazy val pushNotificationsEnabled: Boolean = servicesConfig.getBoolean("featureFlags.pushNotificationsEnabled")

  lazy val subscribeErnsEnabled: Boolean =
    config.getOptional[Boolean]("featureFlags.subscribeErnsEnabled").getOrElse(false)

  val etdsPreValidateTraderEnabled: Boolean =
    servicesConfig.getBoolean("featureFlags.etdsPreValidateTraderEnabled")

  def emcsReceiverMessageUrl: String = s"$eisHost/emcs/digital-submit-new-message/v1"
  def submissionBearerToken: String  =
    servicesConfig.getConfString("eis.submission-bearer-token", "dummySubmissionBearerToken")

  def getNrsSubmissionUrl: String = s"$nrsHost/submission"

  def preValidateTraderUrl: String         = s"$eisHost/emcs/pre-validate-trader/v1"
  def preValidateTraderBearerToken: String =
    servicesConfig.getConfString("eis.pre-validate-trader-bearer-token", "dummyPreValidateTraderBearerToken")

  def preValidateTraderETDSUrl: String         = s"$eisHost/etds/traderprevalidation/v1"
  def preValidateTraderETDSBearerToken: String =
    servicesConfig.getConfString("eis.pre-validate-trader-etds-bearer-token", "dummyPreValidateTraderETDSBearerToken")

  def pushPullNotificationsUri(boxId: String) =
    s"$pushPullNotificationsHost/box/$boxId/notifications"

}
