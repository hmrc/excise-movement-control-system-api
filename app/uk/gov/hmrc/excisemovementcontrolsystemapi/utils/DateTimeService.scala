package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}

@Singleton
class DateTimeService @Inject()(clock: Clock) {

  def now: Instant = Instant.now(clock)
}
