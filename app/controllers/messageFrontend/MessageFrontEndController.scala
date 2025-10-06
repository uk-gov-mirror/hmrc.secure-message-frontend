/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.messageFrontend

import config.AppConfig
import connectors.{ RendererConnector, SecureMessageConnector }
import controllers.{ Encrypted, ParameterisedUrl, PartialHtml }
import model.*
import model.RenderMessageMetadata.{ ReadMessageMetadata, UnreadMessageMetadata }
import org.jsoup.Jsoup
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.{ Configuration, Environment, Logger }
import play.utils.UriEncoding
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{ AuthConnector, AuthorisedFunctions, MissingBearerToken, SessionRecordNotFound }
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{ OnlyRelative, RedirectUrl }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.HtmlPartial
import views.helpers.PortalUrlBuilder

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class MessageFrontEndController @Inject() (
  val appConfig: AppConfig,
  val authConnector: AuthConnector,
  rendererConnector: RendererConnector,
  http: HttpClientV2,
  val messageConnector: SecureMessageConnector,
  val config: Configuration,
  val env: Environment,
  val portalUrlBuilder: PortalUrlBuilder,
  val cc: MessagesControllerComponents,
  servicesConfig: ServicesConfig,
  val encryptAndEncode: EncryptAndEncode
)(implicit ec: ExecutionContext)
    extends FrontendController(cc) with AuthorisedFunctions with I18nSupport with PartialHtml {

  import RendererHandler.*

  private val logger = Logger(getClass)

  private def getOrElseDefaultTaxIdentifiers(taxIdentifiers: List[String], regimes: List[String]): List[String] =
    (taxIdentifiers, regimes) match {
      case (List(), List()) =>
        List("nino", "sautr", "HMRC-OBTDS-ORG", "HMRC-MTD-VAT", "HMRC-MTD-IT", "HMRC-PPT-ORG", "IR-PAYE")
      case _ => taxIdentifiers
    }

  def list(taxIdentifiers: List[String], regimes: List[String] = List()): Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(Retrievals.allEnrolments) { case enrolments =>
        asListHtml(enrolments, getOrElseDefaultTaxIdentifiers(taxIdentifiers, regimes), regimes).map(Ok(_))
      }
  }

  def btaList(taxIdentifiers: List[String], regimes: List[String] = List()): Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(Retrievals.allEnrolments) { case enrolments =>
        btaListHtml(enrolments, getOrElseDefaultTaxIdentifiers(taxIdentifiers, regimes), regimes).map(Ok(_))
      }
  }

  def read(encryptedUrl: Encrypted[ParameterisedUrl]): Action[AnyContent] = Action.async {
    implicit request: MessagesRequest[AnyContent] =>
      authorised() {
        if (encryptedUrl.decryptedValue.url.isEmpty) {
          logger.warn(s"Valid encryptedUrl needs to be present in the path ${request.uri}")
          Future.successful(Forbidden("Valid encryptedUrl needs to be present in the path"))
        } else {
          val parameterisedUrl = encryptedUrl.decryptedValue
          messageConnector.getMessageMetadata(parameterisedUrl.url).flatMap {
            case ReadMessageMetadata(rendererUrl) =>
              rendererConnector
                .getRenderedMessage(rendererUrl, parameterisedUrl.parameters)
                .map {

                  toResult(None)
                }
            case UnreadMessageMetadata(rendererUrl, setReadTimeUrl) =>
              rendererConnector
                .getRenderedMessage(rendererUrl, parameterisedUrl.parameters)
                .map { result =>
                  val markAsRead: MarkAsRead =
                    Some(() =>
                      http
                        .post(new URL(s"${servicesConfig.baseUrl(setReadTimeUrl.service)}${setReadTimeUrl.url}"))
                        .execute[HttpResponse]
                    )
                  toResult(markAsRead)(result)
                }
          }
        }
      }
  }

  def count(
    readPreference: Option[ReadPreference.Value],
    taxIdentifiers: List[String],
    regimes: List[String] = List()
  ): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      messageConnector
        .messageCount(
          MessagesCounts.transformReadPreference(readPreference),
          getOrElseDefaultTaxIdentifiers(taxIdentifiers, regimes),
          regimes
        )
        .map { messageCount =>
          Ok(Json.toJson(messageCount))
        }
        .recover { case _ => Ok(Json.toJson(MessageCount(0))) }
    }.recover {
      recoverWithNoRedirect()
    }
  }

  def inboxLink(
    messagesInboxUrl: RedirectUrl,
    taxIdentifiers: List[String],
    regimes: List[String] = List()
  ): Action[AnyContent] = Action.async { implicit request =>
    hc.authorization
    authorised() {
      messagesInboxUrl
        .getEither(OnlyRelative)
        .fold(
          errorMessage => Future.successful(BadRequest(errorMessage)),
          safeUrl =>
            asLinkHtml(safeUrl.url, getOrElseDefaultTaxIdentifiers(taxIdentifiers, regimes), regimes).map(Ok(_))
        )
    }
  }

  private def recoverWithNoRedirect()(implicit
    request: MessagesRequest[AnyContent]
  ): PartialFunction[Throwable, Result] = { case SessionRecordNotFound(_) | MissingBearerToken(_) =>
    logger.debug(s"Unauthorised request received ${request.uri}")
    Unauthorized("Unauthorised request received")
  }
}

private[controllers] object RendererHandler extends Results {
  type MarkAsRead = Option[() => Unit]

  def toResult(markAsRead: MarkAsRead): HtmlPartial => Result = {
    case HtmlPartial.Success(_, content) =>
      markAsRead.foreach { fireAndForget =>
        fireAndForget()
      }
      Ok(content).withHeaders("X-Title" -> UriEncoding.encodePathSegment(findTitle(content.body), "UTF-8"))

    case HtmlPartial.Failure(status, body) =>
      status.fold(InternalServerError(body)) {
        Results.Status(_)(body)
      }
  }

  private def findTitle(content: String): String = {
    def getTitle(content: String): Try[String] =
      Try(Jsoup.parse(content).getElementsByTag("h1").first().text())

    getTitle(content) match {
      case Failure(_)     => ""
      case Success(value) => value
    }
  }

}
