/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.excisemovementcontrolsystemapi.fixtures

import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.excisemovementcontrolsystemapi.fixture.AuthTestSupport
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.{BoxIdRepository, ErnRetrievalRepository, ErnSubmissionRepository, MovementRepository}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.DateTimeService
import uk.gov.hmrc.mongo.lock.LockRepository

trait ApplicationBuilderSupport extends RepositoryTestStub with AuthTestSupport{

  protected lazy val dateTimeService: DateTimeService = mock[DateTimeService]

  def applicationBuilder(config: Map[String, Any]): GuiceApplicationBuilder = {
    applicationBuilder.configure(config)
  }

  def applicationBuilder: GuiceApplicationBuilder = {

    GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[MovementRepository].to(movementRepository),
        bind[BoxIdRepository].to(boxIdRepository),
        bind[ErnRetrievalRepository].to(ernRetrievalRepository),
        bind[ErnSubmissionRepository].to(ernSubmissionRepository),
        bind[DateTimeService].to(dateTimeService),
        bind[LockRepository].to(lockRepository)
      )
  }

  def application: Application = {
        GuiceApplicationBuilder()
          .configure("metrics.enabled" -> false)
          .build()
  }


}
