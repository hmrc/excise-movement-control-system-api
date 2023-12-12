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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers

import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions.{AuthAction, ParseXmlAction, ValidateErnInMessageAction, ValidateLRNAction}
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{SubmissionMessageService, WorkItemService}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.ErnsMapper
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class SubmitMessageController @Inject()(
                                         authAction: AuthAction,
                                         xmlParser: ParseXmlAction,
                                         validateErnInMessageAction: ValidateErnInMessageAction,
                                         validateLRNAction: ValidateLRNAction,
                                         submissionMessageService: SubmissionMessageService,
                                         workItemService: WorkItemService,
                                         ernsMapper: ErnsMapper,
                                         cc: ControllerComponents
                                       )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def submit(lrn: String): Action[NodeSeq] = {

    (authAction
      andThen xmlParser
      andThen validateErnInMessageAction
      andThen validateLRNAction(lrn)).async(parse.xml) {
      implicit request =>
        val ern = ernsMapper.getSingleErnFromMessage(request.message, request.validErns)

        workItemService.addWorkItemForErn(ern, fastMode = true)

        submissionMessageService.submit(request).flatMap {
          case Right(_) => Future.successful(Accepted(""))
          case Left(error) => Future.successful(error)
        }

    }

  }

}
