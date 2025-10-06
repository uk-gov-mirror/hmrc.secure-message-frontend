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

package model

import com.codahale.metrics.SharedMetricRegistries
import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto

class MessageHeaderSpec extends PlaySpec {

  "EncryptAndEncode" should {
    "generate a url with a encrypted and base64 encoded message readTimeUrl" in new TestCase {
      val encryptedUrl: String = encryptAndEncode.generateEncryptedUrl("read/time/url", "some-base")
      encryptedUrl must startWith("encoded(encrypted(read/time/url::link")
    }

    "generate a url with a returnURL which is itself and encrypted link back to the base url with form as the step" in new TestCase {
      val encryptedUrl: String = encryptAndEncode.generateEncryptedUrl("read/time/url", "/some-base")
      val returnUrl = extractReturnUrlFromThreePartEncryption(encryptedUrl)

      returnUrl must be(
        "/some-base/messages/encoded(encrypted(read/time/url::form::/some-base/messages/encoded(encrypted(read/time/url::ack))))"
      )
      SharedMetricRegistries.clear

    }
  }

  trait TestCase {
    lazy val applicationCrypto = new ApplicationCrypto(Configuration(ConfigFactory.load()))
    val encryptAndEncode = new EncryptAndEncode(applicationCrypto) {
      override lazy val encoder: Encoder = (value: String) => s"encoded(encrypted($value))"
    }

    def extractReturnUrlFromThreePartEncryption(encryptedUrl: String): String =
      encryptedUrl.substring(encryptedUrl.indexOf("::", encryptedUrl.indexOf("::") + 2) + 2, encryptedUrl.length - 2)
  }
}
