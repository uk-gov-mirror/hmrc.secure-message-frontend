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

import com.fasterxml.jackson.core.JsonParseException
import helpers.TestData.{ DAY_30, MONTH_10, TEST_CONTENT, TEST_NAME, TEST_SUBJECT, YEAR_2025 }
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsResultException, JsString, Json }
import views.helpers.DateFormat

import java.time.format.DateTimeFormatter
import java.time.{ Instant, LocalDate, LocalDateTime }

class LetterSpec extends PlaySpec {

  "messageFormat" should {
    import Letter.messageFormat

    "read the json correctly" in new Setup {
      Json.parse(letterJsonStringWithCustomReads).as[Letter] mustBe letterObjectForCustomJsonReads
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(letterJsonStringInvalid).as[Letter]
      }
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(letter) mustBe Json.parse(letterJsonString)
    }
  }

  "senderFormat" should {
    import Letter.senderFormat

    "read the json correctly" in new Setup {
      Json.parse(senderJsonString).as[Sender] mustBe sender
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsResultException] {
        Json.parse(senderJsonStringInvalid).as[Sender]
      }
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(sender) mustBe Json.parse(senderJsonString)
    }
  }

  "dateTimeFormat" should {
    import Letter.dateTimeFormat

    "read the json correctly" in new Setup {
      Json.parse(timeInstantJsonString).as[Instant] mustBe timeInstant
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsonParseException] {
        Json.parse(timeInstantJsonStringInvalid).as[Instant]
      }
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(defaultTimeInstant) mustBe JsString(defaultTimeInstantJsonString)
    }
  }

  "dateFormat" should {
    import Letter.dateFormat

    "read the json correctly" in new Setup {
      Json.parse("20250101").as[LocalDate] mustBe date1
    }

    "throw exception for invalid json" in new Setup {
      intercept[JsonParseException] {
        Json.parse("2025:10:30").as[LocalDate]
      }
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(date) mustBe JsString(dateJsonString)
    }
  }

  trait Setup {
    val timeInstantJsonString: String = "20251012233000000"
    val dateJsonString = "2025-10-30"

    val timeInstantJsonStringInvalid: String = "2025-10-12T23:30:00Z"
    val defaultTimeInstantJsonString: String = "2025-10-12T23:30:00Z"

    val defaultTimeInstant: Instant = Instant.parse("2025-10-12T23:30:00Z")
    val timeInstant: Instant = Instant.parse("+643699-01-11T15:50:00Z")

    val year1970 = 1970

    val date: LocalDate = LocalDate.of(YEAR_2025, MONTH_10, DAY_30)
    val date1: LocalDate = LocalDate.of(year1970, 1, 1)

    val senderJsonString: String =
      """{
        |"name":"test_name",
        |"sent":"2025-10-30"
        |}""".stripMargin

    val senderJsonStringInvalid: String = """{
                                            |"name":"test_name"
                                            |}""".stripMargin

    val sender: Sender = Sender(name = TEST_NAME, sent = date)

    val firstReaderInformationJson: String =
      """{
        |"name":"test_name",
        |"read":20251012233000000
        |}""".stripMargin

    val firstReaderInformation: FirstReaderInformation =
      FirstReaderInformation(name = Some(TEST_NAME), read = timeInstant)

    val letterJsonString: String =
      s"""{
         |"subject":"test_subject",
         |"content":"test_content",
         |"senderInformation":$senderJsonString,
         |"readTime":"2025-10-12T23:30:00Z"
         |}""".stripMargin

    val letterJsonStringWithCustomReads: String =
      s"""{
         |"subject":"test_subject",
         |"content":"test_content",
         |"firstReaderInformation":$firstReaderInformationJson,
         |"senderInformation":$senderJsonString,
         |"readTime":20251012233000000
         |}""".stripMargin

    val letterJsonStringInvalid: String =
      s"""{
         |"subject":"test_subject",
         |"content":"test_content",
         |"readTime":"2025-10-12T23:30:00Z"
         |}""".stripMargin

    val letter: Letter = Letter(
      subject = TEST_SUBJECT,
      content = TEST_CONTENT,
      firstReaderInformation = None,
      senderInformation = sender,
      readTime = Some(defaultTimeInstant)
    )

    val letterObjectForCustomJsonReads: Letter = Letter(
      subject = TEST_SUBJECT,
      content = TEST_CONTENT,
      firstReaderInformation = Some(firstReaderInformation),
      senderInformation = sender,
      readTime = Some(timeInstant)
    )
  }
}
