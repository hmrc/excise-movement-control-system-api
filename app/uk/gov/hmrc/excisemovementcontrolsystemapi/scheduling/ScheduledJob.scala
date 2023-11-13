package uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait ScheduledJob {
  def name: String
  def execute(implicit ec: ExecutionContext): Future[Result]
  def isRunning: Future[Boolean]

  case class Result(message: String)

  val enabled: Boolean
  def configKey: String = name

  def initialDelay: FiniteDuration

  def interval: FiniteDuration

  override def toString() = s"$name after $initialDelay every $interval"
}
