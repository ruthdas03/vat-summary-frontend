/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import audit.AuditingService
import audit.models.ExtendedAuditModel
import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import controllers.predicates.{AgentPredicate, HybridUserPredicate}
import models.{CustomerInformation, User}
import models.errors.{DirectDebitStatusError, PaymentsError, UnknownError}
import models.payments._
import models.viewModels.OpenPaymentsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import services._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import common.TestModels._
import play.api.test.Helpers.redirectLocation
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}

class OpenPaymentsControllerSpec extends ControllerBaseSpec {

  private trait Test {
    val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = successfulAuthResult
    val serviceInfoServiceResult: Future[Html] = Future.successful(Html(""))

    val accountDetailsResponse: HttpGetResult[CustomerInformation] = Right(customerInformationMax)
    val mockAccountDetailsService: AccountDetailsService = mock[AccountDetailsService]
    val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]

    def setupMocks(): Unit = {
      (mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01"))

      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(authResult)

      (mockAuditService.extendedAudit(_: ExtendedAuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .stubs(*, *, *, *)
        .returns({})

      (mockServiceInfoService.getPartial(_: Request[_], _: ExecutionContext))
        .stubs(*,*)
        .returns(serviceInfoServiceResult)
    }

    val payment: PaymentWithPeriod = Payment(
      ReturnDebitCharge,
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-01-01"),
      BigDecimal("10000"),
      Some("ABCD")
    )

    val paymentOnAccount = Payment(
      PaymentOnAccount,
      LocalDate.parse("2017-01-01"),
      BigDecimal("0"),
      None
    )

    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockDateService: DateService = mock[DateService]
    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)
    val mockPaymentsService: PaymentsService = mock[PaymentsService]
    val mockAuditService: AuditingService = mock[AuditingService]
    val mockHybridUserPredicate: HybridUserPredicate = new HybridUserPredicate(mockAccountDetailsService)
    val mockAgentPredicate: AgentPredicate = new AgentPredicate(mockEnrolmentsAuthService, messages, mockAppConfig)
    val mockAuthorisedController: AuthorisedController = new AuthorisedController(
      messages,
      mockEnrolmentsAuthService,
      mockHybridUserPredicate,
      mockAgentPredicate,
      mockAppConfig
    )

    val testUser: User = User("999999999")
    implicit val hc: HeaderCarrier = HeaderCarrier()

    def target: OpenPaymentsController = {
      setupMocks()
      new OpenPaymentsController(
        messages,
        mockEnrolmentsAuthService,
        mockAuthorisedController,
        mockServiceInfoService,
        mockPaymentsService,
        mockDateService,
        mockAppConfig,
        mockAuditService)
    }
  }

  "Calling the openPayments action" when {

    "user is hybrid" should {

      "redirect to VAT overview page" in new Test {

        override val accountDetailsResponse: Right[Nothing, CustomerInformation] = Right(customerInformationHybrid)

        override def setupMocks(): Unit = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(authResult)

          (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(accountDetailsResponse)
        }

        private val result = target.openPayments()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.VatDetailsController.details().url)
      }
    }

    "user is not hybrid" when {

      "the user has open payments" should {

        "return 200 (OK)" in new Test {
          override def setupMocks(): Unit = {
            super.setupMocks()
            (mockPaymentsService.getDirectDebitStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .stubs(*, *, *)
              .returns(Right(true))

            (mockPaymentsService.getOpenPayments(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(Right(Some(Payments(Seq(payment, payment))))))

            (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(accountDetailsResponse)
          }

          private val result = target.openPayments()(fakeRequest)

          status(result) shouldBe Status.OK
        }

        "return the payments view" in new Test {
          override def setupMocks(): Unit = {
            super.setupMocks()
            (mockPaymentsService.getDirectDebitStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .stubs(*, *, *)
              .returns(Right(true))

            (mockPaymentsService.getOpenPayments(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(Right(Some(Payments(Seq(payment, payment))))))

            (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(accountDetailsResponse)
          }

          val result: Result = await(target.openPayments()(fakeRequest))
          val document: Document = Jsoup.parse(bodyOf(result))

          document.select("h1").first().text() shouldBe "What you owe"
        }
      }

      "the user has a Payment On Account charge returned" should {

        "return the payments view with only one payment listed" in new Test {

          override def setupMocks(): Unit = {
            super.setupMocks()
            (mockPaymentsService.getDirectDebitStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .stubs(*, *, *)
              .returns(Right(true))

            (mockPaymentsService.getOpenPayments(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(Right(Some(Payments(Seq(payment, paymentOnAccount))))))

            (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(accountDetailsResponse)
          }

          val result: Result = await(target.openPayments()(fakeRequest))
          val document: Document = Jsoup.parse(bodyOf(result))

          document.select("payment-2") shouldBe empty

        }
      }

      "the user has no open payments" should {

        "return 200 (OK)" in new Test {
          override def setupMocks(): Unit = {
            super.setupMocks()
            (mockPaymentsService.getDirectDebitStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .stubs(*, *, *)
              .returns(Right(true))

            (mockPaymentsService.getOpenPayments(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(Right(None)))

            (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(accountDetailsResponse)
          }

          private val result = target.openPayments()(fakeRequest)

          status(result) shouldBe Status.OK
        }

        "return the payments view" in new Test {
          override def setupMocks(): Unit = {
            super.setupMocks()
            (mockPaymentsService.getDirectDebitStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .stubs(*, *, *)
              .returns(Right(true))

            (mockPaymentsService.getOpenPayments(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(Right(None)))

            (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(accountDetailsResponse)
          }

          val result: Result = await(target.openPayments()(fakeRequest))
          val document: Document = Jsoup.parse(bodyOf(result))

          document.select("h1").first().text() shouldBe "What you owe"
        }
      }

      "the user is not authorised" should {

        "return 403 (Forbidden)" in new Test {
          override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
          private val result = target.openPayments()(fakeRequest)
          status(result) shouldBe Status.FORBIDDEN
        }
      }

      "user is an Agent" should {

        "return 403 (Forbidden)" in new Test {
          override val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = agentAuthResult
          val result: Future[Result] = target.openPayments()(fakeRequest)
          status(result) shouldBe Status.FORBIDDEN
        }
      }

      "the user is not authenticated" should {

        "return 401 (Unauthorised)" in new Test {
          override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
          private val result = target.openPayments()(fakeRequest)
          status(result) shouldBe Status.UNAUTHORIZED
        }
      }

      "the paymentsService returns an error" should {

        "return the payments view" in new Test {
          override def setupMocks(): Unit = {
            super.setupMocks()
            (mockPaymentsService.getDirectDebitStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Left(DirectDebitStatusError))

            (mockPaymentsService.getOpenPayments(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(Right(Some(Payments(Seq(payment, payment))))))

          (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(accountDetailsResponse)
         }
 
         val result: Result = await(target.openPayments()(fakeRequest))
         val document: Document = Jsoup.parse(bodyOf(result))
 
         document.select("h1").first().text() shouldBe "What you owe"
      }
    }

      "an error occurs upstream" should {

        "return 500 (Internal server error)" in new Test {
          override def setupMocks(): Unit = {
            super.setupMocks()
            (mockPaymentsService.getDirectDebitStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .stubs(*, *, *)
              .returns(Right(true))

            (mockPaymentsService.getOpenPayments(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(Left(PaymentsError)))

            (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(accountDetailsResponse)
          }

          private val result = target.openPayments()(fakeRequest)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "return the payments error view" in new Test {

          override def setupMocks(): Unit = {
            super.setupMocks()
            (mockPaymentsService.getDirectDebitStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .stubs(*, *, *)
              .returns(Right(true))

            (mockPaymentsService.getOpenPayments(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(Left(PaymentsError)))

            (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(accountDetailsResponse)
          }

          val result: Result = await(target.openPayments()(fakeRequest))
          val document: Document = Jsoup.parse(bodyOf(result))


          document.select("h1").first().text() shouldBe "Sorry, there is a problem with the service"
        }
      }
    }

    "the call to retrieve hybrid status fails" should {

      "return Internal Server Error" in new Test {

        override def setupMocks(): Unit = {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(authResult)

          (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(Left(UnknownError))
        }

        private val result = target.openPayments()(fakeRequest)

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "Calling the .getModel function" should {

      "return a sequence of OpenPaymentsModel" in new Test {
        override def setupMocks(): Unit = (
          mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01")
        )

        val expected = OpenPaymentsViewModel(
          Seq(OpenPaymentsModel(
            ReturnDebitCharge,
            payment.outstandingAmount,
            payment.due,
            payment.start,
            payment.end,
            payment.periodKey,
            overdue = true
          )),
          Some(true)
        )
        val result: OpenPaymentsViewModel = target.getModel(Seq(payment), Some(true))

        result shouldBe expected
      }
    }
  }
}
