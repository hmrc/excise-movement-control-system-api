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

package uk.gov.hmrc.excisemovementcontrolsystemapi.utils

import com.google.inject.{ImplementedBy, Singleton}

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}

@ImplementedBy(classOf[DateTimeServiceImpl])
trait DateTimeService {
  def timestamp(): Instant
}

@Singleton
class DateTimeServiceImpl extends DateTimeService {
  override def timestamp(): Instant = Instant.now()
}

object DateTimeService {
  implicit class DateTimeFormat(val dateTime: Instant) extends AnyVal {

    implicit def asStringInMilliseconds: String =
      ZonedDateTime
        .ofInstant(dateTime, ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"))
  }

//  def timestamp(): Instant = new DateTimeServiceImpl().timestamp()
}
