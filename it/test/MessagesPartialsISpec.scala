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

/*
 * Copyright 2023 HM Revenue & Customs
 *
 */
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{ BeforeAndAfterEach, Inspectors }
import play.api.Logging
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.{ route, * }
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.SessionKeys
import play.api.http.{ ContentTypes, HeaderNames }
import play.api.mvc.{ AnyContentAsEmpty, Result }
import test.utils.AuthorityBuilder

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.language.postfixOps

class MessagesPartialsISpec
    extends MessageFrontendISpec with IntegrationPatience with Inspectors with BeforeAndAfterEach with Logging {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(30 seconds), interval = scaled(200 millis))

  override protected def beforeEach(): Unit =
    ws.url(s"${secureMessageResource}test-only/delete/secure-messages")
      .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
      .delete()
      .futureValue

  "Message link" must {
    "successfully view message when step and returnUrl are missing" in new TestCase {

      val utr: SaUtr = SaUtr("1555369043")

      val authProvider: AuthorityBuilder = testAuthorisationProvider.governmentGatewayAuthority().withSaUtr(utr)

      val bt: (String, String) = authProvider.bearerTokenHeader()

      val messageId: String = messagesPost(statementMessage)
      val request: FakeRequest[AnyContentAsEmpty.type] =
        getMessageForEncryptedUrl(encryptSaMessageRendererReadUrl(messageId))
          .withSession(SessionKeys.authToken -> bt._2)

      val result: Future[Result] = route(app, request).get
      status(result) must be(Status.OK)

    }

    "return Forbidden message when encryptedUrl is null" in new TestCase {

      val utr = SaUtr("1555369043")

      val authProvider = testAuthorisationProvider.governmentGatewayAuthority().withSaUtr(utr)

      val bt = authProvider.bearerTokenHeader()

      val request = getMessageForEncryptedUrl(null).withSession(SessionKeys.authToken -> bt._2)

      val result = route(app, request).get
      status(result) must be(BAD_REQUEST)
    }
  }

  "Inbox link partial" must {
    "show message count of one when filtering for nino messages only" in new TestCase {
      val utr = SaUtr("1555369043")

      lazy val authProvider = setupFilterableMessages._1

      val link =
        messagesInboxLink1(
          List("nino"),
          Some(authProvider.bearerTokenHeader()),
          Some(authProvider.sessionCookie(authProvider.bearerTokenHeader()._2))
        )

      link must be(Some("1 unread"))
    }

    "show message count of one when filtering for sa utr messages only" in new TestCase {
      val utr = SaUtr("1555369043")

      lazy val authProvider = setupFilterableMessages._1

      val link =
        messagesInboxLink1(
          List("sautr"),
          Some(authProvider.bearerTokenHeader()),
          Some(authProvider.sessionCookie(authProvider.bearerTokenHeader()._2))
        )

      link must be(Some("1 unread"))
    }

    "show message count of one when filtering for ct utr messages only" in new TestCase {
      val utr = SaUtr("1555369043")

      lazy val authProvider = setupFilterableMessages._1

      val link =
        messagesInboxLink1(
          List("ctutr"),
          Some(authProvider.bearerTokenHeader()),
          Some(authProvider.sessionCookie(authProvider.bearerTokenHeader()._2))
        )
      link must be(Some("1 unread"))
    }
  }

  "Messages partials" should {
    "throw a missing bearer token exception if token missing" in new TestCase {
      val utr = SaUtr("1555369043")
      val request = messages()
      // Testing the controller for exception raised
      // ErrorHandler.resolveError is unit tested
      assertThrows[MissingBearerToken] {
        status(route(app, request).get)
      }
    }

    "return portal messages list and change read count in inbox-link when they are read" in new TestCase {

      val utr = SaUtr("1555369043")

      messagesInboxLink() must be(None)

      messagesPost(statementMessage)
      messagesPost(refundMessage)
      messagesPost(atsMessage)

      val bt = ggAuthorisationHeader

      val (rows, parsedMessages) = renderMessageListPartial(3)
      messagesInboxLink1() must be(Some("3 unread"))

      val idxRange: Seq[Int] = 0 to 2
      forAll(idxRange) { idx =>
        val row = rows.get(idx)
        row.classNames() must contain("unread")
        row.attributes().asList() must not contain "data-sso"

        val href = row.getElementsByTag("a").first().attr("href")

        // Simulate delegation from business tax account:
        // assume that business tax account will delegate to us on a similar URL
        logger.warn(s"Trying link $href")
        val request = FakeRequest("GET", s"/$href").withSession(SessionKeys.authToken -> bt._2)

        val responseFirstTime = route(app, request).get
        status(responseFirstTime) must be(Status.OK)

        val responseNextTime = route(app, request).get
        status(responseNextTime) must be(Status.OK)
      }

      val portalMessagesLink = parsedMessages.getElementsByTag("a").last()
      portalMessagesLink.attr("data-sso") must be("false")

      val (refreshedRows, _) = renderMessageListPartial(2)
      refreshedRows.get(1).classNames() must contain("read")

      messagesInboxLink() must be(None)
    }
  }
}
