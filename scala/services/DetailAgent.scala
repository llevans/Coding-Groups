package services

import _root_.scala.actors.Actor
import _root_.scala.reflect.BeanProperty
import com.ge.money.xsd.esb.y2012v11.icoe._
import javax.xml.bind.JAXBElement
import models.Account
import java.lang.String
import org.springframework.ws.client.core.{WebServiceTemplate, WebServiceMessageCallback}
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.soap.{SoapHeader, SoapMessage}
import services._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import services.Details

case object DetailAgent
case class Details(s : Account)
case object Stop

class DetailAgent(val webServiceTemplate: WebServiceTemplate, val accountEndpoint : String) extends Actor {
  var esbFactory: ObjectFactory = new ObjectFactory

  var messageType: String = null

  def act() {
    while (true) {
      receive {
        case Details(s) =>
          this.messageType = "AcctDetails"
          val acctDetailsRqType: AcctDetailsRqType = AccountInverter.invertAccount(s)
          val esbRequest: JAXBElement[AcctDetailsRqType] = esbFactory.createAcctDetailsRq(acctDetailsRqType)
          val esbResponse: JAXBElement[_] = webServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
          val acctDetailsRsType: AcctDetailsRsType = esbResponse.getValue.asInstanceOf[AcctDetailsRsType]
          var updatedAccount = AccountConverter.convert(acctDetailsRsType.getAcctDetailsRsBody, s)

          if (updatedAccount.getProductType.equals("TN")) {
              this.messageType = "Instruction"
              val instructionsRqType: InstructionRqType = AccountInverter.invertTermNoteAccount(updatedAccount)
              val esbRequest: JAXBElement[InstructionRqType] = esbFactory.createInstructionRq(instructionsRqType)
              val esbResponse: JAXBElement[_] = webServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
              val instructionsRsType: InstructionRsType = esbResponse.getValue.asInstanceOf[InstructionRsType]
              updatedAccount = AccountConverter.convert(instructionsRsType.getInstructionRsBody, updatedAccount)
          }
          reply(updatedAccount)
          Console.println("Agent: Details")

        case Stop =>
          Console.println("Agent: Stop")
          exit()
      }
    }
  }
  private[services] var insertSoapHeaderAndActionCallback: WebServiceMessageCallback = new WebServiceMessageCallback {
    def doWithMessage(message: WebServiceMessage) {
      try {
        val soapMessage: SoapMessage = message.asInstanceOf[SoapMessage]
        val soapHeader: SoapHeader = soapMessage.getSoapHeader
        webServiceTemplate.getMarshaller.marshal(LegacyIniter.initESBSoapHeader(messageType), soapHeader.getResult)
      }
      catch {
        case e: Exception => {
        }
      }
    }
  }

}
