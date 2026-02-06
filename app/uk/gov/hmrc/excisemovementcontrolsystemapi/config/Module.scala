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

package uk.gov.hmrc.excisemovementcontrolsystemapi.config

import play.api.inject.Binding
import play.api.{Configuration, Environment}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.{NrsCircuitBreakerProvider, TraderMovementConnector, TraderMovementConnectorV1, TraderMovementConnectorV2}
import uk.gov.hmrc.excisemovementcontrolsystemapi.connectors.NrsConnector.NrsCircuitBreaker
import uk.gov.hmrc.excisemovementcontrolsystemapi.controllers.{DraftExciseMovementController, DraftExciseMovementControllerV1, DraftExciseMovementControllerV2}
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.{IEMessageFactory, IEMessageFactoryV1, IEMessageFactoryV2}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.auditing.{AuditEventFactory, AuditEventFactoryV2}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.validation.{MessageValidation, MessageValidationV1, MessageValidationV2}
import uk.gov.hmrc.excisemovementcontrolsystemapi.scheduling.TransformJob
import uk.gov.hmrc.excisemovementcontrolsystemapi.services.{AuditService, AuditServiceV1, AuditServiceV2, MessageService, MessageServiceV1, MessageServiceV2}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{NrsEventIdMapper, NrsEventIdMapperV1, NrsEventIdMapperV2}
import uk.gov.hmrc.mongo.metrix.MetricOrchestrator

import java.time.Clock

class Module extends play.api.inject.Module {

  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] = {
    val latestSpec          = configuration.get[Boolean]("featureFlags.latestFunctionalSpecEnabled")
    val transformJobEnabled = configuration.get[Boolean]("featureFlags.transformJobEnabled")
    val transformJob        = if (transformJobEnabled) Seq(bind[TransformJob].toSelf.eagerly()) else Seq()
    val versionBindings     =
      if (latestSpec)
        Seq(
          bind[DraftExciseMovementController].to[DraftExciseMovementControllerV2],
          bind[MessageService].to[MessageServiceV2],
          bind[IEMessageFactory].to[IEMessageFactoryV2],
          bind[TraderMovementConnector].to[TraderMovementConnectorV2],
          bind[AuditEventFactory].to[AuditEventFactoryV2],
          bind[NrsEventIdMapper].to[NrsEventIdMapperV2],
          bind[AuditService].to[AuditServiceV2],
          bind[MessageValidation].to[MessageValidationV2]
        )
      else
        Seq(
          bind[DraftExciseMovementController].to[DraftExciseMovementControllerV1],
          bind[MessageService].to[MessageServiceV1],
          bind[IEMessageFactory].to[IEMessageFactoryV1],
          bind[TraderMovementConnector].to[TraderMovementConnectorV1],
          bind[AuditEventFactory].to[AuditEventFactoryV2],
          bind[NrsEventIdMapper].to[NrsEventIdMapperV1],
          bind[AuditService].to[AuditServiceV1],
          bind[MessageValidation].to[MessageValidationV1]
        )

    Seq(
      bind[AppConfig].toSelf.eagerly(),
      bind[JobScheduler].toSelf.eagerly(),
      bind[Clock].toInstance(Clock.systemUTC()),
      bind[MetricOrchestrator].toProvider[MetricsProvider],
      bind[NrsCircuitBreaker].toProvider[NrsCircuitBreakerProvider]
    ) ++ versionBindings ++ transformJob
  }
}
