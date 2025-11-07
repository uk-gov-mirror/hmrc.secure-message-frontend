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
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.{ Html, HtmlFormat }
import views.html.partials.messageReply
import views.viewmodels.MessageReply
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.Helpers.stubMessages

class MessageReplySpec extends PlaySpec with GuiceOneAppPerSuite {

  "view" should {
    "display the correct contents" when {

      "there is no form error" in {
        implicit val messages: Messages = stubMessages()
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest("GET", "/test/path").withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

        val msgReplyModel = MessageReply(
          showReplyForm = true,
          replyFormUrl = "test_url",
          replyIcon = Html("test_content"),
          formErrors = Seq(),
          content = "test_content"
        )

        val view: HtmlFormat.Appendable = app.injector.instanceOf[messageReply].apply(msgReplyModel)
        val viewBody: String = view.body

        assert(viewBody.contains("test_url"))
        assert(viewBody.contains("test_content"))
        assert(viewBody.contains("conversation.reply.form.heading"))
        assert(viewBody.contains("conversation.reply.form.send.button"))
      }

      "there is a form error" in {
        implicit val messages: Messages = stubMessages()
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest("GET", "/test/path").withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

        val msgReplyModel = MessageReply(
          showReplyForm = true,
          replyFormUrl = "test_url",
          replyIcon = Html("test_content"),
          formErrors = Seq(FormError("value", List("conversation.reply.form.exceeded.length"), Seq.empty)),
          content = "test_content"
        )

        val view: HtmlFormat.Appendable = app.injector.instanceOf[messageReply].apply(msgReplyModel)
        val viewBody: String = view.body

        assert(viewBody.contains("test_url"))
        assert(viewBody.contains("test_content"))
        assert(viewBody.contains("conversation.reply.form.heading"))
        assert(viewBody.contains("conversation.reply.form.send.button"))
        assert(viewBody.contains("Error:"))
        assert(viewBody.contains("conversation.reply.form.exceeded.length"))
      }
    }
  }
}
