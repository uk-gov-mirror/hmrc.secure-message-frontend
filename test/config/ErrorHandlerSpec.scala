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

import helpers.TestData.{ TEST_HEADING, TEST_MESSAGE, TEST_TITLE }
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.Results.{ NotFound, Unauthorized }
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{ BearerTokenExpired, IncorrectCredentialStrength, InsufficientConfidenceLevel, InvalidBearerToken, MissingBearerToken, SessionRecordNotFound }
import play.api.test.Helpers.await
import play.twirl.api.Html
import views.html.ErrorTemplate
import play.api.mvc.{ AnyContentAsEmpty, RequestHeader, Result }
import play.api.test.Helpers.*
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.http.NotFoundException

class ErrorHandlerSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite {

  trait Setup {
    val errorHandler: ErrorHandler = app.injector.instanceOf[ErrorHandler]
  }

  "The Error Handler" should {

    "return unauthorised in the case of an invalid bearer token" in new Setup {
      val exception = InvalidBearerToken()
      val result = errorHandler.resolveError(FakeRequest(), exception).futureValue

      result.header.status must be(Status.UNAUTHORIZED)
    }

    "return unauthorised in the case of a bearer token expiring" in new Setup {
      val exception = BearerTokenExpired()
      val result = errorHandler.resolveError(FakeRequest(), exception).futureValue

      result.header.status must be(Status.UNAUTHORIZED)
    }

    "return unauthorised in the case of an inactive session" in new Setup {
      val exception = SessionRecordNotFound()
      val result = errorHandler.resolveError(FakeRequest(), exception).futureValue

      result.header.status must be(Status.UNAUTHORIZED)
    }

    "return unauthorised in the case of an unauthorised request" in new Setup {
      val exception = InsufficientConfidenceLevel()
      val result = errorHandler.resolveError(FakeRequest(), exception).futureValue

      result.header.status must be(Status.UNAUTHORIZED)
    }

    "return unauthorised in the case of MissingBearerToken" in new Setup {
      val exception: MissingBearerToken = MissingBearerToken()
      val result: Result = await(errorHandler.resolveError(FakeRequest(), exception))

      result must be(Unauthorized("Unauthorised request received - Missing Bearer Token"))
    }

    "return NotFound in the case of NotFoundException" in new Setup {
      val exception: NotFoundException = NotFoundException("error occurred")
      val result: Result = await(errorHandler.resolveError(FakeRequest(), exception))

      result.header.status must be(NOT_FOUND)
    }

    "return UNAUTHORIZED in the case of IncorrectCredentialStrength" in new Setup {
      val exception: IncorrectCredentialStrength = IncorrectCredentialStrength("error occurred")
      val result: Result = await(errorHandler.resolveError(FakeRequest(), exception))

      result.header.status must be(UNAUTHORIZED)
    }

    "return correct error template for provided pageTitle, heading and message" in new Setup {
      implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", "/test/path").withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

      implicit val messages: Messages = stubMessages()
      implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

      val errorTemplate: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]

      val result: Html = await(errorHandler.standardErrorTemplate(TEST_TITLE, TEST_HEADING, TEST_MESSAGE)(fakeRequest))

      val actualTemplateBody: String = result.body
      val expectedTemplateBody: String = errorTemplate(TEST_TITLE, TEST_HEADING, TEST_MESSAGE).body

      assert(actualTemplateBody.contains(TEST_MESSAGE))
    }
  }
}
