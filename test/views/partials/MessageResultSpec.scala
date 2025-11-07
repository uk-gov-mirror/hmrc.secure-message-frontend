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

package views.partials

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import play.twirl.api.HtmlFormat
import views.html.partials.messageResult

class MessageResultSpec extends PlaySpec with GuiceOneAppPerSuite {

  "view" should {
    "display the correct contents" in {
      implicit val messages: Messages = stubMessages()
      val messageInboxUrl = "test_inbox_url"

      val view: HtmlFormat.Appendable = app.injector.instanceOf[messageResult].apply(messageInboxUrl)

      val viewBody: String = view.body

      assert(viewBody.contains("confirmation.what.next"))
      assert(viewBody.contains("confirmation.step1"))
      assert(viewBody.contains("confirmation.step2"))
      assert(viewBody.contains("confirmation.back"))
    }
  }
}
