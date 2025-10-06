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

package views.helpers

import com.typesafe.config.ConfigFactory
import config.AppConfig
import helpers.LanguageHelper
import model.{ Encoder, EncryptAndEncode, MessageListItem }
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.i18n.{ Lang, Messages }
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto

import java.time.LocalDate
import javax.inject.Inject

class DateFormatWelshSpec @Inject (configuration: Configuration)
    extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with LanguageHelper {

  implicit val messages: Messages = messagesInWelsh()
  implicit val lang: Lang = langCy
  lazy val applicationCrypto = app.injector.instanceOf[ApplicationCrypto]
  val encryptAndEncode = new EncryptAndEncode(applicationCrypto) {
    override lazy val encoder: Encoder = new Encoder {
      override def encryptAndEncode(value: String) = s"encoded(encrypted($value))"
    }
  }
  "Check month format in Welsh for PTA messages" must {

    class TestConfig(configuration: Configuration) extends AppConfig(configuration) {
      override val btaHost: String = ""
      override val btaBaseUrl: String = ""
      override val ptaHost: String = ""
      override val ptaBaseUrl: String = ""

      override def getPortalPath(pathKey: String): String = ""
    }
    val testUrlBuilder = new PortalUrlBuilder(new TestConfig(configuration))

    "return 'Ionawr' for 'January' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-01-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Ionawr 2022"))
    }
    "return 'Chwefror' for 'February' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-02-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Chwefror 2022"))
    }
    "return 'Mawrth' for 'March' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-03-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Mawrth 2022"))
    }
    "return 'Ebrill' for 'April' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-04-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Ebrill 2022"))
    }
    "return 'Mai' for 'May' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-05-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Mai 2022"))
    }
    "return 'Mehefin' for 'June' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-06-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Mehefin 2022"))
    }
    "return 'Gorffennaf' for 'July' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-07-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Gorffennaf 2022"))
    }
    "return 'Awst' for 'August' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-08-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Awst 2022"))
    }
    "return 'Medi' for 'September' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-09-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Medi 2022"))
    }
    "return 'Hydref' for 'October' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-10-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Hydref 2022"))
    }
    "return 'Tachwedd' for 'November' " in {
      val html = views.html.list_partial(
        "/somePtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-11-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Tachwedd 2022"))
    }
    "return 'Rhagfyr' for 'December' " in {
      val html = views.html.list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-12-01"),
            readTime = None,
            sentInError = false
          )
        ),
        testUrlBuilder,
        Some("someSaUtr"),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Rhagfyr 2022"))
    }

  }

  "Check month format in Welsh for BTA messages" must {
    "return 'Ionawr' for 'January' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-01-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Ionawr 2022"))
    }
    "return 'Chwefror' for 'February' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-02-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Chwefror 2022"))
    }
    "return 'Mawrth' for 'March' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-03-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Mawrth 2022"))
    }
    "return 'Ebrill' for 'April' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-04-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Ebrill 2022"))
    }
    "return 'Mai' for 'May' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-05-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Mai 2022"))
    }
    "return 'Mehefin' for 'June' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-06-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Mehefin 2022"))
    }
    "return 'Gorffennaf' for 'July' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-07-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Gorffennaf 2022"))
    }
    "return 'Awst' for 'August' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-08-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Awst 2022"))
    }
    "return 'Medi' for 'September' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-09-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Medi 2022"))
    }
    "return 'Hydref' for 'October' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-10-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Hydref 2022"))
    }
    "return 'Tachwedd' for 'November' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-11-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Tachwedd 2022"))
    }
    "return 'Rhagfyr' for 'December' " in {
      val html = views.html.bta_list_partial(
        "/someBtaBaseUrl",
        Seq(
          MessageListItem(
            id = "",
            subject = "",
            validFrom = LocalDate.parse("2022-12-01"),
            readTime = None,
            sentInError = false
          )
        ),
        Html(""),
        encryptAndEncode
      )
      html.body must (include("1 Rhagfyr 2022"))
    }

  }

}
