/*
 * Copyright 2026 HM Revenue & Customs
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

import generated.v2._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import cats.data.EitherT
import org.xml.sax.{ErrorHandler, SAXParseException}

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import scala.concurrent.Future
import scala.util.control.NonFatal
import cats.implicits._
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.excisemovementcontrolsystemapi.config.AppConfig

import java.util.Base64
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import scala.xml.{Elem, NodeSeq}

@Singleton
class TransformService @Inject() (appConfig: AppConfig)(implicit ec: ExecutionContext) {
  val xsdPathsV1 = Map(
    "IE704" -> "/v1/ie704uk.xsd",
    "IE801" -> "/v1/ie801.xsd",
    "IE802" -> "/v1/ie802.xsd",
    "IE803" -> "/v1/ie803.xsd",
    "IE807" -> "/v1/ie807.xsd",
    "IE810" -> "/v1/ie810.xsd",
    "IE813" -> "/v1/ie813.xsd",
    "IE818" -> "/v1/ie818.xsd",
    "IE819" -> "/v1/ie819.xsd",
    "IE829" -> "/v1/ie829.xsd",
    "IE837" -> "/v1/ie837.xsd",
    "IE839" -> "/v1/ie839.xsd",
    "IE840" -> "/v1/ie840.xsd",
    "IE871" -> "/v1/ie871.xsd",
    "IE881" -> "/v1/ie881.xsd",
    "IE905" -> "/v1/ie905.xsd"
  )

  val xsdPathsV2 = Map(
    "IE704" -> "/v2/ie704uk.xsd",
    "IE801" -> "/v2/ie801.xsd",
    "IE802" -> "/v2/ie802.xsd",
    "IE803" -> "/v2/ie803.xsd",
    "IE807" -> "/v2/ie807.xsd",
    "IE810" -> "/v2/ie810.xsd",
    "IE813" -> "/v2/ie813.xsd",
    "IE818" -> "/v2/ie818.xsd",
    "IE819" -> "/v2/ie819.xsd",
    "IE829" -> "/v2/ie829.xsd",
    "IE837" -> "/v2/ie837.xsd",
    "IE839" -> "/v2/ie839.xsd",
    "IE840" -> "/v2/ie840.xsd",
    "IE871" -> "/v2/ie871.xsd",
    "IE881" -> "/v2/ie881.xsd",
    "IE905" -> "/v2/ie905.xsd"
  )

  private val schemaMap = new ConcurrentHashMap[String, Schema]()

  def transform(
    messageType: String,
    base64EncodedMessage: String
  ): Future[Either[TransformationError, String]] = {
    val result = for {
      decodedMessage        <- decodeBase64(base64EncodedMessage)
      _                     <- if (appConfig.runV1Validation) validateSchema(messageType, decodedMessage, xsdPathsV1, isOldSchema = true)
                               else EitherT.pure[Future, TransformationError](())
      updatedXML            <- rewriteNamespace(decodedMessage)
      messageWithIE801Check <- if (messageType == "IE801") convertImportSadToCustomDeclarationHelper(updatedXML)
                               else EitherT.fromEither[Future](Right(updatedXML))
      messageWithIE829Check <- if (messageType == "IE829")
                                 exportDeclarationTransformation(scala.xml.XML.loadString(messageWithIE801Check))
                               else EitherT.fromEither[Future](Right(messageWithIE801Check))
      _                     <- validateFormat(messageType, messageWithIE829Check)
      _                     <- validateSchema(messageType, messageWithIE829Check, xsdPathsV2, isOldSchema = false)
    } yield messageWithIE829Check

    result.value.map {
      case Left(e)  => Left(e)
      case Right(a) => Right(base64Encode(a))
    }

  }
  private def validateFormat(messageType: String, message: String): EitherT[Future, TransformationError, Unit] =
    EitherT.fromEither {
      try messageType match {
        case "IE704" => scalaxb.fromXML[IE704Type](scala.xml.XML.loadString(message)); Right(())
        case "IE801" => scalaxb.fromXML[IE801Type](scala.xml.XML.loadString(message)); Right(())
        case "IE802" => scalaxb.fromXML[IE802Type](scala.xml.XML.loadString(message)); Right(())
        case "IE803" => scalaxb.fromXML[IE803Type](scala.xml.XML.loadString(message)); Right(())
        case "IE807" => scalaxb.fromXML[IE807Type](scala.xml.XML.loadString(message)); Right(())
        case "IE810" => scalaxb.fromXML[IE810Type](scala.xml.XML.loadString(message)); Right(())
        case "IE813" => scalaxb.fromXML[IE813Type](scala.xml.XML.loadString(message)); Right(())
        case "IE818" => scalaxb.fromXML[IE818Type](scala.xml.XML.loadString(message)); Right(())
        case "IE819" => scalaxb.fromXML[IE819Type](scala.xml.XML.loadString(message)); Right(())
        case "IE829" => scalaxb.fromXML[IE829Type](scala.xml.XML.loadString(message)); Right(())
        case "IE837" => scalaxb.fromXML[IE837Type](scala.xml.XML.loadString(message)); Right(())
        case "IE839" => scalaxb.fromXML[IE839Type](scala.xml.XML.loadString(message)); Right(())
        case "IE840" => scalaxb.fromXML[IE840Type](scala.xml.XML.loadString(message)); Right(())
        case "IE871" => scalaxb.fromXML[IE871Type](scala.xml.XML.loadString(message)); Right(())
        case "IE881" => scalaxb.fromXML[IE881Type](scala.xml.XML.loadString(message)); Right(())
        case "IE905" => scalaxb.fromXML[IE905Type](scala.xml.XML.loadString(message)); Right(())
        case _       => Left(MessageDoesNotExistError)
      } catch {
        case NonFatal(e) => Left(FormatValidationError(e.toString))
      }

    }

  private def validateSchema(
    messageType: String,
    message: String,
    xsdPaths: Map[String, String],
    isOldSchema: Boolean
  ): EitherT[Future, TransformationError, Unit] = {
    var exceptions = List[String]()
    EitherT {
      Future {
        val schema = schemaMap.computeIfAbsent(
          messageType,
          messageType => {
            val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            val url           = getClass.getResource(xsdPaths(messageType))
            val src           = new StreamSource(url.openStream())
            src.setSystemId(url.toExternalForm)
            schemaFactory.newSchema(src)
          }
        )

        val validator = schema.newValidator()
        validator.setErrorHandler(new ErrorHandler() {
          @Override
          def warning(exception: SAXParseException): Unit =
            exceptions = exception.getMessage :: exceptions

          @Override
          def fatalError(exception: SAXParseException): Unit =
            exceptions = exception.getMessage :: exceptions

          @Override
          def error(exception: SAXParseException): Unit =
            exceptions = exception.getMessage :: exceptions
        })
        validator.validate(new StreamSource(new StringReader(message)))

        if (exceptions.nonEmpty) {
          Left(schemaError(isOldSchema, exceptions))

        } else {
          Right(())
        }
      }.recover { case e =>
        Left(schemaError(isOldSchema, exceptions, Some(e.getMessage)))
      }
    }
  }

  private def decodeBase64(base64EncodedMessage: String): EitherT[Future, TransformationError, String] =
    EitherT.fromEither {
      try Right(new String(Base64.getDecoder.decode(base64EncodedMessage), "UTF-8"))
      catch {
        case NonFatal(e) => Left(Base64DecodingError(e.getMessage))
      }
    }

  private def rewriteNamespace(decodedMessage: String): EitherT[Future, TransformationError, String] =
    EitherT.fromEither[Future] {
      try {
        val namespaceRegex = "\"(urn:publicid:-:EC:DGTAXUD:EMCS:[^\"]+?):V3\\.13\"".r
        Right(namespaceRegex.replaceAllIn(decodedMessage, "\"$1:V3.23\""))
      } catch {
        case e: Exception => Left(RewriteNamespaceError(e.toString))
      }

    }

  private def schemaError(isOldSchema: Boolean, exceptions: List[String], errMessage: Option[String] = None) =
    if (isOldSchema) {
      OldSchemaValidationError(errMessage, exceptions = exceptions)
    } else {
      SchemaValidationError(errMessage, exceptions)
    }
  private def convertImportSadToCustomDeclarationHelper(
    updatedXML: String
  ): EitherT[Future, TransformationError, String]                                                            =
    EitherT.fromEither[Future] {
      try Right(convertImportSadToCustomDeclaration(scala.xml.XML.loadString(updatedXML)).toString())
      catch {
        case e: Exception => Left(ImportSadConversionError(e.toString))
      }
    }

  private def convertImportSadToCustomDeclaration(n: scala.xml.Node): scala.xml.Node =
    n match {

      case e: scala.xml.Elem =>
        val newLabel = e.label match {
          case "ImportSad"       => "ImportCustomsDeclaration"
          case "ImportSadNumber" => "ImportCustomsDeclarationNumber"
          case label             => label
        }

        e.copy(label = newLabel, child = e.child.map(convertImportSadToCustomDeclaration))

      case other => other
    }

  private def exportDeclarationTransformation(xml: scala.xml.Node): EitherT[Future, TransformationError, String] =
    EitherT.fromEither[Future] {
      val exportDeclarationElement = xml \\ "ExportDeclarationAcceptanceOrGoodsReleasedForExport"
      if (exportDeclarationElement.map(_.text).toSet.size > 1) Left(ExportDeclarationMultipleDifferentValuesError)
      else {

        exportDeclarationElement.headOption
          .map { exportDeclaration =>
            def removeExportDeclaration(n: scala.xml.Node): NodeSeq = {
              //get export save it append it to
              val res = n match {
                case e: scala.xml.Elem if e.label == "ExportDeclarationAcceptanceOrGoodsReleasedForExport" =>
                  NodeSeq.Empty
                case e: scala.xml.Elem                                                                     => e.copy(child = e.child.flatMap(removeExportDeclaration))
                case other                                                                                 => other
              }
              res
            }

            def appendToExportDeclarationAcceptanceRelease(xml: scala.xml.Node): NodeSeq =
              //keep, delete, put aside - message only
              //catch if head is empty
              xml match {
                case e: scala.xml.Elem if e.label == "ExportDeclarationAcceptanceRelease" =>
                  e.copy(child = e.child ++ exportDeclaration)
                case e: Elem                                                              =>
                  e.copy(child = e.child.flatMap(appendToExportDeclarationAcceptanceRelease))
                case other                                                                => other

              }

            try Right(appendToExportDeclarationAcceptanceRelease(removeExportDeclaration(xml).head).toString())
            catch {
              case e: Exception => Left(ExportDeclarationTransformError(e.toString))
            }
          }
          .getOrElse(
            Left(
              ExportDeclarationNotFoundTransformError(
                "Could not locate ExportDeclarationAcceptanceOrGoodsReleasedForExport in xml"
              )
            )
          )
      }
    }

  private def base64Encode(string: String): String =
    Base64.getEncoder.encodeToString(string.getBytes(StandardCharsets.UTF_8))
}

sealed trait TransformationError

case class Base64DecodingError(error: String) extends TransformationError
case class FormatValidationError(error: String) extends TransformationError
case class SchemaValidationError(error: Option[String] = None, exceptions: List[String]) extends TransformationError
case class OldSchemaValidationError(error: Option[String] = None, exceptions: List[String]) extends TransformationError
case object MessageDoesNotExistError extends TransformationError

case class RewriteNamespaceError(error: String) extends TransformationError
case class ImportSadConversionError(error: String) extends TransformationError

case class ExportDeclarationNotFoundTransformError(error: String) extends TransformationError
case class ExportDeclarationTransformError(error: String) extends TransformationError
case object ExportDeclarationMultipleDifferentValuesError extends TransformationError

object TransformationError {
  implicit lazy val transformError: OFormat[TransformationError]                                              = Json.format
  implicit lazy val base64DecodingError: OFormat[Base64DecodingError]                                         = Json.format
  implicit lazy val oldSchemaValidationError: OFormat[OldSchemaValidationError]                               = Json.format
  implicit lazy val formatValidationError: OFormat[FormatValidationError]                                     = Json.format
  implicit lazy val schemaValidationError: OFormat[SchemaValidationError]                                     = Json.format
  implicit lazy val messageDoesNotExistError: OFormat[MessageDoesNotExistError.type]                          = Json.format
  implicit lazy val rewriteNamespaceError: OFormat[RewriteNamespaceError]                                     = Json.format
  implicit lazy val importSadConversionError: OFormat[ImportSadConversionError]                               = Json.format
  implicit lazy val exportDeclarationNotFoundTransformError: OFormat[ExportDeclarationNotFoundTransformError] =
    Json.format
  implicit lazy val exportDeclarationTransformError: OFormat[ExportDeclarationTransformError]                 = Json.format
  implicit lazy val exportDeclarationMultipleDifferentValuesError
    : OFormat[ExportDeclarationMultipleDifferentValuesError.type]                                             = Json.format

}

case class EnhancedTransformationError(error: TransformationError, messageType: String, messageID: String)

object EnhancedTransformationError {
  implicit lazy val format: OFormat[EnhancedTransformationError] = Json.format
}
