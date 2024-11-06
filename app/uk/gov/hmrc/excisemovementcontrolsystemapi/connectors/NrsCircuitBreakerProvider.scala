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

package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreaker
import play.api.{Configuration, Logging}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.NrsCircuitBreaker

import javax.inject.{Inject, Provider}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class NrsCircuitBreakerProvider @Inject() (
  configuration: Configuration,
  system: ActorSystem
)(implicit ec: ExecutionContext)
    extends Provider[NrsCircuitBreaker]
    with Logging {

  private val maxFailures: Int             = configuration.get[Int]("microservice.services.nrs.max-failures")
  private val callTimeout: FiniteDuration  = configuration.get[FiniteDuration]("microservice.services.nrs.call-timeout")
  private val resetTimeout: FiniteDuration =
    configuration.get[FiniteDuration]("microservice.services.nrs.reset-timeout")

  private val breaker: CircuitBreaker =
    new CircuitBreaker(
      scheduler = system.scheduler,
      maxFailures = maxFailures,
      callTimeout = callTimeout,
      resetTimeout = resetTimeout
    )
      .onOpen {
        logger.warn("NRS Circuit Breaker has opened")
      }
      .onClose {
        logger.info("NRS Circuit Breaker has closed")
      }

  override def get(): NrsCircuitBreaker = NrsCircuitBreaker(breaker)

}
