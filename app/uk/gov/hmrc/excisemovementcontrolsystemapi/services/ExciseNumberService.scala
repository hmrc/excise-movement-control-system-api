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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import com.google.inject.Singleton
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.{ErrorResponse, MongoError, NotFoundError}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{ExciseNumberRepository, MovementMessageRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{ExciseNumber, Message, Movement}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExciseNumberService @Inject()(
    exciseNumberRepository: ExciseNumberRepository
)(implicit ec: ExecutionContext) {

  def saveExciseNumber(exciseNumber: ExciseNumber): Future[Either[MongoError, ExciseNumber]] = {
    exciseNumberRepository.save(exciseNumber)

      .map(_ => Right(exciseNumber))
      .recover {
        case ex: Throwable => Left(MongoError(ex.getMessage))
      }
  }

}
