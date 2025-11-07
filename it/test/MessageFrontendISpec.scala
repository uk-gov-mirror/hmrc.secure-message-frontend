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

import com.typesafe.config.ConfigFactory
import connectors.SecureMessageConnector
import model.{ EPaye, HmrcPptOrg, ReadPreference }
import org.jsoup.Jsoup
import org.jsoup.nodes.{ Document, Element }
import org.jsoup.select.Elements
import org.scalatest.{ Assertion, BeforeAndAfterEach }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{ HeaderNames, Status }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.WSClient
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import test.models.HmrcPodsOrg
import test.utils.{ AuthorityBuilder, TestAuthorisationProvider }
import uk.gov.hmrc.crypto.{ PlainText, SymmetricCryptoFactory }
import uk.gov.hmrc.domain.*
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }

import java.util.Base64
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.jdk.CollectionConverters.*
import scala.util.Random
import play.api.http.ContentTypes

class MessageFrontendISpec
    extends PlaySpec with GuiceOneServerPerSuite with ScalaFutures with BeforeAndAfterEach with Eventually {

  val duration15: Int = 15
  implicit val defaultTimeout: FiniteDuration = Duration(duration15, TimeUnit.SECONDS)

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.auth.port"       -> 8500,
      "play.http.router"                      -> "testOnlyDoNotUseInAppConf.Routes",
      "microservice.services.datastream.port" -> "8100",
      "microservice.services.datastream.host" -> "localhost"
    )
    .build()

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  lazy val testAuthorisationProvider: TestAuthorisationProvider = app.injector.instanceOf[TestAuthorisationProvider]

  val secureMessageResource = "http://localhost:9051/"

  override protected def beforeEach(): Unit =
    ws.url(s"${secureMessageResource}test-only/delete/secure-messages")
      .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
      .delete()
      .futureValue

  trait TestCase {

    def httpClient: WSClient = ws

    implicit val hc: HeaderCarrier = HeaderCarrier()

    lazy val authBuilder: AuthorityBuilder = testAuthorisationProvider.governmentGatewayAuthority().withSaUtr(utr)
    lazy val ggAuthorisationHeader: (String, String) = authBuilder.bearerTokenHeader()(Duration(1, TimeUnit.MINUTES))
    lazy val cookie: (String, String) = authBuilder.sessionCookie(ggAuthorisationHeader._2)

    def authResource(path: String): String = s"http://localhost:8585/$path"

    def utr: SaUtr

    val duration16: Int = 16
    def randomDetailsId: String = "C" + Random.alphanumeric.filter(_.isDigit).take(duration16).mkString

    lazy val atsMessage: JsObject = Json
      .parse(s"""
                | {
                | "externalRef":{
                |   "id" : "${Random.alphanumeric.filter(_.isDigit).take(duration16).mkString}",
                |   "source":"mdtp"
                |   },
                |   "recipient" : {
                |   "regime" : "sa",
                |   "taxIdentifier" : { "name":"sautr", "value":"$utr"},
                |   "name":{
                |         "line1": "LineOne Street",
                |         "title":"Mr",
                |         "forename":"William",
                |         "secondForename":"Harry",
                |         "surname":"Smith",
                |         "honours":"OBE"
                |      },
                |      "email": "user@email.com"
                |   },
                |   "messageType": "templateName",
                |   "subject":"Here is the subject for $utr",
                |   "content":"SGVyZSBpcyBzb21lIGNvbnRlbnQ=",
                |   "validFrom":"2017-02-14",
                |   "hash": "ABCDEFGHIJ",
                |   "statutory": false,
                |   "renderUrl": {
                |      "service": "ats-message-renderer",
                |      "url": "/ats-message-renderer/message/{messageId}"
                |   }
                | }
      """.stripMargin)
      .as[JsObject]

    lazy val statementMessage: JsObject =
      createMessageJson("mdtp", "SA300", TaxEntity("sa", utr), Some("user@email.com"))

    lazy val refundMessage: JsObject = createMessageJson("mdtp", "R002A", TaxEntity("sa", utr), Some("user@email.com"))

    def ninoMessage(nino: Nino): JsObject =
      createMessageJson("mdtp", "SA300", TaxEntity("paye", nino), Some("user@email.com"))

    def tavcMessage(ctUtr: CtUtr): JsObject =
      createMessageJson("tavcfe", "TAVC001", TaxEntity("ct", ctUtr), Some("user@email.com"))

    def fhddsMessage(fhdds: HmrcObtdsOrg): JsObject =
      createMessageJson("sees", "TAVC001", TaxEntity("fhdds", fhdds), Some("user@email.com"))

    def pptMessage(ppt: HmrcPptOrg): JsObject =
      Json
        .parse(s"""
                  | {
                  |   "externalRef":{
                  |      "id":"${Random.alphanumeric
                   .filter(_.isDigit)
                   .take(duration16)
                   .mkString}",
                  |      "source":"mdtp"
                  |   },
                  |   "recipient":{
                  |      "taxIdentifier":{
                  |         "name":"HMRC-PPT-ORG.ETMPREGISTRATIONNUMBER",
                  |         "value":"${ppt.value}"
                  |      },
                  |      "name":{
                  |         "line1": "LineOne Street",
                  |         "title":"Mr",
                  |         "forename":"William",
                  |         "secondForename":"Harry",
                  |         "surname":"Smith",
                  |         "honours":"OBE"
                  |      },
                  |      "email": "user@email.com"
                  |   },
                  |   "messageType":"message-type",
                  |   "subject":"Here is the subject for ${ppt.value}",
                  |   "content":"SGVyZSBpcyBzb21lIGNvbnRlbnQ=",
                  |   "validFrom":"2017-02-14",
                  |   "details":{
                  |      "formId":"SOME-FORM",
                  |      "statutory":true,
                  |      "paperSent":false,
                  |      "batchId":"1234567",
                  |      "issueDate":"2017-02-14"
                  |   }
                  |}
      """.stripMargin)
        .as[JsObject]

    def epayeMessage(epaye: EPaye): JsObject =
      Json
        .parse(s"""
                  | {
                  |   "externalRef":{
                  |      "id":"${Random.alphanumeric
                   .filter(_.isDigit)
                   .take(duration16)
                   .mkString}",
                  |      "source":"mdtp"
                  |   },
                  |   "recipient":{
                  |      "regime": "epaye",
                  |      "taxIdentifier":{
                  |         "name":"IR-PAYE.EMPREF",
                  |         "value":"${epaye.value}"
                  |      },
                  |      "fullName": "First Last",
                  |      "email": "user@email.com"
                  |   },
                  |   "messageType": "templateName",
                  |   "subject":"Here is the subject for ${epaye.value}",
                  |   "content":"SGVyZSBpcyBzb21lIGNvbnRlbnQ=",
                  |   "validFrom":"2017-02-14"
                  |}
      """.stripMargin)
        .as[JsObject]

    def podsMessage(identifierName: String, identifierValue: String): JsObject =
      Json
        .parse(s"""
                  | {
                  |   "externalRef":{
                  |      "id":"${Random.alphanumeric
                   .filter(_.isDigit)
                   .take(duration16)
                   .mkString}",
                  |      "source":"mdtp"
                  |   },
                  |   "recipient":{
                  |      "taxIdentifier":{
                  |         "name":"$identifierName",
                  |         "value":"$identifierValue"
                  |      },
                  |      "name":{
                  |         "line1": "LineOne Street",
                  |         "title":"Mr",
                  |         "forename":"William",
                  |         "secondForename":"Harry",
                  |         "surname":"Smith",
                  |         "honours":"OBE"
                  |      },
                  |      "email": "user@email.com"
                  |   },
                  |   "messageType":"message-type",
                  |   "subject":"Here is the subject for $identifierValue",
                  |   "content":"SGVyZSBpcyBzb21lIGNvbnRlbnQ=",
                  |   "validFrom":"2017-02-14",
                  |   "details":{
                  |      "formId":"SOME-FORM",
                  |      "statutory":true,
                  |      "paperSent":false,
                  |      "batchId":"1234567",
                  |      "issueDate":"2017-02-14"
                  |   }
                  |}
      """.stripMargin)
        .as[JsObject]

    def vatMessage(vat: HmrcMtdVat): JsObject = createVatMessageJson(TaxEntity("vat", vat), Some("user@email.com"))

    def createMessageJson(source: String, form: String, taxEntity: TaxEntity, email: Option[String]): JsObject =
      Json
        .parse(s"""
                  | {
                  |   "externalRef":{
                  |      "id":"${Random.alphanumeric
                   .filter(_.isDigit)
                   .take(duration16)
                   .mkString}",
                  |      "source":"$source"
                  |   },
                  |   "recipient":{
                  |      "taxIdentifier":{
                  |         "name":"${taxEntity.taxId.name}",
                  |         "value":"${taxEntity.taxId.value}"
                  |      },
                  |      "name":{
                  |         "line1": "LineOne Street",
                  |         "title":"Mr",
                  |         "forename":"William",
                  |         "secondForename":"Harry",
                  |         "surname":"Smith",
                  |         "honours":"OBE"
                  |      }
                  |      ${email
                   .map(e => s""", "email":"$e"""")
                   .getOrElse("")}
                  |   },
                  |   "messageType":"message-type",
                  |   "subject":"Here is the subject for ${taxEntity.taxId.value}",
                  |   "content":"SGVyZSBpcyBzb21lIGNvbnRlbnQ=",
                  |   "validFrom":"2017-02-14",
                  |   "details":{
                  |      "formId":"$form",
                  |      "statutory":true,
                  |      "paperSent":false,
                  |      "batchId":"1234567",
                  |      "issueDate":"2017-02-14"
                  |   }
                  |}
      """.stripMargin)
        .as[JsObject]

    def createVatMessageJson(taxEntity: TaxEntity, email: Option[String]): JsObject =
      Json
        .parse(s"""
                  |{
                  |  "externalRef":{
                  |      "id":"${Random.alphanumeric
                   .filter(_.isDigit)
                   .take(duration16)
                   .mkString}",
                  |     "source":"mdtp"
                  |  },
                  |  "recipient":{
                  |     "taxIdentifier":{
                  |        "name":"HMRC-MTD-VAT",
                  |        "value":"${taxEntity.taxId.value}"
                  |     },
                  |     "name":{
                  |        "line1":"Mr. John Smith"
                  |     }
                  |      ${email.map(e => s""", "email":"$e"""").getOrElse("")}
                  |  },
                  |  "messageType":"mtdfb_vat_principal_sign_up_successful",
                  |  "subject":"Here is the VAT subject for ${taxEntity.taxId.value}",
                  |  "content":"Some base64-encoded HTML",
                  |  "alertQueue":"PRIORITY"
                  |}
        """.stripMargin)
        .as[JsObject]

    def encryptSaMessageRendererReadUrl(messageId: String): String = {
      val crypto =
        SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = "queryParameter.encryption", ConfigFactory.load())
      Base64.getEncoder.encodeToString(crypto.encrypt(PlainText(s"$messageId")).value.getBytes)
    }

    case class FakeResponse(json: JsValue, status: Int, responseString: String = "")

    def getMessageForEncryptedUrl(encryptedUrl: String = ""): FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest("GET", s"/messages/$encryptedUrl")

    def messagesCount(
      readPreference: Option[ReadPreference.Value],
      taxIdentifiers: List[String] = List(),
      regimes: List[String] = List()
    ): FakeRequest[AnyContentAsEmpty.type] = {

      val readOnlyParam = readPreference.fold("") { x =>
        s"?read=$x"
      }
      val taxIdentifiersParam = SecureMessageConnector.formatQueryParam(taxIdentifiers, regimes = regimes)
      FakeRequest("GET", s"/messages/count$readOnlyParam$taxIdentifiersParam")
    }

    def messagesPost(body: JsObject): String = {
      val response =
        httpClient
          .url(s"${secureMessageResource}messages")
          .withHttpHeaders(SessionKeys.authToken -> ggAuthorisationHeader._2)
          .post(body)
          .futureValue

      response.status must be(Status.CREATED)
      (response.json \\ "id").map(_.as[JsString].value).head
    }

    def messages(
      taxIdentifiers: List[String] = List(),
      regimes: List[String] = List()
    ): FakeRequest[AnyContentAsEmpty.type] = {
      val identifiersParams = SecureMessageConnector.formatQueryParam(taxIdentifiers, regimes = regimes)
      FakeRequest("GET", s"/messages$identifiersParams")
    }

    def messagesBta(
      taxIdentifiers: List[String] = List(),
      regimes: List[String] = List()
    ): FakeRequest[AnyContentAsEmpty.type] = {
      val identifiersParams = SecureMessageConnector.formatQueryParam(taxIdentifiers, regimes = regimes)
      FakeRequest("GET", s"/messages/bta$identifiersParams")
    }

    def messagesInboxLink1(
      taxIdentifiers: List[String] = List(),
      authHeader: Option[(String, String)] = None,
      cookieToUse: Option[(String, String)] = None,
      regimes: List[String] = List()
    ): Option[String] = {
      val identifiersParams =
        SecureMessageConnector.formatQueryParam(taxIdentifiers, alwaysAppend = true, regimes = regimes)
      val request = FakeRequest("GET", s"/messages/inbox-link?messagesInboxUrl=/example/url$identifiersParams")
        .withSession(SessionKeys.authToken -> authHeader.getOrElse(ggAuthorisationHeader)._2)

      val result = route(app, request).get
      status(result) must be(Status.OK)
      val alerts = Jsoup.parse(contentAsString(result)).getElementById("unreadMessages")
      if (alerts == null) None else Some(alerts.text())
    }

    def messagesInboxLink(
      taxIdentifiers: List[String] = List(),
      authHeader: Option[(String, String)] = None,
      cookieToUse: Option[(String, String)] = None,
      regimes: List[String] = List()
    ): Option[String] = {
      testAuthorisationProvider.governmentGatewayAuthority().bearerTokenHeader()
      SecureMessageConnector.formatQueryParam(taxIdentifiers, alwaysAppend = true, regimes = regimes)

      val bt = authBuilder.bearerTokenHeader()

      val response =
        ws.url(s"${secureMessageResource}secure-messaging/messages")
          .withHttpHeaders(HeaderNames.AUTHORIZATION -> bt._2)
          .get()

      val responseValue = response.futureValue
      responseValue.status must be(Status.OK)
      val alerts = Jsoup.parse(responseValue.body).getElementById("unreadMessages")
      if (alerts == null) None else Some(alerts.text())
    }

    def renderMessageListPartial(expectedMessages: Int): (Elements, Document) =
      eventually {
        val request = messages().withSession(SessionKeys.authToken -> authBuilder.bearerTokenHeader()._2)
        val result = route(app, request).get
        status(result) must be(Status.OK)

        val parsedMessages = Jsoup.parse(contentAsString(result))

        val rows = parsedMessages.getElementsByClass("message")
        withClue("number of messages") {
          rows.size() must be >= expectedMessages
        }
        (rows, parsedMessages)
      }

    def setupFilterableMessages: (AuthorityBuilder, String, String, String, String, String, String, String) = {
      val nino = Nino("NH123456D")
      val ctUtr = CtUtr("876487234")
      val fhdds = HmrcObtdsOrg("XZFH00000100024")
      val ppt = HmrcPptOrg("XMPPT0000000001")
      val pods = HmrcPodsOrg("A1234567")
      val vat = HmrcMtdVat("123456789")
      val epaye = EPaye("840Pd00123456")
      val authProvider = testAuthorisationProvider
        .governmentGatewayAuthority()
        .withNino(nino)
        .withSaUtr(utr)
        .withCtUtr(ctUtr)
        .withFhdds(fhdds)
        .withMtdVat(vat)
        .withPpt(ppt)
        .withPods(pods)
        .withEPaye(epaye)

      messagesPost(ninoMessage(nino))
      messagesPost(statementMessage)
      messagesPost(tavcMessage(ctUtr))
      messagesPost(fhddsMessage(fhdds))
      messagesPost(pptMessage(ppt))
      messagesPost(vatMessage(vat))
      messagesPost(epayeMessage(epaye))

      (authProvider, nino.value, ctUtr.value, fhdds.value, vat.value, ppt.value, pods.value, epaye.value)
    }
  }

  def expectedMessages(responseBody: String, expectedMessagesCount: Int): Assertion = {
    val parsedMessages = Jsoup.parse(responseBody)

    val rows = parsedMessages.getElementsByClass("message")
    withClue("number of messages") {
      rows.size() must be >= expectedMessagesCount
    }
  }

  def exactMessageCount(responseBody: String, messagesCount: Int): Assertion = {
    val parsedMessages = Jsoup.parse(responseBody)

    val rows = parsedMessages.getElementsByClass("table--borderless").first().getElementsByClass("message")
    withClue("exact number of messages") {
      rows.size() must be(messagesCount)
    }
  }

  def emailMessagesSubject(responseBody: String): List[String] = {
    val parsedMessages = Jsoup.parse(responseBody)

    parsedMessages
      .getElementsByClass("table--borderless")
      .first()
      .getElementsByClass("message-subject")
      .iterator()
      .asScala
      .toList
      .map { subjectElement =>
        subjectElement.html()
      }
  }

  def messageSubHeading(body: String): String =
    Jsoup.parse(body).getElementsByClass("taxid-heading").first().getElementsByTag("p").first().html()

  def hasMessageSubHeading(body: String): Boolean = !Jsoup.parse(body).getElementsByClass("taxid-heading").isEmpty

  def headingIdentifiers(body: String): Set[(String, String)] = {
    val entries: Elements = Jsoup.parse(body).getElementsByClass("tabular-data__entry")
    entries.asScala.toSet.map { (headingElements: Element) =>
      val idLabel = headingElements.getElementsByClass("tabular-data__heading").first().html()
      val idValue = headingElements.getElementsByClass("tabular-data__data-1").first().html()
      (idLabel, idValue)
    }
  }
}
