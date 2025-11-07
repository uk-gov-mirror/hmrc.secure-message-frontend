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
import ReadPreference.{ Both, No, Yes }

class ReadPreferenceSpec extends PlaySpec {

  "validate" should {
    "return the correct Enum value for the valid input" in {
      val result: Either[String, ReadPreference.Value] = ReadPreference.validate("Yes")
      result mustBe Right(Yes)

      val result1: Either[String, ReadPreference.Value] = ReadPreference.validate("No")
      result1 mustBe Right(No)

      val result2: Either[String, ReadPreference.Value] = ReadPreference.validate("Both")
      result2 mustBe Right(Both)
    }

    "return the error text for invalid values" in {
      val result: Either[String, ReadPreference.Value] = ReadPreference.validate("Unknown")
      result.swap mustBe Right("unknown read preference: Unknown")
    }
  }
}
