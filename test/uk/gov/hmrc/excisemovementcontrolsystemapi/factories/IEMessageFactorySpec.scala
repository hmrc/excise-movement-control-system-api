package uk.gov.hmrc.excisemovementcontrolsystemapi.factories

import generated.{MessagesOption, NewMessagesDataResponse}
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import scalaxb.DataRecord
import uk.gov.hmrc.excisemovementcontrolsystemapi.models.messages.{IE704Message, IE801Message, IE810Message, IE813Message, IE815Message, IE818Message, IE819Message, IE837Message, IE871Message}

class IEMessageFactorySpec extends PlaySpec {

  private val message = mock[DataRecord[MessagesOption]]
  private val sut = IEMessageFactory()


  "createIEMessage" should {

    "return throw an error" when {
      "cannot handle message type" in {
        when(message.key).thenReturn(Some("Anything"))
        intercept[RuntimeException] {
          sut.createIEMessage(message)
        }.getMessage mustBe s"Could not create Message object. Unsupported message: Anything"
      }

      "messageType is empty" in {
        when(message.key).thenReturn(None)
        intercept[RuntimeException] {
          sut.createIEMessage(message)
        }.getMessage mustBe "Could not create Message object. Message type is empty"
      }
    }

    "return an instance of IE704Message" in {
      when(message.key).thenReturn(Some("IE704"))
      sut.createIEMessage(message).isInstanceOf[IE704Message]
    }

    "return an instance of IE801Message" in {
      when(message.key).thenReturn(Some("IE801"))
      sut.createIEMessage(message).isInstanceOf[IE801Message]
    }

    "return an instance of IE810Message" in {
      when(message.key).thenReturn(Some("IE810"))
      sut.createIEMessage(message).isInstanceOf[IE810Message]
    }

    "return an instance of IE813Message" in {
      when(message.key).thenReturn(Some("IE813"))
      sut.createIEMessage(message).isInstanceOf[IE813Message]
    }

    "return an instance of IE815Message" in {
      when(message.key).thenReturn(Some("IE815"))
      sut.createIEMessage(message).isInstanceOf[IE815Message]
    }

    "return an instance of IE818Message" in {
      when(message.key).thenReturn(Some("IE818"))
      sut.createIEMessage(message).isInstanceOf[IE818Message]
    }

    "return an instance of IE819Message" in {
      when(message.key).thenReturn(Some("IE819"))
      sut.createIEMessage(message).isInstanceOf[IE819Message]
    }

    "return an instance of IE837Message" in {
      when(message.key).thenReturn(Some("IE837"))
      sut.createIEMessage(message).isInstanceOf[IE837Message]
    }

    "return an instance of IE871Message" in {
      when(message.key).thenReturn(Some("IE871"))
      sut.createIEMessage(message).isInstanceOf[IE871Message]
    }
  }
}
