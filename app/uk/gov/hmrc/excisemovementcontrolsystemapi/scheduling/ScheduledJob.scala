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

package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait ScheduledJob {
  def name: String

  def execute(implicit ec: ExecutionContext): Future[Result]

  def isRunning: Future[Boolean]

<<<<<<<< HEAD:app/uk/gov/hmrc/excisemovementcontrolsystemapi/scheduling/ScheduledJob.scala
  case class Result(message: String)

  val enabled: Boolean

  def configKey: String = name

  def initialDelay: FiniteDuration

  def intervalBetweenJobRunning: FiniteDuration

  override def toString() = s"$name after $initialDelay every $intervalBetweenJobRunning"
}
========

>>>>>>>> d81b057 (Emcs message polling (#42)):app/uk/gov/hmrc/excisemovementcontrolsystemapi/models/MongoError.scala
