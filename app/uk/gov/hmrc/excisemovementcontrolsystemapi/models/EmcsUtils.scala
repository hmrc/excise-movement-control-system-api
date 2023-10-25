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

package uk.gov.hmrc.excisemovementcontrolsystemapi.models


import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.{Base64, UUID}

class EmcsUtils {

  def getCurrentDateTime: LocalDateTime = LocalDateTime.now()
  def getCurrentDateTimeString: String = getCurrentDateTime.toString
  def generateCorrelationId: String = UUID.randomUUID().toString
  def createEncoder: Base64.Encoder = Base64.getEncoder

  def encode(str: String): String = {
    Base64.getEncoder.encodeToString(str.getBytes(StandardCharsets.UTF_8))
  }

  def decode(str: String): String = {
    Base64.getDecoder.decode(str).map(_.toChar).mkString
  }
}
