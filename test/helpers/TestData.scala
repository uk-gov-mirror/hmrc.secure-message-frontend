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

package helpers

import java.time.LocalDate

object TestData {
  val FIVE = 5
  val TWO = 2

  val YEAR_2025 = 2025

  val MONTH_11 = 11
  val MONTH_10 = 10

  val DAY_1 = 1
  val DAY_30 = 30

  val TEST_LOCAL_DATE: LocalDate = LocalDate.of(YEAR_2025, MONTH_11, DAY_1)

  val TEST_SUBJECT = "test_subject"
  val TEST_CONTENT = "test_content"
  val TEST_NAME = "test_name"
  val TEST_ID = "test_id"
  val TEST_FORENAME = "test_forename"
  val TEST_SECOND_FORENAME = "test_second_fore_name"
  val TEST_SURNAME = "test_surname"
  val TEST_LINE1 = "test_line1"
  val TEST_LINE2 = "test_line2"
  val TEST_ADDRESS_STRING = "test_address"

  val TEST_TITLE = "test_title"
  val TEST_HEADING = "test_heading"
  val TEST_MESSAGE = "test_message"
  val TEST_CLIENT = "test_client"
  val TEST_SENDER_NAME = "test_sender"
  val TEST_STATUS = "test_status"
  val TEST_HONOURS = "test_honours"

  val TEST_SERVICE_NAME = "test_service"

  val TEST_LANGUAGE_ENGLISH = "English"
}
