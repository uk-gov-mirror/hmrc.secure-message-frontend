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

package controllers.messageFrontend

import com.codahale.metrics.SharedMetricRegistries
import controllers.messageFrontend.RendererHandler.MarkAsRead
import controllers.messageFrontend.{ MessageFrontEndController, RendererHandler }
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.twirl.api.Html
import uk.gov.hmrc.play.partials.HtmlPartial

import scala.concurrent.Promise

class MessagesFrontendControllerSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite {

  trait Setup {
    val controller = app.injector.instanceOf[MessageFrontEndController]
  }

  val htmlString =
    """<article class=\"content__body\">\n    \n    <h1 class=\"heading-xlarge\">Your online tax letters</h1>
      |<p class=\"message_time faded-text--small\">This message was sent to you on 28 May 2021</p>
      |<br>
      |<p>Now you have signed up to online tax letters, we want to tell you more.</p>\n
      |<h2>Our promise to you</h2>\n<p>We will:</p>\n
      |<ul class=\"bullets\">\n
      |<li>send you key online tax letters, such as if you have paid too little or too much tax or,
      |if you are a Self Assessment customer, when your return is due</li>
      |\n
      |<li>email to let you know whenever you have a new letter to read</li>\n</ul>\n
      |<h2>You will never miss a tax letter</h2>\n
      |<p>We will write to you by post if:</p>\n
      |<ul class=\"bullets\">\n
      |<li>an email notification ever bounces (we send an email, but it does not reach you and is returned to us)</li>
      |\n<li>you have not actioned something important</li>\n
      |</ul>\n
      |<p>\nThis service is still growing, so at the moment we only send some tax letters online.
      |This is changing so you can be confident all your important tax letters will be in one place.
      |\nYou can also print them at any time, so you have a paper copy if you ever need one.</p>\n
      |<h2>To read your tax letters</h2>\n
      |<p>You need to:</p>
      |\n<ul class=\"bullets\">\n<li>sign in to HMRC services and select 'Messages'</li>\n
      |<h1>asdsdasuasdsdasdkj</h1>
      |<li>you can also use HMRC's free mobile app. Search for 'HMRC app' to download it</li>\n
      |</ul>\n\n\n</article>""".stripMargin

  "Renderer Handler" must {
    "call markAsRead if available" in new Setup {
      val markAsReadVerification = Promise[String]()
      val markAsRead: MarkAsRead = Some(() => markAsReadVerification.success("Success"))

      val result = RendererHandler.toResult(markAsRead)(HtmlPartial.Success(None, Html(htmlString)))

      result.header.status mustBe 200
      result.header.headers("X-Title") mustBe "Your%20online%20tax%20letters"
      markAsReadVerification.future.futureValue mustBe "Success"
      SharedMetricRegistries.clear()
    }
  }
}
