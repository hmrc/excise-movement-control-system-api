package uk.gov.hmrc.excisemovementcontrolsystemapi.models
import Transformation.{XmlNamespaceTransformer, ie801Encoded, ie815decoded}
import org.w3c.dom.ls.LSInput
import generated.v1.{IE801Type => IE801TypeV1}
import generated.v2._

import java.io.{InputStream, StringReader}
import java.util.Base64
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import scala.annotation.unused
import scala.xml.{Elem, PrettyPrinter}
object TransformXSD {

  val xsdPaths = Map(
    "IE717" -> "ie717.xsd",
    "IE801" -> "v2/ie801.xsd",
    "IE802" -> "ie802.xsd",
    "IE803" -> "ie803.xsd",
    "IE804" -> "ie804.xsd",
    "IE805" -> "ie805.xsd",
    "IE806" -> "ie806.xsd",
    "IE807" -> "ie807.xsd",


  )

  def main(args: Array[String]) = {


    val updatedHeaderNameSpaceXML = XmlNamespaceTransformer.updateXMLNamespace(scala.xml.XML.loadString(Transformation.ie815Xml))

    // println(IE815XML.ie815encoded)
    // println(IE815XML.ie815decoded)
    val pp = new PrettyPrinter(300, 2)

    println(pp.format(updatedHeaderNameSpaceXML))

    // validateSchema("IE801", updatedHeaderNameSpaceXML)

    ()

  }


  def validateFormat(messageType: String, message: String): Either[TransformationError, Unit] = {
    try {
      messageType match {
        case "IE704" => scalaxb.fromXML[IE704Type](scala.xml.XML.loadString(message)); Right()
        case "IE801" => scalaxb.fromXML[IE801Type](scala.xml.XML.loadString(message)); Right()
        case "IE802" => scalaxb.fromXML[IE802Type](scala.xml.XML.loadString(message)); Right()
        case "IE803" => scalaxb.fromXML[IE803Type](scala.xml.XML.loadString(message)); Right()
        case "IE807" => scalaxb.fromXML[IE807Type](scala.xml.XML.loadString(message)); Right()
        case "IE810" => scalaxb.fromXML[IE810Type](scala.xml.XML.loadString(message)); Right()
        case "IE813" => scalaxb.fromXML[IE813Type](scala.xml.XML.loadString(message)); Right()
        case "IE815" => scalaxb.fromXML[IE815Type](scala.xml.XML.loadString(message)); Right()
        case "IE818" => scalaxb.fromXML[IE818Type](scala.xml.XML.loadString(message)); Right()
        case "IE819" => scalaxb.fromXML[IE819Type](scala.xml.XML.loadString(message)); Right()
        case "IE829" => scalaxb.fromXML[IE829Type](scala.xml.XML.loadString(message)); Right()
        case "IE837" => scalaxb.fromXML[IE837Type](scala.xml.XML.loadString(message)); Right()
        case "IE839" => scalaxb.fromXML[IE839Type](scala.xml.XML.loadString(message)); Right()
        case "IE840" => scalaxb.fromXML[IE840Type](scala.xml.XML.loadString(message)); Right()
        case "IE871" => scalaxb.fromXML[IE871Type](scala.xml.XML.loadString(message)); Right()
        case "IE881" => scalaxb.fromXML[IE881Type](scala.xml.XML.loadString(message)); Right()
        case "IE905" => scalaxb.fromXML[IE905Type](scala.xml.XML.loadString(message)); Right()
        case _ => Left(MessageDoesNotExistError)
      }

    } catch {
      case e: Exception => Left(FormatValidationError(e.toString))
    }


  }

  def validateSchema(messageType: String, message: String): Either[TransformationError, Unit] = {

    try {
      val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val url = getClass.getClassLoader.getResource(xsdPaths(messageType))
      val src = new StreamSource(url.openStream())
      src.setSystemId(url.toExternalForm)
      println(url.toExternalForm)
      val schema = schemaFactory.newSchema(src)


      val validator = schema.newValidator()
      validator.validate(new StreamSource(new StringReader(message)))
      Right(())
    } catch {
      case e: Exception => Left(SchemaValidationError(e.toString))
    }
  }
}




