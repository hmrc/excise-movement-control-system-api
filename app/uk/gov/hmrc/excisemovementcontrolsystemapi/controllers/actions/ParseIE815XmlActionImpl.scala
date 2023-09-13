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

package uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.actions

import com.google.inject.ImplementedBy
import generated.IE815Type
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auth.{AuthorizedIE815Request, AuthorizedRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ParseIE815XmlActionImpl @Inject()(implicit val executionContext: ExecutionContext) extends ParseIE815XmlAction {

  override def refine[A](request: AuthorizedRequest[A]): Future[Either[Result, AuthorizedIE815Request[A]]] = {
    Future.successful(Right(AuthorizedIE815Request(request, null, "123")))
  }
}

@ImplementedBy(classOf[ParseIE815XmlActionImpl])
trait ParseIE815XmlAction extends ActionRefiner[AuthorizedRequest, AuthorizedIE815Request]
