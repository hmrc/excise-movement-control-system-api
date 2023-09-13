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

import com.google.inject.ImplementedBy

import javax.inject.Inject
import scala.xml.{NodeSeq, SAXParseException}
class EmcsXmlParser @Inject() extends Parser {

  @throws(classOf[SAXParseException])
  def getXmlContentFromString(strToParse: String, tags: String*): Option[String] = {
    val xml: NodeSeq = scala.xml.NodeSeq.fromSeq(scala.xml.XML.loadString(strToParse))

    getXmlContentFrom(xml, tags: _*)
  }

  @throws(classOf[SAXParseException])
  def getXmlContentFrom(xml: NodeSeq, tags: String*): Option[String] = {
    val value = tags.flatMap(o => xml \ o).map(n => n.text.trim)
    if(value.isEmpty) None else Some(value.mkString(","))
  }
}

@ImplementedBy(classOf[EmcsXmlParser])
trait Parser {
  def getXmlContentFromString(strToParse: String, tags: String*): Option[String]
  def getXmlContentFrom(xml: NodeSeq, tags: String*): Option[String]
}
