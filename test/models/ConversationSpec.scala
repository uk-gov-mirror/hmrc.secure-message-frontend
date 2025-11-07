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

package models

import com.ibm.icu.text.SimpleDateFormat
import org.scalatestplus.play.PlaySpec

import java.time.{ Instant, LocalDate, LocalDateTime }
import play.api.libs.json.{ JsResultException, JsString, Json }
import models.{ Conversation, SenderInformation }
import com.fasterxml.jackson.core.JsonParseException
import helpers.TestData.{ TEST_CLIENT, TEST_CONTENT, TEST_ID, TEST_LANGUAGE_ENGLISH, TEST_NAME, TEST_STATUS, TEST_SUBJECT }

import java.time.format.DateTimeFormatter
import java.util.Date

class ConversationSpec extends PlaySpec {

  "Conversation.conversationFormat" should {
    import Conversation.conversationFormat

    "read the json correctly" in new Setup {
      Json.parse(conversationJsonString).as[Conversation] mustBe conversationObject
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(conversationJsonInvalid).as[Conversation]
      }
    }
  }

  "Message.messageReads" should {
    import Message.messageReads

    "read the json correctly" in new Setup {
      Json.parse(messageJson).as[Message] mustBe message
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(messageJsonInvalid).as[Message]
      }
    }
  }

  "SenderInformation.dateFormat" should {
    import SenderInformation.dateFormat

    "read the json correctly" in new Setup {
      Json.parse(timeInstantString1).as[Instant] mustBe timeInstant1
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsonParseException] {
        Json.parse(timeInstantString).as[Instant]
      }
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(timeInstant) mustBe JsString(timeInstantString)
    }
  }

  "SenderInformation.senderInformationFormat" should {
    import SenderInformation.senderInformationFormat

    "read the json correctly" in new Setup {
      Json.parse(senderInformationJson).as[SenderInformation] mustBe senderInformation
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(senderInformationJsonInvalid).as[SenderInformation]
      }
    }
  }

  "FirstReaderInformation.dateFormat" should {
    import FirstReaderInformation.dateFormat

    "read the json correctly" in new Setup {
      Json.parse(timeInstantString1).as[Instant] mustBe timeInstant1
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsonParseException] {
        Json.parse(timeInstantString).as[Instant]
      }
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(timeInstant) mustBe JsString(timeInstantString)
    }
  }

  "FirstReaderInformation.firstReaderFormat" should {
    import FirstReaderInformation.firstReaderFormat

    "read the json correctly" in new Setup {
      Json.parse(firstReaderInformationJson).as[FirstReaderInformation] mustBe firstReaderInformation
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(firstReaderInformationJsonInvalid).as[FirstReaderInformation]
      }
    }
  }

  trait Setup {
    val timeInstant: Instant = Instant.parse("2025-10-12T23:30:00Z")
    val timeInstant1: Instant = Instant.parse("+643699-01-11T15:50:00Z")
    val timeInstantString: String = "2025-10-12T23:30:00Z"
    val timeInstantString1: String = "20251012233000000"

    val senderInformationJson: String =
      """{
        |"name":"test_name",
        |"sent":20251012233000000,
        |"self":true
        |}""".stripMargin

    val senderInformationJsonInvalid: String =
      """{
        |"name":"test_name",
        |"self":false
        |}""".stripMargin

    val senderInformation: SenderInformation =
      SenderInformation(name = Some(TEST_NAME), sent = timeInstant1, self = true)

    val firstReaderInformationJson: String =
      """{
        |"name":"test_name",
        |"read":20251012233000000
        |}""".stripMargin

    val firstReaderInformationJsonInvalid: String =
      """{
        |"name":"test_name"
        |}""".stripMargin

    val firstReaderInformation: FirstReaderInformation =
      FirstReaderInformation(name = Some(TEST_NAME), read = timeInstant1)

    val messageJson: String = """{
                                |"senderInformation":{"name":"test_name","sent":20251012233000000,"self":true},
                                |"content":"test_content"}""".stripMargin

    val messageJsonInvalid: String =
      """{
        |"content":"test_content"}""".stripMargin

    val message: Message = Message(senderInformation = senderInformation, firstReader = None, content = TEST_CONTENT)

    val conversationJsonString: String =
      s"""{
         |"client":"test_client",
         |"conversationId":"test_id",
         |"status":"test_status",
         |"subject":"test_subject",
         |"language":"English",
         |"messages":[$messageJson]
         |}""".stripMargin

    val conversationJsonInvalid: String = s"""{
                                             |"client":"test_client",
                                             |"conversationId":"test_id",
                                             |"status":"test_status",
                                             |"messages":[$messageJson]
                                             |}""".stripMargin

    val conversationObject: Conversation = Conversation(
      client = TEST_CLIENT,
      conversationId = TEST_ID,
      status = TEST_STATUS,
      tags = None,
      subject = TEST_SUBJECT,
      language = TEST_LANGUAGE_ENGLISH,
      messages = List(message)
    )
  }
}
