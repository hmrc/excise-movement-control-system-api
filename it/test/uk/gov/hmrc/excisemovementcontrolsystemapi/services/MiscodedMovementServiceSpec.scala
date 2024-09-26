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

package uk.gov.hmrc.excisemovementcontrolsystemapi.services

import generated.{IE704Type, IE801Type, IE802Type, IE803Type, IE807Type, IE810Type, IE813Type, IE818Type, IE819Type, IE829Type, IE837Type, IE839Type, IE840Type, IE871Type, IE881Type, IE905Type}
import org.mockito.MockitoSugar
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, EitherValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import scalaxb.CanWriteXML
import uk.gov.hmrc.excisemovementcontrolsystemapi.data.{MessageParams, XmlMessageGeneratorFactory}
import uk.gov.hmrc.excisemovementcontrolsystemapi.factories.IEMessageFactory
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.MessageTypes.{IE704, IE801, IE802, IE803, IE807, IE810, IE813, IE818, IE819, IE829, IE837, IE839, IE840, IE871, IE881, IE905}
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages._
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.MovementRepository
import uk.gov.hmrc.excisemovementcontrolsystemapi.repository.model.{Message, Movement}
import uk.gov.hmrc.excisemovementcontrolsystemapi.utils.{DateTimeService, EmcsUtils}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.Instant
import java.time.temporal.ChronoUnit

