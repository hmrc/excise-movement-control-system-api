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

import com.mongodb.client.result.InsertOneResult
import dispatch.Future
import org.bson.BsonValue
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.mongodb.scala.{MongoCollection, SingleObservable}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MovementMessageCreatedResult
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.MovementMessage
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import scala.concurrent.ExecutionContext

class MovementMessageRepositorySpec extends PlaySpec {

  protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val mockMongoComponent = mock[MongoComponent]

  private val movementMessage = MovementMessage("123", "ABC", "123AV")

  "saveMovementMessage" should {
    "return CreateMovementMessageResult" in {
      //TODO: implement this
    }
  }
}
