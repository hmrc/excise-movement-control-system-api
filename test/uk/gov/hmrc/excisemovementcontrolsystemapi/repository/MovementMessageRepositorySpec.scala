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

package uk.gov.hmrc.excisemovementcontrolsystemapi.repository

import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.MovementMessage
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class MovementMessageRepositorySpec extends PlaySpec
  with DefaultPlayMongoRepositorySupport[MovementMessage]
  with IntegrationPatience
  with OptionValues {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val appConfig = mock[AppConfig]
  protected override val repository = new MovementMessageRepository(mongoComponent, appConfig)

  "saveMovementMessage" should {
    "return insert a movement message" in {
      val repository = new MovementMessageRepository(mongoComponent, appConfig)

      val result = repository.saveMovementMessage(MovementMessage("123", "345", Some("789"), None)).futureValue
      val insertedRecord = find(
        Filters.and(
          Filters.equal("consignorId", "345"),
          Filters.equal("localReferenceNumber", "123")
        )
      ).futureValue
        .headOption
        .value

      result mustEqual true
      insertedRecord.localReferenceNumber mustEqual "123"
      insertedRecord.consignorId mustEqual "345"
      insertedRecord.consigneeId mustEqual Some("789")
      insertedRecord.administrativeReferenceCode mustEqual None
    }


  }
}
