package services

import _root_.scala.actors.Future
import _root_.scala.collection.mutable.Stack
import _root_.scala.reflect.BeanProperty
import _root_.scala.util.control.Breaks._
import exceptions.EsbException
import models.Account
import models.Accounts
import models.InvestmentRates
import models.RecentActivity
import xsd.corelib.y2010v10.icoe.MsgRsHdrType
import xsd.esb.y2012v11.icoe._
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.core.{WebServiceTemplate, WebServiceMessageCallback}
import org.springframework.ws.client.core.support.WebServiceGatewaySupport
import org.springframework.ws.soap.SoapHeader
import org.springframework.ws.soap.SoapMessage
import javax.xml.bind.JAXBElement
import java.util.ArrayList
import java.util.Calendar
import java.util.List
import javax.xml.transform.stream.StreamResult
import java.lang.String
import services._

object AccountClient {}

class AccountClient extends WebServiceGatewaySupport with IESBClient {
  var esbFactory: ObjectFactory = new ObjectFactory
  var messageType: String = null
  var accountEndpoint: String = null

  val readyStack : scala.collection.mutable.Stack[DetailAgent] = Stack()

  for (i <- 0 to 20)   {
      val detailAgent : DetailAgent = new DetailAgent(getWebServiceTemplate(), accountEndpoint)
      detailAgent.start
      readyStack.push(detailAgent)
  }

  def getAccountList(account: Account): Accounts = {
    val activeStack : scala.collection.mutable.Stack[DetailAgent] = Stack()
    this.messageType = "AcctList"
    val timer = System.currentTimeMillis();
    val acctListRqType: AcctListRqType = AccountInverter.invertAccountList(account)
    val esbRequest: JAXBElement[AcctListRqType] = esbFactory.createAcctListRq(acctListRqType)
    val esbResponse: JAXBElement[_] = getWebServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
    val accListResponse: AcctListRsType = esbResponse.getValue.asInstanceOf[AcctListRsType]
    val accountList: Accounts = AccountConverter.convert(accListResponse)
    val detailedAccountList: List[Account] = new ArrayList[Account]
    val futureList: List[Future[Any]] = new ArrayList[Future[Any]]

    import scala.collection.JavaConversions._
    for (investmentAccount <- accountList.getItems) {
      val detailAgent : DetailAgent = readyStack.pop
      activeStack.push(detailAgent)
      futureList.add(detailAgent !! Details(investmentAccount))
    }
    for (future <- futureList) {
      val result = future()
      var updatedAccount = result.asInstanceOf[Account]
      detailedAccountList.add(updatedAccount)
    }

    for (i <- 0 until accountList.getItems().size)
      readyStack.push(activeStack.pop)

    accountList.setItems(detailedAccountList)
    System.out.println("dbg: Account List from ESB ==> duration " + (System.currentTimeMillis() - timer)/1000.00);
    return accountList
  }

  def getMaturityPayoutOption(investmentAccount: Account): Account = {
    this.messageType = "Instructions"
    val instructionRqType: InstructionRqType = AccountInverter.invertTermNoteAccount(investmentAccount)
    val esbRequest: JAXBElement[InstructionRqType] = esbFactory.createInstructionRq(instructionRqType)
    val esbResponse: JAXBElement[_] = getWebServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
    val instructionRsType: InstructionRsType = esbResponse.getValue.asInstanceOf[InstructionRsType]
    var updatedAccount = AccountConverter.convert(instructionRsType.getInstructionRsBody, investmentAccount)
    return updatedAccount
  }

  def getAccountDetails(investmentAccount: Account): Account = {
    this.messageType = "AcctDetails"
    val acctDetailsRqType: AcctDetailsRqType = AccountInverter.invertAccount(investmentAccount)
    val esbRequest: JAXBElement[AcctDetailsRqType] = esbFactory.createAcctDetailsRq(acctDetailsRqType)
    val esbResponse: JAXBElement[_] = getWebServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
    val acctDetailsRsType: AcctDetailsRsType = esbResponse.getValue.asInstanceOf[AcctDetailsRsType]
    var updatedAccount = AccountConverter.convert(acctDetailsRsType.getAcctDetailsRsBody, investmentAccount)
    return updatedAccount
  }

