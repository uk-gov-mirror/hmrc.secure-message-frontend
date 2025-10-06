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

package controllers

import com.codahale.metrics.SharedMetricRegistries
import com.typesafe.config.ConfigFactory
import config.AppConfig
import connectors.SecureMessageConnector
import helpers.LanguageHelper
import model.{ Encoder, EncryptAndEncode, MessageCount, MessageListItem }
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.i18n.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{ Enrolment, EnrolmentIdentifier, Enrolments }
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto
import uk.gov.hmrc.http.HeaderCarrier
import views.helpers.PortalUrlBuilder

import java.time.LocalDate
import scala.concurrent.{ ExecutionContext, Future }

class PartialHtmlSpec
    extends PlaySpec with MockitoSugar with ScalaFutures with GuiceOneAppPerSuite with IntegrationPatience
    with LanguageHelper {

  implicit val request: FakeRequest[_] = FakeRequest("GET", "/message/sa/123456789")
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val enrolmentSa: Enrolment = Enrolment(
    key = "IR-SA",
    state = "Activated",
    identifiers = List(EnrolmentIdentifier(key = "UTR", value = "123456789"))
  )
  val enrolmentNino: Enrolment = Enrolment(
    key = "HMRC-NI",
    state = "Activated",
    identifiers = List(EnrolmentIdentifier(key = "NINO", value = "AA123456A"))
  )
  val enrolmentNino2: Enrolment = Enrolment(
    key = "HMRC-NI",
    state = "Activated",
    identifiers = List(EnrolmentIdentifier(key = "NINO", value = "AA123456D"))
  )
  val enrolmentCt: Enrolment = Enrolment(
    key = "IR-CT",
    state = "Activated",
    identifiers = List(EnrolmentIdentifier(key = "UTR", value = "987654321"))
  )
  val enrolmentsWithSa: Enrolments = Enrolments(Set(enrolmentSa))
  val enrolmentsWithNino: Enrolments = Enrolments(Set(enrolmentNino))
  val enrolmentsWithNino2: Enrolments = Enrolments(Set(enrolmentNino2))
  val enrolmentsWithSaNinoCt: Enrolments = Enrolments(Set(enrolmentCt, enrolmentSa, enrolmentNino))

  lazy val appConfigMock: AppConfig = mock[AppConfig]
  lazy val messageConnectorMock: SecureMessageConnector = mock[SecureMessageConnector]
  lazy val portalUrlBuilderMock: PortalUrlBuilder = mock[PortalUrlBuilder]
  lazy val partialHtml: PartialHtml = mock[PartialHtml]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure()
      .overrides(bind[AppConfig].toInstance(appConfigMock))
      .overrides(bind[SecureMessageConnector].toInstance(messageConnectorMock))
      .overrides(bind[PortalUrlBuilder].toInstance(portalUrlBuilderMock))
      .overrides(bind[PartialHtml].toInstance(partialHtml))
      .build()

  trait TestListPartialHtml extends PartialHtml {

    lazy val applicationCrypto = app.injector.instanceOf[ApplicationCrypto]
    val encryptAndEncode = new EncryptAndEncode(applicationCrypto) {
      override lazy val encoder: Encoder = new Encoder {
        override def encryptAndEncode(value: String) = s"encoded(encrypted($value))"
      }
    }

    override val appConfig: AppConfig = appConfigMock

    def messageHeaders: Seq[MessageListItem]

    override def messageConnector: SecureMessageConnector = messageConnectorMock

    override def portalUrlBuilder: PortalUrlBuilder = portalUrlBuilderMock
  }

  "ListResult" must {

    when(portalUrlBuilderMock.buildPortalUrl(any[Option[String]], any[String])).thenReturn("/someUrl")

    "display SA-agnostic text for non-SA users (who will have no messages), as Nino user in English" in {

      implicit val messages: Messages = messagesInEnglish()

      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] = Seq.empty
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("en"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      listResults.asListHtml(enrolmentsWithNino2).futureValue.body must (
        include(Messages("empty.message")) and
          not include "Self Assessment" and
          not include (Messages("read.other.message"))
      )
    }

    "display SA-agnostic text for SA users with no messages" in {

      implicit val messages: Messages = messagesInEnglish()

      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] = Seq.empty
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("en"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      listResults.asListHtml(enrolmentsWithSa).futureValue.body must (
        include(Messages("empty.message")) and
          not include "Self Assessment" and
          not include (Messages("read.other.message"))
      )
    }

    "display SA-agnostic text for SA users with messages" in {

      implicit val messages: Messages = messagesInEnglish()

      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] =
          Seq(
            MessageListItem(
              id = "",
              subject = "Leaving self assessment",
              validFrom = LocalDate.parse("2014-08-14"),
              readTime = None,
              sentInError = false
            )
          )
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("en"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      listResults.asListHtml(enrolmentsWithSa).futureValue.body must (
        not include Messages("tax.identifiers.heading")(messages) and
          not include "Self Assessment" and
          not include (Messages("read.other.message")(messages))
      )
    }

    "display the correct content in welsh if message is empty" in {

      implicit val messages: Messages = messagesInEnglish()

      val cyMessages = messagesInWelsh()

      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] = Seq.empty
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("cy"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      val result = listResults.asListHtml(enrolmentsWithSa)(hc, welshRequest, ec, cyMessages).futureValue.body

      result must (
        include(cyMessages("empty.message")) and
          not include (cyMessages("read.other.message"))
      )

      result mustNot (
        include(Messages("empty.message")) and
          not include (Messages("read.other.message"))
      )
    }

    "display the correct content in welsh if message exists" in {

      val cyMessages = messagesInWelsh()
      implicit val messages: Messages = messagesInEnglish()

      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] =
          Seq(
            MessageListItem(
              "",
              "Leaving self assessment",
              LocalDate.parse("2014-08-14"),
              readTime = None,
              sentInError = false
            )
          )
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("cy"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      val header = request.headers.add((HeaderNames.ACCEPT_LANGUAGE, "cy"))
      val req = request.withHeaders(header)

      val result = listResults.asListHtml(enrolmentsWithSa)(hc, req, ec, cyMessages).futureValue.body

      result must (
        include(cyMessages("sa.heading.status")) and
          include(cyMessages("sa.heading.date")) and
          include(cyMessages("sa.heading.subject")) and
          include(cyMessages("unread"))
      )

      result mustNot (
        include(Messages("sa.heading.status")) and
          include(Messages("sa.heading.date")) and
          include(Messages("sa.heading.subject")) and
          include(Messages("unread"))
      )
    }

    "filter out headers of messages that have been replied to" in {

      implicit val messages: Messages = messagesInEnglish()

      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] =
          Seq(
            MessageListItem(
              "5c5467b70c00000c00a1b0c4",
              "This is the original",
              LocalDate.parse("2014-08-14"),
              readTime = None,
              sentInError = false
            ),
            MessageListItem(
              id = "6c5467b70c00000c00a1b0c5",
              subject = "This is the reply",
              validFrom = LocalDate.parse("2014-08-14"),
              readTime = None,
              sentInError = false,
              replyTo = Option("5c5467b70c00000c00a1b0c4")
            ),
            // Out of order multi-reply
            MessageListItem(
              id = "6c5467b70c00000c00a1b0c6",
              subject = "This is an unordered reply",
              validFrom = LocalDate.parse("2014-08-14"),
              readTime = None,
              sentInError = false,
              replyTo = Option("6c5467b70c00000c00a1b0c3")
            ),
            MessageListItem(
              "6c5467b70c00000c00a1b0c3",
              "This is an unordered original",
              LocalDate.parse("2014-08-14"),
              readTime = None,
              sentInError = false
            ),
            MessageListItem(
              id = "6c5467b70c00000c00a1b0c0",
              subject = "This is the latest unordered reply",
              validFrom = LocalDate.parse("2016-08-14"),
              readTime = None,
              sentInError = false,
              replyTo = Option("6c5467b70c00000c00a1b0c6")
            )
          )
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("en"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      val result = listResults.asListHtml(enrolmentsWithNino)(hc, request, ec, messages).futureValue.body

      result must (
        include(Messages("This is the reply")) and
          not include Messages("This is the original") and
          include(Messages("This is the latest unordered reply")) and
          not include Messages("This is an unordered original") and
          not include Messages("This is an unordered reply")
      )
    }
    SharedMetricRegistries.clear()

  }

  "btaListResult" must {

    "display no text for users with no messages" in {

      implicit val messages: Messages = messagesInEnglish()

      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] = Seq.empty
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("en"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      listResults.btaListHtml(enrolmentsWithSa).futureValue.body must include(Messages("sa.no.messages"))
    }

    implicit val messages: Messages = messagesInEnglish()

    "display SA text for SA users with messages" in {
      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] =
          Seq(
            MessageListItem(
              "",
              "Leaving self assessment",
              LocalDate.parse("2014-08-14"),
              readTime = None,
              sentInError = false
            )
          )
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("en"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      listResults.btaListHtml(enrolmentsWithSa).futureValue.body must (
        include("14 August 2014") and
          include("Leaving self assessment")
      )
    }

    "display message withdrawn error when message contains error" in {

      implicit val messages: Messages = messagesInEnglish()

      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] =
          Seq(
            MessageListItem(
              "",
              "Leaving self assessment",
              LocalDate.parse("2014-08-14"),
              readTime = None,
              sentInError = true
            )
          )
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("en"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      listResults.btaListHtml(enrolmentsWithSa).futureValue.body must include(Messages("withdrawn"))
    }

    "display the correct content in welsh" in {

      implicit val messages: Messages = messagesInEnglish()

      val cyMessages: Messages = messagesInWelsh()

      val listResults = new TestListPartialHtml {
        override def messageHeaders: Seq[MessageListItem] =
          Seq(
            MessageListItem(
              "",
              "Leaving self assessment",
              LocalDate.parse("2014-08-14"),
              readTime = None,
              sentInError = true
            )
          )
        when(
          messageConnectorMock.messages(any[List[String]], any[List[String]], ArgumentMatchers.eq("cy"))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(messageHeaders))
      }

      val result =
        listResults.btaListHtml(enrolmentsWithSa, List("ctutr"))(hc, welshRequest, ec, cyMessages).futureValue.body
      result must (
        include(cyMessages("sa.heading.status")) and
          include(cyMessages("sa.heading.date")) and
          include(cyMessages("sa.heading.subject")) and
          include(cyMessages("withdrawn")) and
          include(cyMessages("unread"))
      )

      result mustNot (
        include(Messages("sa.heading.status")) and
          include(Messages("sa.heading.date")) and
          include(Messages("sa.heading.subject")) and
          include(Messages("withdrawn")) and
          include(Messages("unread"))
      )
    }
    SharedMetricRegistries.clear()

  }

  "Calling inbox-link with query params" must {

    implicit val messages: Messages = messagesInEnglish()

    val inboxLinkResults = new TestListPartialHtml {
      override def messageHeaders: Seq[MessageListItem] =
        Seq(
          MessageListItem(
            "",
            "Leaving self assessment",
            LocalDate.parse("2014-08-14"),
            readTime = None,
            sentInError = false
          )
        )
      when(
        messageConnectorMock.messageCount(any(), any[List[String]], any[List[String]])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(MessageCount(1)))
    }

    "generate the link with correct message inbox url" in {
      implicit val request: Request[_] =
        FakeRequest("GET", "/messages/inbox-link?messagesInboxUrl=/personal-account/messages")

      val page = Jsoup.parse(
        inboxLinkResults.asLinkHtml(request.getQueryString("messagesInboxUrl").get, List()).futureValue.body
      )
      page.getElementsByTag("a").first.attr("href") must be("/personal-account/messages")
    }

    "display the correct html content" in {
      implicit val request: Request[_] =
        FakeRequest("GET", "/messages/inbox-link?messagesInboxUrl=/personal-account/messages")

      inboxLinkResults.asLinkHtml(request.getQueryString("messagesInboxUrl").get, List()).futureValue.body must (
        include(Messages("unread.lowercase")) and
          include(Messages("sa.header")) and
          include(Messages("go.inbox"))
      )
    }

    "display the correct content in welsh" in {

      val cyMessages = messagesInWelsh()

      implicit val req: Request[_] =
        FakeRequest("GET", "/messages/inbox-link?messagesInboxUrl=/personal-account/messages").withHeaders(
          (HeaderNames.ACCEPT_LANGUAGE, "cy")
        )

      val result = inboxLinkResults
        .asLinkHtml("/messages/inbox-link?messagesInboxUrl=/personal-account/messages", List())(hc, req, ec, cyMessages)
        .futureValue
        .body

      result must (
        include(cyMessages("sa.header")) and
          include(cyMessages("unread.lowercase")) and
          include(cyMessages("go.inbox"))
      )

      result mustNot (
        include(Messages("sa.header")) and
          include(Messages("unread.lowercase")) and
          include(">" + Messages("go.inbox"))
      )
    }
    SharedMetricRegistries.clear()

  }

  "getEnrolmentIdentifier" must {

    val taxIdentifierResults = new TestListPartialHtml {
      override def messageHeaders: Seq[MessageListItem] = ???
    }

    "return an optional enrolment identifier value when presented with an enrolment key which is present in the provided enrolment set" in {
      taxIdentifierResults.getEnrolmentIdentifier("IR-SA", enrolmentsWithSaNinoCt) must be(Some("123456789"))
    }

    "return None when presented with an enrolment key which isn't present in the provided set of enrolments" in {
      taxIdentifierResults.getEnrolmentIdentifier("IR-SA", enrolmentsWithNino) must be(None)
    }
  }

  "The Tax Identifiers Partial" must {
    val testNino = "AA123456A"
    val testSaUtr = "123456789"
    val testCtUtr = "987654321"

    def getFirstTaxIdentifiersFromPartial(body: String): String =
      getTaxIdentifierFromRow(getTaxIdentifierRows(body).first())

    def getTaxIdentifierRows(body: String): Elements =
      Jsoup.parse(body).getElementsByClass("tabular-data__entry")

    def getTaxIdentifierFromRow(row: Element): String =
      row.getElementsByClass("tabular-data__data-1").first().html()

    def getIdentifierDescriptionFromRow(row: Element): String =
      row.getElementsByClass("tabular-data__heading").first().html()

    implicit val messages: Messages = messagesInEnglish()

    val taxIdentifierResults = new TestListPartialHtml {
      override def messageHeaders: Seq[MessageListItem] = ???
    }

    "Only display nino details when filtered for them, even though the auth context contains more" in {
      getFirstTaxIdentifiersFromPartial(
        taxIdentifierResults.taxIdentifiersPartial(enrolmentsWithSaNinoCt, List("nino")).body
      ) must be(testNino)
    }

    "Only display sa utr details when filtered for them, even though the auth context contains more" in {
      getFirstTaxIdentifiersFromPartial(
        taxIdentifierResults.taxIdentifiersPartial(enrolmentsWithSaNinoCt, List("sautr")).body
      ) must be(testSaUtr)
    }

    "Only display ct utr details when filtered for them, even though the auth context contains more" in {
      getFirstTaxIdentifiersFromPartial(
        taxIdentifierResults.taxIdentifiersPartial(enrolmentsWithSaNinoCt, List("ctutr")).body
      ) must be(testCtUtr)
    }

    "If a nino is asked for but not present it must not display that line" in {
      val rows =
        getTaxIdentifierRows(taxIdentifierResults.taxIdentifiersPartial(enrolmentsWithSa, List("nino", "sautr")).body)
      rows.size() must be(1)
      getTaxIdentifierFromRow(rows.first()) must be(testSaUtr)
    }

    "display the tax identifiers in the correct order" in {

      implicit val messages: Messages = messagesInEnglish()

      val rows = getTaxIdentifierRows(
        taxIdentifierResults.taxIdentifiersPartial(enrolmentsWithSaNinoCt, List("nino", "sautr", "ctutr")).body
      )
      rows.size() must be(3)
      getIdentifierDescriptionFromRow(rows.get(0)) must be(s"${Messages("tax.identifiers.sautr")}:")
      getIdentifierDescriptionFromRow(rows.get(1)) must be(s"${Messages("tax.identifiers.nino")}:")
      getIdentifierDescriptionFromRow(rows.get(2)) must be(s"${Messages("tax.identifiers.ctutr")}:")
    }

    "display the tax identifiers in welsh" in {

      val cyMessages = messagesInWelsh()

      def getHasMessages(body: String): String =
        Jsoup.parse(body).getElementsByClass("has-messages").first().getElementsByTag("p").first().html()

      val partial =
        taxIdentifierResults
          .taxIdentifiersPartial(enrolmentsWithSaNinoCt, List("nino", "sautr", "ctutr"))(hc, welshRequest, cyMessages)
          .body
      getHasMessages(partial) must be(s"${cyMessages("tax.identifiers.heading")}:")

      val rows = getTaxIdentifierRows(partial)

      rows.size() must be(3)
      getIdentifierDescriptionFromRow(rows.get(0)) must be(s"${cyMessages("tax.identifiers.sautr")}:")
      getIdentifierDescriptionFromRow(rows.get(1)) must be(s"${cyMessages("tax.identifiers.nino")}:")
      getIdentifierDescriptionFromRow(rows.get(2)) must be(s"${cyMessages("tax.identifiers.ctutr")}:")
    }
    SharedMetricRegistries.clear()

  }
}
