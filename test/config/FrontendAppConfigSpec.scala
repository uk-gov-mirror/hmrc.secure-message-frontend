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

package config

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{ Application, Configuration }
import com.typesafe.config.ConfigFactory
import play.api.inject.guice.GuiceApplicationBuilder

class FrontendAppConfigSpec extends PlaySpec with GuiceOneAppPerSuite {
  "btaHost" should {
    "return the correct value" in new Setup {
      appConfig.btaHost mustBe "http://localhost:9020"
    }
  }

  "btaBaseUrl" should {
    "return the correct value" in new Setup {
      appConfig.btaBaseUrl mustBe "http://localhost:9020/business-account"
    }
  }

  "ptaHost" should {
    "return the correct value" in new Setup {
      appConfig.ptaHost mustBe "http://localhost:9232"
    }
  }

  "ptaBaseUrl" should {
    "return the correct value" in new Setup {
      appConfig.ptaBaseUrl mustBe "http://localhost:9232/personal-account"
    }
  }

  "getPortalPath" should {
    "return the correct value" in new Setup {
      appConfig.getPortalPath("test_key") mustBe "test_key"
    }
  }

  trait Setup {
    val config: Configuration = Configuration(
      ConfigFactory.parseString(
        s"""
           |metrics.enabled=false,
           |metrics.enabled=false
           |business-account {
           |host ="http://localhost:9020"
           |}
           |personal-account {
           |host ="http://localhost:9232"
           |},
           |portal {
           |destinationPath {
           |test_key = "test_key"
           |}
           |}""".stripMargin
      )
    )

    val app: Application = new GuiceApplicationBuilder().configure(config).build()

    implicit val appConfig: AppConfig = app.injector.instanceOf[FrontendAppConfig]
  }
}
