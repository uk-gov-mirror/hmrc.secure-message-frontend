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

class LetterSpec extends PlaySpec {
  "messageFormat" should {
    import Letter.messageFormat
    "read the json correctly" in {}

    "throw exception for incorrect json" in new Setup {}

    "generate correct output for Json Writes" in {}
  }

  "senderFormat" should {
    import Letter.senderFormat
    "read the json correctly" in {}

    "throw exception for incorrect json" in new Setup {}

    "generate correct output for Json Writes" in {}
  }

  "dateTimeFormat" should {
    import Letter.dateTimeFormat
    "read the json correctly" in {}

    "throw exception for incorrect json" in new Setup {}

    "generate correct output for Json Writes" in {}
  }

  "dateFormat" should {
    import Letter.dateFormat
    "read the json correctly" in {}

    "throw exception for incorrect json" in new Setup {}

    "generate correct output for Json Writes" in {}
  }

  trait Setup {}
}
