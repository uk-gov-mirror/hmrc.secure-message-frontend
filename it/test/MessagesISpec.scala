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
import model.{ EPaye, MessageCount, ReadPreference }
import org.scalatest.Inspectors
import org.scalatest.time.{ Millis, Span }
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers.*
import test.models.HmrcPodsPpOrg
import test.utils.AuthorityBuilder
import uk.gov.hmrc.domain.{ CtUtr, HmrcObtdsOrg, Nino, SaUtr }
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class MessagesISpec extends MessageFrontendISpec with Inspectors {

  override implicit def patienceConfig: PatienceConfig = {
    val duration: Int = 750
    val spanTime = 50
    PatienceConfig(timeout = scaled(Span(duration, Millis)), interval = scaled(Span(spanTime, Millis)))
  }

  "Message Count" should {

    "return 0 for non SA users (ct-utr)" in new TestCase {
      val utr = SaUtr("UNUSED")
      val authProvider = testAuthorisationProvider.governmentGatewayAuthority()

      val request = messagesCount(Some(ReadPreference.No))
        .withSession(SessionKeys.authToken -> authProvider.bearerTokenHeader()._2)
      val result = route(app, request).get
      status(result) must be(Status.OK)
      contentAsJson(result).as[MessageCount] must be(MessageCount(0))

    }

    "return count as 1 for an authenticated nino-only user with 1 message" in new TestCase {
      val utr = SaUtr("UNUSED")
      val nino = Nino("NH123456D")

      lazy val authBuilderNino = testAuthorisationProvider.governmentGatewayAuthority().withNino(nino)

      messagesPost(ninoMessage(nino))

      val request = messagesCount(None, List("nino"))
        .withSession(SessionKeys.authToken -> authBuilderNino.bearerTokenHeader()._2)
      val result: Future[Result] = route(app, request).get

      eventually {
        status(result) must be(Status.OK)
        contentAsJson(result).as[MessageCount] must be(MessageCount(1))
      }
    }

    "not throw a missing bearer token or session record not found exception" in new TestCase {
      val utr = SaUtr("UNUSED")
      val request = messagesCount(None)
      val result = route(app, request).get
      status(result) must be(Status.UNAUTHORIZED)
    }

    "return count as 1 for an authenticated user with nino, ct utr & sa utr, with 1 message of each type while asking for only nino messages" in new TestCase {
      val utr = SaUtr("UNUSED")

      lazy val authProvider: AuthorityBuilder = setupFilterableMessages._1

      val request = messagesCount(None, List("nino"))
        .withSession(SessionKeys.authToken -> authProvider.bearerTokenHeader()._2)

      val result = route(app, request).get
      status(result) must be(Status.OK)
      contentAsJson(result).as[MessageCount] must be(MessageCount(1))
    }

    "return count as 1 for an authenticated user with nino, ct utr & sa utr, with 1 message of each type while asking for only sautr messages" in new TestCase {
      val utr = SaUtr("UNUSED")

      lazy val authProvider: AuthorityBuilder = setupFilterableMessages._1

      val request = messagesCount(None, List("sautr"))
        .withSession(SessionKeys.authToken -> authProvider.bearerTokenHeader()._2)

      val result = route(app, request).get
      status(result) must be(Status.OK)
      contentAsJson(result).as[MessageCount] must be(MessageCount(1))
    }

    "return count as 2 for an authenticated user with nino, ct utr & sa utr, " +
      "with 1 message of each type while asking for nino & sautr messages" in new TestCase {
        val utr = SaUtr("UNUSED")

        lazy val authProvider = setupFilterableMessages._1

        val request = messagesCount(None, List("nino", "sautr"))
          .withSession(
            SessionKeys.authToken -> authProvider.bearerTokenHeader()._2
          )
        val result = route(app, request).get
        status(result) must be(Status.OK)
        contentAsJson(result).as[MessageCount] must be(MessageCount(2))
      }

    "return count as 3 for an authenticated user with nino, ct utr & sa utr, with 1 message of each type while asking for all messages" in new TestCase {
      override def utr: SaUtr = SaUtr("123456789")

      lazy val authProvider = setupFilterableMessages._1

      val request = messagesCount(None, List("nino", "ctutr", "sautr"))
        .withSession(
          authProvider.bearerTokenHeader(),
          authProvider.sessionCookie(authProvider.bearerTokenHeader()._2),
          SessionKeys.authToken -> authProvider.bearerTokenHeader()._2
        )
      val result = route(app, request).get
      status(result) must be(Status.OK)
      contentAsJson(result).as[MessageCount] must be(MessageCount(3))
    }
  }

  "Message List" should {
    "return 1 message for nino-only user" in new TestCase {
      val utr = SaUtr("UNUSED")

      val (authContext, _, _, _, _, _, _, _) = setupFilterableMessages

      val request = messages()
        .withSession(SessionKeys.authToken -> authContext.bearerTokenHeader()._2)
      val result: Future[Result] = route(app, request).get

      status(result) must be(Status.OK)
      expectedMessages(contentAsString(result), 1)
    }

    "return no messages for ct-utr user" in new TestCase {
      val utr = SaUtr("UNUSED")
      val ctUtr = CtUtr("9874923499")
      lazy val authProvider = testAuthorisationProvider.governmentGatewayAuthority().withCtUtr(ctUtr)
      lazy val authHeader = authProvider.bearerTokenHeader()

      val request = messages()
        .withSession(SessionKeys.authToken -> authHeader._2)
      val result: Future[Result] = route(app, request).get

      status(result) must be(Status.OK)
      contentAsString(result) must include("no-messages")
    }

    "return all messages user using /messages endpoint for sa-utr user" in new AuthenticatedUserMessageCount {
      messagesPost(refundMessage)

      val bt = authBuilder.bearerTokenHeader()
      val request = messages()
        .withSession(SessionKeys.authToken -> bt._2)
      val result: Future[Result] = route(app, request).get

      status(result) must be(Status.OK)
      expectedMessages(contentAsString(result), 1)
    }

    "return all messages using /messages endpoint for ctUtr user" in new AuthenticatedUserMessageCount {
      val ctUtr = CtUtr("9874923499")
      messagesPost(tavcMessage(ctUtr))

      val authBuilderForCtUtr = testAuthorisationProvider.governmentGatewayAuthority().withCtUtr(ctUtr)

      val request = messages(List("ctutr"))
        .withSession(SessionKeys.authToken -> authBuilderForCtUtr.bearerTokenHeader()._2)
      val result: Future[Result] = route(app, request).get

      status(result) must be(Status.OK)
      expectedMessages(contentAsString(result), 1)
    }

    "return only nino messages when filtering the /messages endpoint" in new AuthenticatedUserMessageCount {
      val (authProvider, nino, _, _, _, _, _, _) = setupFilterableMessages

      val request = messages(List("nino"))
        .withSession(SessionKeys.authToken -> authProvider.bearerTokenHeader()._2)

      val result: Future[Result] = route(app, request).get

      status(result) must be(Status.OK)
      expectedMessages(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"$b1${b2}Here is the subject for $nino$a$a")
    }

    "return only sautr messages when filtering the /messages endpoint" in new AuthenticatedUserMessageCount {
      val (authContext, _, _, _, _, _, _, _) = setupFilterableMessages

      val request = messages(List("sautr"))
        .withSession(SessionKeys.authToken -> authContext.bearerTokenHeader()._2)
      val result: Future[Result] = route(app, request).get

      status(result) must be(Status.OK)

      expectedMessages(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"$b1${b2}Here is the subject for ${utr.value}$a$a")
    }

    "return only ctutr messages when filtering the /messages endpoint" in new AuthenticatedUserMessageCount {
      val (_, _, ctUtr, _, _, _, _, _) = setupFilterableMessages

      val authBuilderForCtUtr = testAuthorisationProvider.governmentGatewayAuthority().withCtUtr(CtUtr(ctUtr))

      val request = messages(List("ctutr"))
        .withSession(SessionKeys.authToken -> authBuilderForCtUtr.bearerTokenHeader()._2)
      val result: Future[Result] = route(app, request).get

      status(result) must be(Status.OK)
      expectedMessages(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"$b1${b2}Here is the subject for $ctUtr$a$a")
    }

    "return all enrolments messages when a filter isn't provided on the /messages endpoint" in new AuthenticatedUserMessageCount {
      val (authContext, nino, _, fhdds, vat, _, _, _) = setupFilterableMessages

      val request = messages()
        .withSession(SessionKeys.authToken -> authContext.bearerTokenHeader()._2)
      val result: Future[Result] = route(app, request).get

      status(result) must be(Status.OK)

      val idHeadings = Set(
        ("Self Assessment UTR:", utr.value),
        ("National Insurance number:", nino)
      )

      hasMessageSubHeading(contentAsString(result)) must be(true)
      headingIdentifiers(contentAsString(result)) must be(idHeadings)
      val countVal: Int = 8
      exactMessageCount(contentAsString(result), countVal)
      val actualSubjects = emailMessagesSubject(contentAsString(result))
      actualSubjects must contain(s"$b1${b2}Here is the subject for $nino$a$a")
      actualSubjects must contain(s"$b1${b2}Here is the subject for ${utr.value}$a$a")
      emailMessagesSubject(contentAsString(result)) must contain(s"$b1${b2}Here is the subject for $fhdds$a$a")
      emailMessagesSubject(contentAsString(result)) must contain(s"$b1${b2}Here is the VAT subject for $vat$a$a")
    }

    "return only nino messages when filtering the /messages/bta endpoint" in new AuthenticatedUserMessageCount {
      val (authContext, nino, _, _, _, _, _, _) = setupFilterableMessages
      val request = messagesBta(List("nino"))
        .withSession(
          authContext.bearerTokenHeader(),
          authContext.sessionCookie(authContext.bearerTokenHeader()._2),
          SessionKeys.authToken -> authContext.bearerTokenHeader()._2
        )
      val result = route(app, request).get
      status(result) must be(Status.OK)
      exactMessageCount(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for $nino$a")
    }

    "return only FHDDS messages when filtering the /messages/bta endpoint" in new AuthenticatedUserMessageCount {
      val (authContext, _, _, fhdds, _, _, _, _) = setupFilterableMessages
      val request = messagesBta(List("HMRC-OBTDS-ORG"))
        .withSession(
          authContext.bearerTokenHeader(),
          authContext.sessionCookie(authContext.bearerTokenHeader()._2),
          SessionKeys.authToken -> authContext.bearerTokenHeader()._2
        )
      val result = route(app, request).get
      status(result) must be(Status.OK)
      exactMessageCount(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for $fhdds$a")
      hasMessageSubHeading(contentAsString(result)) must be(false)
    }

    "return only PPT messages when filtering the /messages/bta endpoint" in new AuthenticatedUserMessageCount {
      val (authContext, _, _, _, _, ppt, _, _) = setupFilterableMessages
      val request = messagesBta(List("ETMPREGISTRATIONNUMBER"), List("ppt"))
        .withSession(
          authContext.bearerTokenHeader(),
          authContext.sessionCookie(authContext.bearerTokenHeader()._2),
          SessionKeys.authToken -> authContext.bearerTokenHeader()._2
        )
      val result = route(app, request).get
      status(result) must be(Status.OK)
      exactMessageCount(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for $ppt$a")
      hasMessageSubHeading(contentAsString(result)) must be(false)
    }
    // Ignored due to DC-4366
    "return PODS regime and PSAID taxIdentifier messages only when filtering the /messages/bta endpoint" ignore new AuthenticatedUserMessageCount {
      val (authContext, _, _, _, _, _, pods, _) = setupFilterableMessages

      val request = messagesBta(List("PSAID"), List("pods"))
        .withSession(
          authContext.bearerTokenHeader(),
          authContext.sessionCookie(authContext.bearerTokenHeader()._2),
          SessionKeys.authToken -> authContext.bearerTokenHeader()._2
        )
      val result = route(app, request).get
      status(result) must be(Status.OK)
      exactMessageCount(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for $pods$a")
      hasMessageSubHeading(contentAsString(result)) must be(false)
    }
    // Ignored due to DC-4366
    "return PODS regime and PSPID taxIdentifier messages when filtering the /messages/bta endpoint" ignore new AuthenticatedUserMessageCount {
      val authContext = testAuthorisationProvider
        .governmentGatewayAuthority()
        .withNino(Nino("NH123456D"))
        .withPodsPp(HmrcPodsPpOrg("A12345678"))

      val request = messagesBta(List("PSPID"), List("pods"))
        .withSession(
          authContext.bearerTokenHeader(),
          authContext.sessionCookie(authContext.bearerTokenHeader()._2),
          SessionKeys.authToken -> authContext.bearerTokenHeader()._2
        )
      val result = route(app, request).get
      status(result) must be(Status.OK)
      exactMessageCount(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for A12345678$a")
      hasMessageSubHeading(contentAsString(result)) must be(false)
    }

    "return only VAT messages when filtering the /messages/bta endpoint" in new AuthenticatedUserMessageCount {
      val (authContext, _, _, _, vat, _, _, _) = setupFilterableMessages
      val request = messagesBta(List("HMRC-MTD-VAT"))
        .withSession(
          authContext.bearerTokenHeader(),
          authContext.sessionCookie(authContext.bearerTokenHeader()._2),
          SessionKeys.authToken -> authContext.bearerTokenHeader()._2
        )
      val result = route(app, request).get
      status(result) must be(Status.OK)
      exactMessageCount(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the VAT subject for $vat$a")
      hasMessageSubHeading(contentAsString(result)) must be(false)
    }

    "not display identifiers heading if the only enrolment available is FHDDS on the /messages/bta endpoint" in new AuthenticatedUserMessageCount {
      val fhdds = HmrcObtdsOrg("XZFH00000100024")
      val authContext = testAuthorisationProvider.governmentGatewayAuthority().withFhdds(fhdds)
      messagesPost(fhddsMessage(fhdds))

      val request = messagesBta(List("HMRC-OBTDS-ORG"))
        .withSession(
          authContext.bearerTokenHeader(),
          authContext.sessionCookie(authContext.bearerTokenHeader()._2),
          SessionKeys.authToken -> authContext.bearerTokenHeader()._2
        )
      val result = route(app, request).get
      status(result) must be(Status.OK)
      exactMessageCount(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for ${fhdds.value}$a")
      hasMessageSubHeading(contentAsString(result)) must be(false)
    }

    "return only sautr messages when filtering the /messages/bta endpoint" in new AuthenticatedUserMessageCount {
      val (authContext, _, _, _, _, _, _, _) = setupFilterableMessages
      val request = messagesBta(List("sautr"))
        .withSession(
          authContext.bearerTokenHeader(),
          authContext.sessionCookie(authContext.bearerTokenHeader()._2),
          SessionKeys.authToken -> authContext.bearerTokenHeader()._2
        )

      val result = route(app, request).get
      status(result) must be(Status.OK)
      hasMessageSubHeading(contentAsString(result)) must be(true)
      exactMessageCount(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for ${utr.value}$a")
    }

    "return only IR-PAYE messages when filtering the /messages/bta endpoint" in new AuthenticatedUserMessageCount {
      val epayeId: EPaye = EPaye("840PR12345678")
      val authContext = testAuthorisationProvider.governmentGatewayAuthority().withEPaye(epayeId)
      messagesPost(epayeMessage(epayeId))

      val request = messagesBta(List("EMPREF"), List("epaye"))
        .withSession(
          authContext.bearerTokenHeader(),
          authContext.sessionCookie(authContext.bearerTokenHeader()._2),
          SessionKeys.authToken -> authContext.bearerTokenHeader()._2
        )
      val result = route(app, request).get
      status(result) must be(Status.OK)
      exactMessageCount(contentAsString(result), 1)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for ${epayeId.value}$a")
    }

    "return all enrolments messages when a filter isn't provided on the /messages/bta endpoint" in new AuthenticatedUserMessageCount {
      val (authContext, nino, _, fhdds, vat, _, _, _) = setupFilterableMessages
      val request = messagesBta()
        .withSession(SessionKeys.authToken -> authContext.bearerTokenHeader()._2)
      val result = route(app, request).get
      val idHeadings = Set(
        ("Self Assessment UTR:", utr.value),
        ("National Insurance number:", nino)
      )
      status(result) must be(Status.OK)
      hasMessageSubHeading(contentAsString(result)) must be(true)
      headingIdentifiers(contentAsString(result)) must be(idHeadings)
      val countVal: Int = 4
      exactMessageCount(contentAsString(result), countVal)
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for $nino$a")
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for ${utr.value}$a")
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the subject for $fhdds$a")
      emailMessagesSubject(contentAsString(result)) must contain(s"${bta_b1}Here is the VAT subject for $vat$a")
    }
  }

  trait AuthenticatedUserMessageCount extends TestCase {
    val utr = SaUtr("123456789")
    val b1 = """<span class="underline black-text govuk-body">"""
    val bta_b1 = """<span class="underline black-text govuk-body bold">"""
    val b2 = """<span class="govuk-!-font-weight-bold black-text govuk-body">"""
    val a = """</span>"""
  }
}