  def getInvestmentRates(inProductType: String): InvestmentRates = {
    this.messageType = "Rates"
    val ratesRq: RatesRqType = esbFactory.createRatesType
    ratesRq.setMsgRqHdr(LegacyIniter.initESBRequestMessageHeader)
    val requestBody: RatesRqType.RatesRqBody = esbFactory.createRatesRqTypeRatesRqBody
    requestBody.setAcctClass(Account.acctClassEnumeration.get("Retail"))
    requestBody.setProductType(inProductType)
    ratesRq.setRatesRqBody(requestBody)
    val esbRequest: JAXBElement[RatesRqType] = esbFactory.createRatesRq(ratesRq)
    val esbResponse: JAXBElement[_] = getWebServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
    val ratesRs: RatesRsType = esbResponse.getValue.asInstanceOf[RatesRsType]
    var rates: InvestmentRates = new InvestmentRates
    if (ratesRs != null) {
      rates = RatesConverter.convert(ratesRs.getRatesRsBody)
    }
    return rates
  }

  def getRecentActivity(account: Account): RecentActivity = {
    this.messageType = "AcctActivity"
    val acctActivityRq: acctActivityRqType = ActivityInverter.invertAccount(account)
    val esbRequest: JAXBElement[AcctActivityRqType] = esbFactory.createAcctActivityRq(recentActivityRq)
    val esbResponse: JAXBElement[_] = getWebServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
    val acctActivityRsType: AcctActivityRsType = esbResponse.getValue.asInstanceOf[AcctActivityRsType]
    var activity: RecentActivity = new RecentActivity
    if (acctActivityRsType != null) {
      activity = ActivityConverter.convert(acctActivityRsType.getAcctActivityRsBody)
    }
    return activity
  }

  def search(account: Account): RecentActivity = {
    var activity: RecentActivity = new RecentActivity
    if (account == null) return activity
    if (account.getSearchBy == "search-date") {
      if (account.getBeginDate != null) {
        if (account.getEndDate != null) {
          activity = searchByDateRange(account)
        }
        else {
          account.setEndDate(Calendar.getInstance.getTime)
          activity = searchByDateRange(account)
        }
      }
    }
    else if (account.getSearchBy == "search-check") {
      if (account.getBeginCheck != null) {
        if (account.getEndCheck != null) {
          activity = searchByCheckRange(account)
        }
        else {
          activity = searchByCheck(account)
        }
      }
    }
    else {
      if (account.getBeginAmount != null) {
        activity = searchByAmountRange(account)
      }
    }
    return activity
  }

  def searchByDateRange(account: Account): RecentActivity = {
    this.messageType = "TransByDate"
    val transByDateRqType: TransByDateRqType = ActivityInverter.invertAccountForDateSearch(account)
    val esbRequest: JAXBElement[TransByDateRqType] = esbFactory.createTransByDateRq(tranByDtRqType)
    val esbResponse: JAXBElement[_] = getWebServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
    val transByDateRsType: TransByDateRsType = esbResponse.getValue.asInstanceOf[TransByDateRsType]
    var activity: RecentActivity = new RecentActivity
    if (transByDateRsType != null && transByDateRsType.getTransByDateRsBody != null) {
      activity = ActivityConverter.convert(transByDateRsType.getTransByDateRsBody)
    }
    return activity
  }

