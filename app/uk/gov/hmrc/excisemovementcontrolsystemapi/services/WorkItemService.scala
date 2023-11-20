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
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.EmcsUtils
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.ExciseNumberWorkItem
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemRepository}

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.duration.{DurationInt, MINUTES}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class WorkItemService @Inject()
(
  workItemRepository: WorkItemRepository[ExciseNumberWorkItem],
  timestampService: TimestampSupport
)(implicit ec: ExecutionContext) {

  def createWorkItem(ern: String): Future[WorkItem[ExciseNumberWorkItem]] = {
    //TODO replace with app config
    workItemRepository.pushNew(ExciseNumberWorkItem(ern),timestampService.timestamp().plusSeconds(5 * 60))
  }

}
