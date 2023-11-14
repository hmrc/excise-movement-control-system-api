package uk.gov.hmrc.excisemovementcontrolsystemapi.conf

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.{DAYS, Duration, FiniteDuration, MINUTES}
import java.time.{Duration => JavaDuration}

class AppConfigSpec extends PlaySpec {

  private val validAppConfig =
    """
      |appName=excise-movement-control-system-api
      |mongodb.uri="mongodb://localhost:27017/plastic-packaging-tax-returns"
      |mongodb.movement.TTLInDays=10
      |pollingNewMessageJob.intervalInMinutes=4
      |pollingNewMessageJob.initialDelayInMinutes=4
      |queue.retryAfterMinutes=4
    """.stripMargin

  private def createAppConfig = {
    val config = ConfigFactory.parseString(validAppConfig)
    val configuration = Configuration(config)
    new AppConfig(configuration, new ServicesConfig(configuration))
  }

  val configService: AppConfig = createAppConfig

  "AppConfig" should {
    "return config for TTL for Movement Mongo collection" in{
      configService.getMovementTTLInDays mustBe Duration.create(10, DAYS)
    }

    "return config for PollingNewMessageJob interval" in {
      configService.intervalInMinutes mustBe Duration.create(4, MINUTES)
    }

    "return config for PollingNewMessageJob initialDelay" in {
      configService.initialDelayInMinutes mustBe Duration.create(4, MINUTES)
    }

    "return config for the queue retryAfter" in {
      configService.retryAfterMinutes mustBe JavaDuration.ofMinutes(4)
    }
  }
}