  def searchByCheckRange(account: Account): RecentActivity = {
    this.messageType = "TransByCheck"
    val transByCheckRq: TransByCheckRqType = ActivityInverter.invertAccountForCheckSearch(account)
    val esbRequest: JAXBElement[TransByCheckRqType] = esbFactory.createTransByCheckRq(transByCheckRq)
    val esbResponse: JAXBElement[_] = getWebServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
    val transByCheckRsType: TransByCheckRsType = esbResponse.getValue.asInstanceOf[TransByCheckRsType]
    var activity: RecentActivity = new RecentActivity
    if (transByCheckRsType != null && transByCheckRsType.getTransByCheckRsBody != null) {
      activity = ActivityConverter.convert(transByCheckRsType.getTransByCheckRsBody)
    }
    return activity
  }

  def searchByCheck(account: Account): RecentActivity = {
    this.messageType = "TransByCheckNum"
    val transByCheckNumRq: TransByCheckNumRqType = ActivityInverter.invertAccountForCheckNumSearch(account)
    val esbRequest: JAXBElement[TransByCheckNumRqType] = esbFactory.createTransByCheckNumRq(transByCheckNumRq)
    val esbResponse: JAXBElement[_] = getWebServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
    val transByCheckNumRsType: TransByCheckNumRsType = esbResponse.getValue.asInstanceOf[TransByCheckNumRsType]
    var activity: RecentActivity = new RecentActivity
    if (transByCheckNumRsType != null && transByCheckNumRsType.getTransByCheckNumRsBody != null) {
      activity = ActivityConverter.convert(transByCheckNumRsType.getTransByCheckNumRsBody)
    }
    return activity
  }

  def searchByAmountRange(account: Account): RecentActivity = {
    this.messageType = "TransByAmt"
    val transByAmtRq: TransByAmtRqType = ActivityInverter.invertAccountForAmountSearch(account)
    val esbRequest: JAXBElement[TransByAmtRqType] = esbFactory.createTransByAmtRq(transByAmtRq)
    val esbResponse: JAXBElement[_] = getWebServiceTemplate.marshalSendAndReceive(accountEndpoint, esbRequest, insertSoapHeaderAndActionCallback).asInstanceOf[JAXBElement[_]]
    val transByAmtRsType: TransByAmtRsType = esbResponse.getValue.asInstanceOf[TransByAmtRsType]
    var activity: RecentActivity = new RecentActivity
    if (transByAmtRsType != null && transByAmtRsType.getTransByAmtRsBody != null) {
      activity = ActivityConverter.convert(transByAmtRsType.getTransByAmtRsBody)
    }
    return activity
  }

  private def lookForErrorCode(responseHeader: MsgRsHdrType) {
    if (responseHeader != null && responseHeader.getStatus != null) {
      responseHeader.getStatus.getStatusCode match {
        case "100" =>
          break //todo: break is not supported
        case "1002" =>
        case "1004" =>
        case "1403" =>
          throw new EsbException(responseHeader.getStatus.getStatusDetails.getStatusDetail.get(0).getDesc.get(0))
        case _ =>
          throw new EsbException(responseHeader.getStatus.getStatusDetails.getStatusDetail.get(0).getDesc.get(0))
      }
    }
  }

  private def soutRequest(request: AnyRef) {
    try {
      val result: StreamResult = new StreamResult(System.out)
      System.out.println("\n")
      getWebServiceTemplate.getMarshaller.marshal(request, result)
      System.out.println("\n")
    }
    catch {
      case e: Exception => {
      }
    }
  }

  private[services] var insertSoapHeaderAndActionCallback: WebServiceMessageCallback = new WebServiceMessageCallback {
    def doWithMessage(message: WebServiceMessage) {
      try {
        val soapMessage: SoapMessage = message.asInstanceOf[SoapMessage]
        val soapHeader: SoapHeader = soapMessage.getSoapHeader
        getWebServiceTemplate.getMarshaller.marshal(LegacyIniter.initESBSoapHeader(messageType), soapHeader.getResult)
      }
      catch {
        case e: Exception => {
        }
      }
    }
  }
}

