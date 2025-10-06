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

package model

import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto

import java.util.Base64
import javax.inject.Inject

trait Encoder {
  def encryptAndEncode(plainText: String): String
}

class EncryptAndEncode @Inject() (val applicationCrypto: ApplicationCrypto) {

  def generateEncryptedUrl(messageId: String, baseUrl: String): String = {
    val returnUrl = {
      val encodedAndEncryptedSecureMessagingAckUrl = encoder.encryptAndEncode(s"$messageId::ack")

      def encodedAndEncryptedSecureMessagingFormUrl(ackReturnUrl: String) =
        encoder.encryptAndEncode(s"$messageId::form::$ackReturnUrl")

      val ackReturnUrl = s"$baseUrl/messages/$encodedAndEncryptedSecureMessagingAckUrl"
      s"$baseUrl/messages/${encodedAndEncryptedSecureMessagingFormUrl(ackReturnUrl)}"
    }

    encoder.encryptAndEncode(s"$messageId::link::$returnUrl")
  }

  lazy val encoder = new Encoder {
    override def encryptAndEncode(v: String) =
      Base64.getEncoder.encodeToString(applicationCrypto.QueryParameterCrypto.encrypt(PlainText(v)).value.getBytes)
  }
}
