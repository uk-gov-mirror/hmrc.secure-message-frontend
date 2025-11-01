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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsResultException, Json }

import java.time.format.DateTimeFormatter
import java.time.{ Instant, LocalDate, LocalDateTime }
import java.util.Locale

class MessageListItemSpec extends PlaySpec {

  "messageListItemFormat" should {

    import MessageListItem.messageListItemFormat

    "read the json correctly" in new Setup {
      Json
        .parse(messageListItemJsonStringWithCustomReadTimeInstant)
        .as[MessageListItem] mustBe messageListItem.copy(readTime = None)
    }

    "throw exception while reading the invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(messageListItemJsonStringInvalid).as[MessageListItem]
      }
    }

    "write the object correctly" in new Setup {
      Json.toJson(messageListItem) mustBe Json.parse(messageListItemJsonStringWithDefaultReadTimeInstant)
    }
  }

  "lang" should {

    "return the correct language value" in new Setup {
      messageListItem.copy(language = Some("cy")).lang mustBe "cy"
      messageListItem.lang mustBe "en"
    }
  }

  trait Setup {
    val year = 2025
    val month = 11
    val day = 1

    val localDate: LocalDate = LocalDate.of(year, month, day)
    val readTime: Instant = Instant.parse("2025-11-01T23:30:00Z")
    val readTimeInstantForCustomReads: Instant = Instant.parse("+643699-01-11T15:50:00Z")

    val taxpayerName: TaxpayerName = TaxpayerName(
      title = Some("test_title"),
      forename = Some("test_forename"),
      secondForename = Some("test_second_fore_name"),
      surname = Some("test_surname"),
      honours = Some("test_honours"),
      line1 = Some("test_line1"),
      line2 = Some("test_line2")
    )

    val messageListItem: MessageListItem = MessageListItem(
      id = "test_id",
      subject = "Test_subject",
      validFrom = localDate,
      taxpayerName = Some(taxpayerName),
      readTime = Some(readTime),
      sentInError = false,
      replyTo = Some("test_address"),
      messageDesc = None,
      counter = Some(1),
      language = None
    )

    val messageListItemJsonStringWithDefaultReadTimeInstant: String =
      s"""{
         |"id":"test_id",
         |"subject":"Test_subject",
         |"validFrom":"2025-11-01",
         |"taxpayerName":{"title":"test_title",
         |"forename":"test_forename",
         |"secondForename":"test_second_fore_name",
         |"surname":"test_surname",
         |"honours":"test_honours",
         |"line1":"test_line1",
         |"line2":"test_line2"},
         |"readTime":"2025-11-01T23:30:00Z",
         |"sentInError":false,
         |"replyTo":"test_address",
         |"counter":1
         |}""".stripMargin

    val messageListItemJsonStringWithCustomReadTimeInstant: String =
      s"""{
         |"id":"test_id",
         |"subject":"Test_subject",
         |"validFrom":"2025-11-01",
         |"taxpayerName":{
         |"title":"test_title",
         |"forename":"test_forename",
         |"secondForename":"test_second_fore_name",
         |"surname":"test_surname",
         |"honours":"test_honours",
         |"line1":"test_line1",
         |"line2":"test_line2"
         |},
         |"sentInError":false,
         |"replyTo":"test_address",
         |"counter":1
         |}""".stripMargin

    val messageListItemJsonStringInvalid: String =
      s"""{
         |"id":"test_id",
         |"validFrom":"2025-11-01",
         |"taxpayerName":{
         |"title":"test_title",
         |"forename":"test_forename",
         |"secondForename":"test_second_fore_name",
         |"surname":"test_surname",
         |"honours":"test_honours",
         |"line1":"test_line1",
         |"line2":"test_line2"
         |},
         |"sentInError":false,
         |"replyTo":"test_address",
         |"counter":1
         |}""".stripMargin
  }
}
