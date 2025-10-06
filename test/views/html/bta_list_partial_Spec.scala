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

package views.html

import com.typesafe.config.ConfigFactory
import helpers.LanguageHelper
import model.{ Encoder, EncryptAndEncode, MessageListItem }
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{ Lang, Messages }
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto

import java.time.{ Instant, LocalDate }

class bta_list_partial_Spec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with LanguageHelper {

  implicit val messages: Messages = messagesInEnglish()
  implicit val lang: Lang = langEn
  lazy val applicationCrypto = app.injector.instanceOf[ApplicationCrypto]
  val encryptAndEncode = new EncryptAndEncode(applicationCrypto) {
    override lazy val encoder: Encoder = new Encoder {
      override def encryptAndEncode(value: String) = s"encoded(encrypted($value))"
    }
  }
  "bta_list_partial" should {

    val Unread = None
    val Read = Some(Instant.now)

    "generate all field for unread message" in {
      val html = views.html.bta_list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "Leaving self assessment",
            validFrom = LocalDate.parse("2014-08-14"),
            readTime = Unread,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )

      html.body must (
        include("Unread") and
          include("Leaving self assessment") and
          include("14 August 2014")
      )
    }

    "include relevant class for unread message" in {
      val html = views.html.bta_list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "Leaving self assessment",
            validFrom = LocalDate.parse("2014-08-14"),
            readTime = Unread,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )

      html.body must (
        include("marker-column__marker") and
          include("visuallyhidden") and
          include("bold")
      )
    }

    "generate all field for read message" in {
      val html = views.html.bta_list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "Leaving self assessment",
            validFrom = LocalDate.parse("2014-08-14"),
            readTime = Read,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )

      html.body must not include "Unread"
      html.body must include("Read")
      html.body must include("""<caption class="govuk-visually-hidden">Messages</caption>""")
    }
  }
}
