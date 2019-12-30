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
import common.TestModels.{agentAuthResult, customerInformationHybrid, customerInformationMax}
import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import controllers.predicates.{AgentPredicate, HybridUserPredicate}
import models.errors.{UnknownError, VatLiabilitiesError}
import models.payments.ReturnDebitCharge
import models.viewModels.{PaymentsHistoryModel, PaymentsHistoryViewModel}
import models.{CustomerInformation, ServiceResponse, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class PaymentHistoryControllerSpec extends ControllerBaseSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockAccountDetailsService: AccountDetailsService = mock[AccountDetailsService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockPaymentsService: PaymentsService = mock[PaymentsService]
  val mockDateService: DateService = mock[DateService]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  implicit val mockAuditService: AuditingService = mock[AuditingService]
  val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)
  val mockHybridUserPredicate: HybridUserPredicate = new HybridUserPredicate(mockAccountDetailsService, mockServiceErrorHandler)
  val mockAgentPredicate: AgentPredicate = new AgentPredicate(mockEnrolmentsAuthService, messages, mockAppConfig)
  val mockAuthorisedController: AuthorisedController = new AuthorisedController(
    messages,
    mockEnrolmentsAuthService,
    mockHybridUserPredicate,
    mockAgentPredicate,
    mockAppConfig
  )

  lazy val fakeRequestWithEmptyDate: FakeRequest[AnyContentAsEmpty.type] =
    fakeRequest.withSession("customerMigratedToETMPDate" -> "")
  val exampleAmount = 100
  implicit val user: User = User("123456789")

  private trait Test {
    val serviceResultYearOne: ServiceResponse[Seq[PaymentsHistoryModel]] =
      Right(Seq(
        PaymentsHistoryModel(
          chargeType    = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2018-01-01")),
          taxPeriodTo   = Some(LocalDate.parse("2018-02-01")),
          amount        = exampleAmount,
          clearedDate   = Some(LocalDate.parse("2018-03-01"))
        ),
        PaymentsHistoryModel(
          chargeType    = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2018-03-01")),
          taxPeriodTo   = Some(LocalDate.parse("2018-04-01")),
          amount        = exampleAmount,
          clearedDate   = Some(LocalDate.parse("2018-05-01"))
        )
      ))
    val serviceResultYearTwo: ServiceResponse[Seq[PaymentsHistoryModel]] =
      Right(Seq(
        PaymentsHistoryModel(
          chargeType    = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2017-01-01")),
          taxPeriodTo   = Some(LocalDate.parse("2017-02-01")),
          amount        = exampleAmount,
          clearedDate   = Some(LocalDate.parse("2017-03-01"))
        ),
        PaymentsHistoryModel(
          chargeType    = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2017-03-01")),
          taxPeriodTo   = Some(LocalDate.parse("2017-04-01")),
          amount        = exampleAmount,
          clearedDate   = Some(LocalDate.parse("2017-05-01"))
        )
      ))

    val currentYear: Int = 2018
    val paymentsServiceCall: Boolean = false
    val authCall: Boolean = false
    val accountDetailsCall: Boolean = false
    val enrolments: Set[Enrolment] = Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), ""))
    lazy val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
      Enrolments(enrolments),
      Some(Individual)
    ))
    val accountDetailsResponse: HttpGetResult[CustomerInformation] = Right(customerInformationMax)
    val serviceInfoServiceResult: Future[Html] = Future.successful(Html(""))

    def setup(): Any = {
      (mockDateService.now: () => LocalDate)
        .stubs()
        .returns(LocalDate.parse("2018-05-01"))

      (mockServiceInfoService.getPartial(_: Request[_], _: User, _: ExecutionContext))
        .stubs(*,*,*)
        .returns(serviceInfoServiceResult)

      if (authCall) {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(authResult)
      }

      if (accountDetailsCall) {
        (mockAccountDetailsService.getAccountDetails(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(accountDetailsResponse)
      }

      if (paymentsServiceCall) {
        (mockAuditService.extendedAudit(_: ExtendedAuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .stubs(*, *, *, *)
          .returns({})

        (mockPaymentsService.getPaymentsHistory(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *).noMoreThanOnce()
          .returns(serviceResultYearOne)

        (mockPaymentsService.getPaymentsHistory(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *).noMoreThanOnce()
          .returns(serviceResultYearTwo)
      }
    }

    def target: PaymentHistoryController = {
      setup()
      new PaymentHistoryController(
        messages,
        mockPaymentsService,
        mockAuthorisedController,
        mockDateService,
        mockServiceInfoService,
        mockEnrolmentsAuthService,
        mockAccountDetailsService
      )
    }
  }

  private trait AllCallsTest extends Test {
    override val authCall: Boolean = true
    override val accountDetailsCall: Boolean = true
    override val paymentsServiceCall: Boolean = true
  }

  private trait NoPaymentsCallsTest extends Test {
    override val authCall: Boolean = true
    override val accountDetailsCall: Boolean = true
  }

  "Calling the paymentHistory action" when {

    "the user is logged in" when {

      "return 200" in new AllCallsTest {
        private val result = target.paymentHistory()(fakeRequestWithSession)
        status(result) shouldBe Status.OK
      }

      "return HTML" in new AllCallsTest {
        private val result = target.paymentHistory()(fakeRequestWithSession)
        contentType(result) shouldBe Some("text/html")
      }

      "return charset utf-8" in new AllCallsTest {
        private val result = target.paymentHistory()(fakeRequestWithSession)
        charset(result) shouldBe Some("utf-8")
      }
    }

    "the user is not logged in" should {

      "return SEE_OTHER" in new Test {
        override val authCall = true
        override lazy val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.failed(MissingBearerToken())
        val result: Future[Result] = target.paymentHistory()(fakeRequestWithSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(mockAppConfig.signInUrl)
      }
    }

    "the user is not authenticated" should {

      "return 403 (Forbidden)" in new Test {
        override val authCall = true
        override lazy val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.failed(InsufficientEnrolments())
        val result: Future[Result] = target.paymentHistory()(fakeRequestWithSession)
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "user is an Agent" should {

      "redirect to Agent Action page" in new Test {
        override val authCall = true
        override lazy val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = agentAuthResult
        val result: Future[Result] = target.paymentHistory()(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(mockAppConfig.agentClientLookupActionUrl)
      }
    }

    "the user is hybrid" should {

      "redirect to VAT overview page" in new NoPaymentsCallsTest {
        override val accountDetailsResponse: Right[Nothing, CustomerInformation] = Right(customerInformationHybrid)
        private val result = target.paymentHistory()(fakeRequestWithSession)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.VatDetailsController.details().url)
      }
    }

    "the call to retrieve hybrid status fails" should {

      "return Internal Server Error" in new NoPaymentsCallsTest {
        override val accountDetailsResponse: HttpGetResult[CustomerInformation] = Left(UnknownError)
        private val result = target.paymentHistory()(fakeRequestWithSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "an error occurs upstream" should {

      "return a 500" in new AllCallsTest {
        override val serviceResultYearOne = Left(VatLiabilitiesError)
        private val result: Result = await(target.paymentHistory()(fakeRequestWithSession))
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return the standard error view" in new AllCallsTest {
        override val serviceResultYearOne = Left(VatLiabilitiesError)
        private val result: Result = await(target.paymentHistory()(fakeRequestWithSession))
        val document: Document = Jsoup.parse(bodyOf(result))
        document.select("h1").first().text() shouldBe "Sorry, there is a problem with the service"
      }
    }
  }

  "Calling .customerMigratedWithin15M" when {

    "the interval between dates is less than 15 months" should {

      "return true" in new Test {
        target.customerMigratedWithin15M(Some(LocalDate.parse("2017-02-02"))) shouldBe true
      }
    }

    "the interval between dates is 15 months or greater" should {

      "return false" in new Test {
        target.customerMigratedWithin15M(Some(LocalDate.parse("2017-02-01"))) shouldBe false
      }
    }

    "the interval is 0 days" should {

      "return true" in new Test {
        target.customerMigratedWithin15M(Some(LocalDate.parse("2018-05-01"))) shouldBe true
      }
    }

    "the date provided is None" should {

      "return false as a default" in new Test {
        target.customerMigratedWithin15M(None) shouldBe false
      }
    }
  }

  "Calling .getMigratedToETMPDate" when {

    "the ETMP migration date is already in session" should {

      "return the date" in new Test {
        await(target.getMigratedToETMPDate(fakeRequestWithSession, user)) shouldBe Some(LocalDate.parse("2018-01-01"))
      }
    }

    "an empty value is in session" should {

      "return None" in new Test {
        await(target.getMigratedToETMPDate(fakeRequestWithEmptyDate, user)) shouldBe None
      }
    }

    "no value is in session" when {

      "the account details service returns a successful result" should {

        "return the date" in new Test {
          override val accountDetailsCall: Boolean = true
          override val accountDetailsResponse: HttpGetResult[CustomerInformation] =
            Right(customerInformationMax.copy(customerMigratedToETMPDate = Some("2015-05-05")))
          await(target.getMigratedToETMPDate(fakeRequest, user)) shouldBe Some(LocalDate.parse("2015-05-05"))
        }
      }

      "the account details service returns a failure" should {

        "return None" in new Test {
          override val accountDetailsCall: Boolean = true
          override val accountDetailsResponse: HttpGetResult[CustomerInformation] = Left(UnknownError)
          await(target.getMigratedToETMPDate(fakeRequest, user)) shouldBe None
        }
      }
    }
  }

  "Calling .getValidYears" when {

    "the migration year is equal to the current year" should {

      "return a sequence of just the current year" in new Test {
        target.getValidYears(user.vrn, Some(LocalDate.parse("2018-01-01"))) shouldBe Seq(currentYear)
      }
    }

    "the migration year is not equal to the current year" should {

      "return a sequence of the current year and previous year" in new Test {
        target.getValidYears(user.vrn, Some(LocalDate.parse("2017-12-12"))) shouldBe Seq(currentYear, currentYear - 1)
      }
    }

    "the migration year could not be retrieved" should {

      "return a sequence of the current year and previous year" in new Test {
        target.getValidYears(user.vrn, None) shouldBe Seq(currentYear, currentYear - 1)
      }
    }
  }

  "Calling .generateViewModel" when {

    "both service call parameters are successful" when {

      "the customer was migrated in the current year" should {

        "return a PaymentsHistoryViewModel with the correct information" in new Test {
          target.generateViewModel(
            serviceResultYearOne, serviceResultYearTwo, showPreviousPaymentsTab = false, Some(LocalDate.parse("2018-01-01"))
          ) shouldBe Some(PaymentsHistoryViewModel(
            currentYear,
            None,
            previousPaymentsTab = false,
            serviceResultYearOne.right.get ++ serviceResultYearTwo.right.get
          ))
        }
      }

      "the customer was not migrated in the current year" should {

        "return a PaymentsHistoryViewModel with the correct information" in new Test {
          target.generateViewModel(
            serviceResultYearOne, serviceResultYearTwo, showPreviousPaymentsTab = false, Some(LocalDate.parse("2017-12-12"))
          ) shouldBe Some(PaymentsHistoryViewModel(
            currentYear,
            Some(currentYear - 1),
            previousPaymentsTab = false,
            serviceResultYearOne.right.get ++ serviceResultYearTwo.right.get
          ))
        }
      }
    }

    "either of the service call parameters fail" should {

      "return None" in new Test {
        override val serviceResultYearOne = Left(VatLiabilitiesError)
        target.generateViewModel(
          serviceResultYearOne, serviceResultYearTwo, showPreviousPaymentsTab = false, None
        ) shouldBe None
      }
    }
  }
}
