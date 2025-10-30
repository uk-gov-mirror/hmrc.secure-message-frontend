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
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ JsString, Json }
import views.helpers.DateFormat

import java.time.format.DateTimeFormatter
import java.time.{ Instant, LocalDate, LocalDateTime }

class LetterSpec extends PlaySpec {

  "messageFormat" should {
    import Letter.messageFormat

    "read the json correctly" in new Setup {}

    "throw exception for incorrect json" in new Setup {}

    "generate correct output for Json Writes" in new Setup {}
  }

  "senderFormat" should {
    import Letter.senderFormat

    "read the json correctly" in new Setup {}

    "throw exception for incorrect json" in new Setup {}

    "generate correct output for Json Writes" in new Setup {}
  }

  "dateTimeFormat" should {
    import Letter.dateTimeFormat

    "read the json correctly" in new Setup {
      Json.parse(timeInstantString).as[Instant] mustBe timeInstant
    }

    "throw exception for incorrect json" in new Setup {
      intercept[JsonParseException] {
        Json.parse(timeInstantStringIncorrect).as[Instant]
      }
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(defaultTimeInstant) mustBe JsString(defaultTimeInstantString)
    }
  }

  "dateFormat" should {
    import Letter.dateFormat

    "read the json correctly" in new Setup {
      Json.parse("20250101").as[LocalDate] mustBe date1
    }

    "throw exception for incorrect json" in new Setup {}

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(date) mustBe JsString(dateString)
    }
  }

  trait Setup {
    val timeInstantString: String = "20251012233000000"
    val dateString = "2025-10-30"

    val timeInstantStringIncorrect: String = "2025-10-12T23:30:00Z"
    val defaultTimeInstantString: String = "2025-10-12T23:30:00Z"

    val defaultTimeInstant: Instant = Instant.parse("2025-10-12T23:30:00Z")
    val timeInstant: Instant = Instant.parse("+643699-01-11T15:50:00Z")

    val year = 2025
    val year1970 = 1970
    val month = 10
    val day = 30

    val date: LocalDate = LocalDate.of(year, month, day)
    val date1: LocalDate = LocalDate.of(year1970, 1, 1)
  }
}
