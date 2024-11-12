package uk.gov.hmrc.excisemovementcontrolsystemapi.connectors

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreaker
import play.api.{Configuration, Logging}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.NrsCircuitBreaker

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class NrsCircuitBreakerProvider @Inject()(
                                            configuration: Configuration,
                                            system: ActorSystem
                                          )(implicit ec: ExecutionContext) extends Provider[NrsCircuitBreaker] with Logging {

  private val maxFailures: Int = configuration.get[Int]("nrs.max-failures")
  private val callTimeout: FiniteDuration = configuration.get[FiniteDuration]("nrs.call-timeout")
  private val resetTimeout: FiniteDuration = configuration.get[FiniteDuration]("nrs.reset-timeout")

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
