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

import org.scalatestplus.play.PlaySpec

import java.time.Instant
import play.api.libs.json.{ JsResultException, JsString, Json }

class MessageHeaderSpec extends PlaySpec {

  "Json Reads" should {
    import MessageHeader.conversationHeaderReads

    "read the json correctly" in new Setup {
      Json.parse(messageHeaderJsonStringForCustomReads).as[MessageHeader] mustBe messageHeaderWithCustomIssueDate
    }

    "throw exception for the invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(messageHeaderJsonInvalidString).as[MessageHeader]
      }
    }
  }

  "Json Writes" should {
    "write the object correctly" in new Setup {
      Json.toJson(messageHeader) mustBe Json.parse(messageHeaderJsonString)
    }
  }

  trait Setup {
    val issueDate: Instant = Instant.parse("2025-10-12T23:30:00Z")
    val issueDateFosCustomReads: Instant = Instant.parse("+643699-01-11T15:50:00Z")

    val issueDateJsonString = "2025-10-12T23:30:00Z"
    val issueDateJsonForCustomRead = "20251012233000000"

    val countFive = 5

    val messageHeader: MessageHeader = MessageHeader(
      messageType = MessageType.Conversation,
      id = "test_id",
      subject = "test_subject",
      issueDate = issueDate,
      senderName = Some("test_sender"),
      unreadMessages = true,
      count = countFive,
      conversationId = Some("test_id"),
      client = Some("test_client")
    )

    val messageHeaderWithCustomIssueDate: MessageHeader = MessageHeader(
      messageType = MessageType.Conversation,
      id = "test_id",
      subject = "test_subject",
      issueDate = issueDateFosCustomReads,
      senderName = Some("test_sender"),
      unreadMessages = true,
      count = countFive,
      conversationId = Some("test_id"),
      client = Some("test_client")
    )

    val messageHeaderJsonString: String =
      """{
        |"messageType":"conversation",
        |"id":"test_id",
        |"subject":"test_subject",
        |"issueDate":"2025-10-12T23:30:00Z",
        |"senderName":"test_sender",
        |"unreadMessages":true,
        |"count":5,
        |"conversationId":"test_id",
        |"client":"test_client"
        |}""".stripMargin

    val messageHeaderJsonStringForCustomReads: String =
      """{
        |"messageType":"conversation",
        |"id":"test_id",
        |"subject":"test_subject",
        |"issueDate":20251012233000000,
        |"senderName":"test_sender",
        |"unreadMessages":true,
        |"count":5,
        |"conversationId":"test_id",
        |"client":"test_client"
        |}""".stripMargin

    val messageHeaderJsonInvalidString: String =
      """{
        |"messageType":"conversation",
        |"id":"test_id",
        |"subject":"test_subject",
        |"senderName":"test_sender",
        |"unreadMessages":true,
        |"conversationId":"test_id",
        |"client":"test_client"
        |}""".stripMargin
  }
}