class MiscodedMovementServiceSpec
  extends PlaySpec
    with CleanMongoCollectionSupport
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite
    with MockitoSugar {

  private val dateTimeService = mock[DateTimeService]
  private val now = Instant.now.truncatedTo(ChronoUnit.MILLIS)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MongoComponent].toInstance(mongoComponent),
      bind[DateTimeService].toInstance(dateTimeService)
    )
    .build()

  private lazy val movementRepository: MovementRepository = app.injector.instanceOf[MovementRepository]
  private lazy val miscodedMovementService = app.injector.instanceOf[MiscodedMovementService]

  private val utils = new EmcsUtils
  private val messageFactory = IEMessageFactory()

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(dateTimeService.timestamp()).thenReturn(now)
  }

  "recodeMessages" should {

    "recode the messages for the given movement" in {

      val lrn = "lrn"
      val arc = "arc"

      val consignor = "consignor"
      val consignee = "consignee"

      val badIe704 = formatXmlIncorrectly[IE704Type, IE704Message](consignor, MessageParams(IE704, "XI000000", consigneeErn = Some(consignee), localReferenceNumber = Some(lrn), administrativeReferenceCode = None))
      val goodIe704 = formatXmlCorrectly[IE704Type, IE704Message](consignor, MessageParams(IE704, "XI000000", consigneeErn = Some(consignee), localReferenceNumber = Some(lrn), administrativeReferenceCode = None), "http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3")

      val badIe801 = formatXmlIncorrectly[IE801Type, IE801Message](consignor, MessageParams(IE801, "XI000001", consigneeErn = Some(consignee), localReferenceNumber = Some(lrn), administrativeReferenceCode = Some(arc)))
      val goodIe801 = formatXmlCorrectly[IE801Type, IE801Message](consignor, MessageParams(IE801, "XI000001", consigneeErn = Some(consignee), localReferenceNumber = Some(lrn), administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE801:V3.13")

      val badIe802 = formatXmlIncorrectly[IE802Type, IE802Message](consignor, MessageParams(IE802, "XI000002", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe802 = formatXmlCorrectly[IE802Type, IE802Message](consignor, MessageParams(IE802, "XI000002", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE802:V3.13")

      val badIe803 = formatXmlIncorrectly[IE803Type, IE803Message](consignor, MessageParams(IE803, "XI000003", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe803 = formatXmlCorrectly[IE803Type, IE803Message](consignor, MessageParams(IE803, "XI000003", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE803:V3.13")

      val badIe807 = formatXmlIncorrectly[IE807Type, IE807Message](consignor, MessageParams(IE807, "XI000004", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe807 = formatXmlCorrectly[IE807Type, IE807Message](consignor, MessageParams(IE807, "XI000004", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE807:V3.13")

      val badIe810 = formatXmlIncorrectly[IE810Type, IE810Message](consignor, MessageParams(IE810, "XI000005", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe810 = formatXmlCorrectly[IE810Type, IE810Message](consignor, MessageParams(IE810, "XI000005", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE810:V3.13")

      val badIe813 = formatXmlIncorrectly[IE813Type, IE813Message](consignor, MessageParams(IE813, "XI000006", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe813 = formatXmlCorrectly[IE813Type, IE813Message](consignor, MessageParams(IE813, "XI000006", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE813:V3.13")

      val badIe818 = formatXmlIncorrectly[IE818Type, IE818Message](consignor, MessageParams(IE818, "XI000008", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe818 = formatXmlCorrectly[IE818Type, IE818Message](consignor, MessageParams(IE818, "XI000008", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE818:V3.13")

      val badIe819 = formatXmlIncorrectly[IE819Type, IE819Message](consignor, MessageParams(IE819, "XI000009", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe819 = formatXmlCorrectly[IE819Type, IE819Message](consignor, MessageParams(IE819, "XI000009", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE819:V3.13")

      val badIe829 = formatXmlIncorrectly[IE829Type, IE829Message](consignor, MessageParams(IE829, "XI000010", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe829 = formatXmlCorrectly[IE829Type, IE829Message](consignor, MessageParams(IE829, "XI000010", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE829:V3.13")

      val badIe837 = formatXmlIncorrectly[IE837Type, IE837Message](consignor, MessageParams(IE837, "XI000011", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe837 = formatXmlCorrectly[IE837Type, IE837Message](consignor, MessageParams(IE837, "XI000011", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE837:V3.13")

      val badIe839 = formatXmlIncorrectly[IE839Type, IE839Message](consignor, MessageParams(IE839, "XI000012", consigneeErn = Some(consignee), localReferenceNumber = Some(lrn), administrativeReferenceCode = Some(arc)))
      val goodIe839 = formatXmlCorrectly[IE839Type, IE839Message](consignor, MessageParams(IE839, "XI000012", consigneeErn = Some(consignee), localReferenceNumber = Some(lrn), administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE839:V3.13")

      val badIe840 = formatXmlIncorrectly[IE840Type, IE840Message](consignor, MessageParams(IE840, "XI000013", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe840 = formatXmlCorrectly[IE840Type, IE840Message](consignor, MessageParams(IE840, "XI000013", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE840:V3.13")

      val badIe871 = formatXmlIncorrectly[IE871Type, IE871Message](consignor, MessageParams(IE871, "XI000014", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe871 = formatXmlCorrectly[IE871Type, IE871Message](consignor, MessageParams(IE871, "XI000014", consigneeErn = Some(consignee), localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE871:V3.13")

      val badIe881 = formatXmlIncorrectly[IE881Type, IE881Message](consignor, MessageParams(IE881, "XI000015", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe881 = formatXmlCorrectly[IE881Type, IE881Message](consignor, MessageParams(IE881, "XI000015", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE881:V3.13")

      val badIe905 = formatXmlIncorrectly[IE905Type, IE905Message](consignor, MessageParams(IE905, "XI000016", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)))
      val goodIe905 = formatXmlCorrectly[IE905Type, IE905Message](consignor, MessageParams(IE905, "XI000016", consigneeErn = None, localReferenceNumber = None, administrativeReferenceCode = Some(arc)), "urn:publicid:-:EC:DGTAXUD:EMCS:PHASE4:IE905:V3.13")

      // Here we're renaming the default namespace for the IE704 to show that if the message element doesn't end in `Type` then we won't replace it
      val ignoredMessage = {
        utils.encode(
          utils.decode(
            formatXmlCorrectly[IE704Type, IE704Message](consignor, MessageParams(IE704, "XI000017", consigneeErn = Some(consignee), localReferenceNumber = Some(lrn), administrativeReferenceCode = None), "http://www.govtalk.gov.uk/taxation/InternationalTrade/Excise/ie704uk/3")
          ).replaceAll("ie704uk:", "foo:").replace("xmlns:ie704uk=", "xmlns:foo=")
        )
      }

      val movement = Movement(
        boxId = None,
        localReferenceNumber = lrn,
        consignorId = consignor,
        consigneeId = Some(consignee),
        administrativeReferenceCode = Some(arc),
        lastUpdated = now.minus(1, ChronoUnit.DAYS),
        messages = Seq(
          Message(badIe704, "IE704", "XI000000", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe801, "IE801", "XI000001", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe802, "IE802", "XI000002", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe803, "IE803", "XI000003", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe807, "IE807", "XI000004", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe810, "IE810", "XI000005", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe813, "IE813", "XI000006", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe818, "IE818", "XI000008", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe819, "IE819", "XI000009", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe829, "IE829", "XI000010", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe837, "IE837", "XI000011", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe839, "IE839", "XI000012", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe840, "IE840", "XI000013", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe871, "IE871", "XI000014", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe881, "IE881", "XI000015", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(badIe905, "IE905", "XI000016", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(ignoredMessage, "IE704", "XI000017", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS))
        )
      )

      val expectedMovement = movement.copy(
        messages = Seq(
          Message(goodIe704, "IE704", "XI000000", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe801, "IE801", "XI000001", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe802, "IE802", "XI000002", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe803, "IE803", "XI000003", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe807, "IE807", "XI000004", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe810, "IE810", "XI000005", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe813, "IE813", "XI000006", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe818, "IE818", "XI000008", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe819, "IE819", "XI000009", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe829, "IE829", "XI000010", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe837, "IE837", "XI000011", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe839, "IE839", "XI000012", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe840, "IE840", "XI000013", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe871, "IE871", "XI000014", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe881, "IE881", "XI000015", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(goodIe905, "IE905", "XI000016", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS)),
          Message(ignoredMessage, "IE704", "XI000017", consignee, Set.empty, now.minus(4, ChronoUnit.DAYS))
        )
      )

      movementRepository.save(movement).futureValue
      miscodedMovementService.recodeMessages(movement._id).futureValue

      val result = movementRepository.getMovementById(movement._id).futureValue.value

      result mustEqual expectedMovement
    }
  }

  private def formatXmlIncorrectly[A, B <: IEMessage with ({ val obj: A })](ern: String, params: MessageParams)(implicit ev: CanWriteXML[A]): String = {
    utils.encode(scalaxb.toXML(messageFactory.createFromXml(
      params.messageType.value,
      XmlMessageGeneratorFactory.generate(ern, params)
    ).asInstanceOf[B].obj, s"${params.messageType.value}Type", generated.defaultScope).toString)
  }

  private def formatXmlCorrectly[A, B <: IEMessage with ({ val obj: A })](ern: String, params: MessageParams, namespace: String)(implicit ev: CanWriteXML[A]): String = {
    utils.encode(scalaxb.toXML(messageFactory.createFromXml(
      params.messageType.value,
      XmlMessageGeneratorFactory.generate(ern, params)
    ).asInstanceOf[B].obj, Some(namespace), Some(params.messageType.value), generated.defaultScope).toString)
  }
}
