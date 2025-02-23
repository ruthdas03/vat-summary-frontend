/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.predicates

import common.{EnrolmentKeys, SessionKeys}
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.User
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import services.EnrolmentsAuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.LoggerUtil
import views.html.errors.AgentUnauthorised

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentPredicate @Inject()(authService: EnrolmentsAuthService,
                               mcc: MessagesControllerComponents,
                               agentUnauthorised: AgentUnauthorised,
                               financialPredicate: FinancialPredicate)
                              (implicit appConfig: AppConfig,
                               ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with LoggerUtil {

  def authoriseAsAgent(block: Request[AnyContent] => User => Future[Result], financialRequest: Boolean = false)
                      (implicit request: Request[AnyContent]): Future[Result] = {

    val agentDelegatedAuthorityRule: String => Enrolment = vrn =>
      Enrolment(EnrolmentKeys.mtdVatEnrolmentKey)
        .withIdentifier(EnrolmentKeys.mtdVatIdentifierKey, vrn)
        .withDelegatedAuthRule(EnrolmentKeys.agentDelegatedAuthRuleKey)

    request.session.get(SessionKeys.agentSessionVrn) match {
      case Some(vrn) =>
        authService
          .authorised(agentDelegatedAuthorityRule(vrn))
          .retrieve(allEnrolments) {
            enrolments =>
              enrolments.enrolments.collectFirst {
                case Enrolment(EnrolmentKeys.agentEnrolmentKey, Seq(EnrolmentIdentifier(_, arn)), EnrolmentKeys.activated, _) => arn
              } match {
                case Some(arn) =>
                  val user = User(vrn, arn = Some(arn))
                  if(financialRequest) {
                    financialPredicate.authoriseFinancialAction(block)(request, user)
                  } else {
                    block(request)(user)
                  }
                case None =>
                  logger.debug("[AgentPredicate][authoriseAsAgent] - " +
                    "Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
                  Future.successful(Forbidden(agentUnauthorised()))
              }
          } recover {
          case _: NoActiveSession =>
            logger.debug(s"AgentPredicate][authoriseAsAgent] - No active session. Redirecting to ${appConfig.signInUrl}")
            Redirect(appConfig.signInUrl)
          case _: AuthorisationException =>
            logger.debug(s"[AgentPredicate][authoriseAsAgent] - Agent does not have delegated authority for Client. " +
              s"Redirecting to ${appConfig.agentClientUnauthorisedUrl(request.uri)}")
            Redirect(appConfig.agentClientUnauthorisedUrl(request.uri))
          }
      case None =>
        logger.debug("[AuthPredicate][authoriseAsAgent] - No Client VRN in session. " +
          s"Redirecting to ${appConfig.agentClientLookupStartUrl(request.uri)}")
        Future.successful(Redirect(appConfig.agentClientLookupStartUrl(request.uri)))
    }
  }
}
