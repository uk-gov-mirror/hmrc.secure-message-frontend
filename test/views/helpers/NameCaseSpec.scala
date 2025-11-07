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

package views.helpers

import org.scalatestplus.play.PlaySpec

class NameCaseSpec extends PlaySpec {

  "nc" should {

    "return the correct name with correct case" in {
      NameCase.nc("Mack") mustBe "Mack"
      NameCase.nc("Macky") mustBe "Macky"
      NameCase.nc("Mace") mustBe "Mace"
      NameCase.nc("\bMacEvicius") mustBe "\bMacevicius"
      NameCase.nc("\bMacHado") mustBe "\bMachado"
      NameCase.nc("\bMacHar") mustBe "\bMachar"
      NameCase.nc("\bMacHin") mustBe "\bMachin"
      NameCase.nc("\bMacHlin") mustBe "\bMachlin"
      NameCase.nc("\bMacIas") mustBe "\bMacias"
      NameCase.nc("\bMacIulis") mustBe "\bMaciulis"
      NameCase.nc("\bMacKie") mustBe "\bMackie"
      NameCase.nc("\bMacKle") mustBe "\bMackle"
      NameCase.nc("\bMacKlin") mustBe "\bMacklin"
      NameCase.nc("\bMacQuarie") mustBe "\bMacquarie"
      NameCase.nc("\bMacOmber") mustBe "\bMacomber"
      NameCase.nc("\bMacIn") mustBe "\bMacin"
      NameCase.nc("\bMacKintosh") mustBe "\bMackintosh"
      NameCase.nc("\bMacKen") mustBe "\bMacken"
      NameCase.nc("\bMacHen") mustBe "\bMachen"
      NameCase.nc("\bMacisaac") mustBe "\bMacisaac"
      NameCase.nc("\bMacHiel") mustBe "\bMachiel"
      NameCase.nc("\bMacIol") mustBe "\bMaciol"
      NameCase.nc("\bMacKell") mustBe "\bMackell"
      NameCase.nc("\bMacKlem") mustBe "\bMacklem"
      NameCase.nc("\bMacKrell") mustBe "\bMackrell"
      NameCase.nc("\bMacLin") mustBe "\bMaclin"
      NameCase.nc("\bMacKey") mustBe "\bMackey"
      NameCase.nc("\bMacKley") mustBe "\bMackley"
      NameCase.nc("\bMacHell") mustBe "\bMachell"
      NameCase.nc("\bMacHon") mustBe "\bMachon"
      NameCase.nc("\bMacmurdo") mustBe "\bMacMurdo"
    }
  }
}
