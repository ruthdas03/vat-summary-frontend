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

package controllers

import common.SessionKeys
import common.TestModels._
import models.User
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.Html
import services.ServiceInfoService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.certificate.VatCertificate

import scala.concurrent.{ExecutionContext, Future}

class VatCertificateControllerSpec extends ControllerBaseSpec {

  private trait Test {
    val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
    val vatCertificate: VatCertificate = injector.instanceOf[VatCertificate]

    val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = successfulAuthResult
    val serviceInfoServiceResult: Future[Html] = Future.successful(Html(""))

    val customerInfoCallExpected: Boolean = true

    def setup(): Any = {
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[~[Enrolments, Option[AffinityGroup]]])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(authResult)

      if (customerInfoCallExpected) {
        (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Right(customerInformationMax)))
      }

      (mockServiceInfoService.getPartial(_: Request[_], _: User, _: ExecutionContext))
        .stubs(*, *, *)
        .returns(serviceInfoServiceResult)

    }

    def target: VatCertificateController = {
      setup()
      new VatCertificateController(
        mockServiceInfoService,
        authorisedController,
        mockAccountDetailsService,
        mcc,
        vatCertificate,
        mockServiceErrorHandler
      )
    }
  }

  "The show() action" when {

    "the user is non-agent" when {

      "authorised" should {

        "return OK (200)" in new Test {
          private val result = target.show()(fakeRequest)
          status(result) shouldBe Status.OK
        }

        "return HTML" in new Test {
          private val result = target.show()(fakeRequest)
          contentType(result) shouldBe Some("text/html")
        }
      }
    }

    "the user is an agent" when {

      "user is authorised" should {

        "return OK (200)" in new Test {
          override def setup(): Unit = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[~[Enrolments, Option[AffinityGroup]]])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *, *)
              .returns(agentAuthResult)
              .noMoreThanOnce()

            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *, *)
              .returns(Future.successful(agentEnrolments))
              .noMoreThanOnce()

            (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(Right(customerInformationMax)))
            (mockServiceInfoService.getPartial(_: Request[_], _: User, _: ExecutionContext))
              .stubs(*, *, *)
              .returns(serviceInfoServiceResult)
          }

          private val result = target.show()(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

          status(result) shouldBe Status.OK
          contentType(result) shouldBe Some("text/html")
        }
      }

      "user is unauthorised" should {

        "return FORBIDDEN and agent unauthorised page" in new Test {

          override def setup(): Unit = {
            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[~[Enrolments, Option[AffinityGroup]]])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *, *)
              .returns(Future.successful(new ~(otherEnrolment, Some(Agent))))
              .noMoreThanOnce()

            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *, *)
              .returns(Future.successful(otherEnrolment))
              .noMoreThanOnce()
          }

          private val result = target.show()(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

          status(result) shouldBe Status.FORBIDDEN
          Jsoup.parse(contentAsString(result)).title() shouldBe "You can’t use this service yet - VAT - GOV.UK"
        }
      }
    }

    "the user is logged in with invalid credentials" should {

      "return Forbidden (403)" in new Test {
        override def setup(): Unit = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[~[Enrolments, Option[AffinityGroup]]])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.successful(new ~(otherEnrolment, Some(Agent))))
            .noMoreThanOnce()

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.successful(otherEnrolment))
            .noMoreThanOnce()
        }

        override val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.failed(InsufficientEnrolments())
        private val result = target.show()(fakeRequest.withSession(SessionKeys.agentSessionVrn -> "1112223331"))
        status(result) shouldBe Status.FORBIDDEN
      }

      "return HTML" in new Test {
        override def setup(): Unit = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[~[Enrolments, Option[AffinityGroup]]])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.successful(new ~(otherEnrolment, Some(Agent))))
            .noMoreThanOnce()

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.successful(otherEnrolment))
            .noMoreThanOnce()
        }

        override val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.failed(InsufficientEnrolments())
        private val result = target.show()(fakeRequest.withSession(SessionKeys.agentSessionVrn -> "1112223331"))
        contentType(result) shouldBe Some("text/html")
      }
    }

    "the user is not logged in" should {

      "return SEE_OTHER" in new Test {
        override def setup(): Unit = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[~[Enrolments, Option[AffinityGroup]]])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.failed(MissingBearerToken()))
            .noMoreThanOnce()
        }

        override val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.failed(MissingBearerToken())
        private val result = target.show()(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to sign in" in new Test {
        override def setup(): Unit = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[~[Enrolments, Option[AffinityGroup]]])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.failed(MissingBearerToken()))
            .noMoreThanOnce()
        }

        private val result = target.show()(fakeRequest)
        redirectLocation(result) shouldBe Some(mockAppConfig.signInUrl)
      }
    }
  }
}
