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

import java.time.LocalDate
import audit.AuditingService
import audit.models.ExtendedAuditModel
import common.TestModels._
import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
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
import views.html.errors.StandardError
import views.html.payments.PaymentHistory
import play.api.test.Helpers.defaultAwaitTimeout

import scala.concurrent.{ExecutionContext, Future}

class PaymentHistoryControllerSpec extends ControllerBaseSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val standardError: StandardError = injector.instanceOf[StandardError]
  val paymentHistory: PaymentHistory = injector.instanceOf[PaymentHistory]

  val mockPaymentsService: PaymentsService = mock[PaymentsService]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  implicit val mockAuditService: AuditingService = mock[AuditingService]

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
        ),
        PaymentsHistoryModel(
          chargeType    = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2017-03-01")),
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
        ),
        PaymentsHistoryModel(
          chargeType    = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2017-03-01")),
          taxPeriodTo   = Some(LocalDate.parse("2018-04-01")),
          amount        = exampleAmount,
          clearedDate   = Some(LocalDate.parse("2018-05-01"))
        )
      ))
    val serviceResultYearThree: ServiceResponse[Seq[PaymentsHistoryModel]] =
      Right(Seq(
        PaymentsHistoryModel(
          chargeType    = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2016-01-01")),
          taxPeriodTo   = Some(LocalDate.parse("2016-02-01")),
          amount        = exampleAmount,
          clearedDate   = Some(LocalDate.parse("2016-03-01"))
        ),
        PaymentsHistoryModel(
          chargeType    = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2016-07-01")),
          taxPeriodTo   = Some(LocalDate.parse("2016-08-01")),
          amount        = exampleAmount,
          clearedDate   = Some(LocalDate.parse("2016-09-01"))
        ),
        PaymentsHistoryModel(
          chargeType    = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2016-09-01")),
          taxPeriodTo   = Some(LocalDate.parse("2016-10-01")),
          amount        = exampleAmount,
          clearedDate   = Some(LocalDate.parse("2016-11-01"))
        )
      ))
    val emptyResult: ServiceResponse[Seq[PaymentsHistoryModel]] = Right(Seq())

    val currentYear: Int = 2018
    val paymentsServiceCall: Boolean = false
    val authCall: Boolean = false
    val accountDetailsCall: Boolean = false
    val enrolments: Set[Enrolment] = Set(
      Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), ""),
      Enrolment("HMCE-VATDEC-ORG", Seq(EnrolmentIdentifier("VATRegNo", "123456789")), "")
    )

    lazy val authResult: Future[Enrolments ~ Option[AffinityGroup]] = Future.successful(new ~(
      Enrolments(enrolments),
      Some(Individual)
    ))
    val accountDetailsResponse: HttpGetResult[CustomerInformation] = Right(customerInformationMax)
    val accountDetailsResponseNoMigratedDate: HttpGetResult[CustomerInformation] = Right(customerInformationMin)
    val serviceInfoServiceResult: Future[Html] = Future.successful(Html(""))

    def setup(): Any = {
      mockDateServiceCall()

      (mockServiceInfoService.getPartial(_: Request[_], _: User, _: ExecutionContext))
        .stubs(*, *, *)
        .returns(serviceInfoServiceResult)

      if (authCall) {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(authResult)
      }

      if (accountDetailsCall) {
        mockCustomerInfo(Future.successful(accountDetailsResponse))
      }

      if (paymentsServiceCall) {
        (mockAuditService.extendedAudit(_: ExtendedAuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .stubs(*, *, *, *)
          .returns({})

        (mockPaymentsService.getPaymentsHistory(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *).noMoreThanOnce()
          .returns(Future.successful(serviceResultYearOne))

        (mockPaymentsService.getPaymentsHistory(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *).noMoreThanOnce()
          .returns(Future.successful(serviceResultYearTwo))
      }
    }

    def target: PaymentHistoryController = {
      setup()
      new PaymentHistoryController(
        mockPaymentsService,
        authorisedController,
        mockDateService,
        mockServiceInfoService,
        mockAccountDetailsService,
        mockServiceErrorHandler,
        mcc,
        paymentHistory,
        ddInterruptPredicate
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
        override lazy val authResult: Future[Enrolments ~ Option[AffinityGroup]] = Future.failed(MissingBearerToken())
        val result: Future[Result] = target.paymentHistory()(fakeRequestWithSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(mockAppConfig.signInUrl)
      }
    }

    "the user is not authenticated" should {

      "return 403 (Forbidden)" in new Test {
        override val authCall = true
        override lazy val authResult: Future[Enrolments ~ Option[AffinityGroup]] = Future.failed(InsufficientEnrolments())
        val result: Future[Result] = target.paymentHistory()(fakeRequestWithSession)
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "user is an Agent" should {

      "return 200" in new AllCallsTest {
        override lazy val authResult: Future[Enrolments ~ Option[AffinityGroup]] = agentAuthResult
        override def setup(): Any = {
          super.setup()
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.successful(agentEnrolments))
            .noMoreThanOnce()
        }
        private val result = target.paymentHistory()(agentFinancialRequest)
        status(result) shouldBe Status.OK
      }
    }

    "the user is hybrid" should {

      "redirect to VAT overview page" in new NoPaymentsCallsTest {
        override val accountDetailsResponse: Right[Nothing, CustomerInformation] = Right(customerInformationHybrid)
        private val result = target.paymentHistory()(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.VatDetailsController.details().url)
      }
    }

    "the call to retrieve hybrid status fails" should {

      "return Internal Server Error" in new NoPaymentsCallsTest {
        override val accountDetailsResponse: HttpGetResult[CustomerInformation] = Left(UnknownError)
        private val result = target.paymentHistory()(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "an error occurs upstream" should {

      "return a 500" in new AllCallsTest {
        override val serviceResultYearOne = Left(VatLiabilitiesError)
        private val result: Future[Result] = target.paymentHistory()(fakeRequestWithSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return the standard error view" in new AllCallsTest {
        override val serviceResultYearOne = Left(VatLiabilitiesError)
        private val result: Future[Result] = target.paymentHistory()(fakeRequestWithSession)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("h1").first().text() shouldBe "Sorry, there is a problem with the service"
      }
    }

    "the user is insolvent and not continuing to trade" should {

      "return 403 (Forbidden)" in new Test {
        override val authCall = true
        val result: Future[Result] = target.paymentHistory()(insolventRequest)
        status(result) shouldBe Status.FORBIDDEN
      }
    }
    "the user has no viewDirectDebitInterrupt in session" should {

      "return 303(SEE OTHER)" in new Test  {
        override def setup(): Any = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(authResult)
        }
        val result: Future[Result] = target.paymentHistory()(DDInterruptRequest)
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.DDInterruptController.directDebitInterruptCall("/homepage").url)
      }
    }

    "the user has a VATDEC enrolment and no customerMigratedToETMPDate" should {

      "return 200" in new AllCallsTest {
        override val accountDetailsCall: Boolean = false
        (mockPaymentsService.getPaymentsHistory(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *).noMoreThanOnce()
          .returns(Future.successful(emptyResult))
        mockCustomerInfo(Future.successful(accountDetailsResponseNoMigratedDate))
        mockCustomerInfo(Future.successful(accountDetailsResponseNoMigratedDate))
        val result: Future[Result] = target.paymentHistory()(fakeRequest)
        status(result) shouldBe OK
      }

      "render the Previous Payments tab" in new AllCallsTest {
        override val accountDetailsCall: Boolean = false
        (mockPaymentsService.getPaymentsHistory(_: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *).noMoreThanOnce()
          .returns(Future.successful(emptyResult))
        mockCustomerInfo(Future.successful(accountDetailsResponseNoMigratedDate))
        mockCustomerInfo(Future.successful(accountDetailsResponseNoMigratedDate))
        val result: Future[Result] = target.paymentHistory()(fakeRequest)
        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("li.govuk-tabs__list-item:nth-child(4)").text() shouldBe "Previous payments"
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

    "the account details service response contains customer info" should {

      "return the date" in new Test {
        target.getMigratedToETMPDate(Right(customerInformationMax)) shouldBe Some(LocalDate.parse("2017-05-06"))
      }
    }

    "the account details service response contains an error" should {

      "return None" in new Test {
        target.getMigratedToETMPDate(Left(UnknownError)) shouldBe None
      }
    }
  }

  "Calling .showInsolventContent" when {

    "the account details service response contains customer info" when {

      "the user is insolvent and not exempt from restrictions" should {

        "return true" in new Test {
          target.showInsolventContent(Right(customerInformationInsolventTrading)) shouldBe true
        }
      }

      "the user is insolvent and exempt from restrictions" should {

        "return false" in new Test {
          target.showInsolventContent(Right(customerInformationInsolventTradingExempt)) shouldBe false
        }
      }

      "the user is not insolvent" should {

        "return false" in new Test {
          target.showInsolventContent(Right(customerInformationMax)) shouldBe false
        }
      }
    }

    "the account details service response contains an error" should {

      "return false" in new Test {
        target.showInsolventContent(Left(UnknownError)) shouldBe false
      }
    }
  }

  "Calling .getValidYears" when {

    "the migration year is equal to the current year" should {

      "return a sequence of just the current year" in new Test {
        target.getValidYears(Some(LocalDate.parse("2018-01-01"))) shouldBe Seq(currentYear)
      }
    }

    "the migration year is the previous year" should {

      "return a sequence of the current year and previous year" in new Test {
        target.getValidYears(Some(LocalDate.parse("2017-12-12"))) shouldBe Seq(currentYear, currentYear - 1)
      }
    }

    "the migration year is two years ago" should {

      "return a sequence of the current year and the two years prior" in new Test {
        target.getValidYears(Some(LocalDate.parse("2016-12-12"))) shouldBe Seq(currentYear, currentYear - 1, currentYear - 2)
      }
    }

    "the migration year could not be retrieved" should {

      "return a sequence of the current year and previous year" in new Test {
        target.getValidYears(None) shouldBe Seq(currentYear, currentYear - 1, currentYear - 2)
      }
    }
  }

  "Calling .isLast24Months" should {
    "return true" when {
      "the provided date is younger than 24 months" in new Test {
        val (year, month, day): (Int, Int, Int) = (2016, 6, 1)
        target.isLast24Months(Some(LocalDate.of(year, month, day))) shouldBe true
      }
      "the provided date is exactly 24 months ago" in new Test {
        val (year, month, day): (Int, Int, Int) = (2016, 5, 1)
        target.isLast24Months(Some(LocalDate.of(year, month, day))) shouldBe true
      }
      "no date is provided" in new Test {
        target.isLast24Months(None) shouldBe true
      }
    }
    "return false" when {
      "the provided date is older than 24 months" in new Test {
        val (year, month, day): (Int, Int, Int) = (2016, 4, 1)
        target.isLast24Months(Some(LocalDate.of(year, month, day))) shouldBe false
      }
    }
  }

  "Calling .generateViewModel" when {

    "both service call parameters are successful" when {

      "the customer was migrated in the current year" should {

        "return a PaymentsHistoryViewModel with the correct information" in new Test {
          target.generateViewModel(
            serviceResultYearOne,
            serviceResultYearTwo,
            emptyResult,
            showPreviousPaymentsTab = false,
            Some(LocalDate.parse("2018-01-01")),
            showInsolvencyContent = false,
            None
          ) shouldBe Some(PaymentsHistoryViewModel(
            currentYear,
            None,
            None,
            previousPaymentsTab = false,
            (serviceResultYearOne.right.get ++ serviceResultYearTwo.right.get).distinct,
            showInsolvencyContent = false,
            None
          ))
        }
      }

      "the customer was migrated in the previous year" should {

        "return a PaymentsHistoryViewModel with the correct information" in new Test {
          target.generateViewModel(
            serviceResultYearOne,
            serviceResultYearTwo,
            emptyResult,
            showPreviousPaymentsTab = false,
            Some(LocalDate.parse("2017-01-01")),
            showInsolvencyContent = false,
            None
          ) shouldBe Some(PaymentsHistoryViewModel(
            currentYear,
            Some(currentYear - 1),
            None,
            previousPaymentsTab = false,
            (serviceResultYearOne.right.get ++ serviceResultYearTwo.right.get).distinct,
            showInsolvencyContent = false,
            None
          ))
        }
      }

      "the customer was migrated two years ago" should {

        "return a PaymentsHistoryViewModel with the correct information" in new Test {
          target.generateViewModel(
            serviceResultYearOne,
            serviceResultYearTwo,
            serviceResultYearThree,
            showPreviousPaymentsTab = false,
            Some(LocalDate.parse("2016-12-12")),
            showInsolvencyContent = false,
            None
          ) shouldBe Some(PaymentsHistoryViewModel(
            currentYear,
            Some(currentYear - 1),
            Some(currentYear - 2),
            previousPaymentsTab = false,
            (serviceResultYearOne.right.get ++ serviceResultYearTwo.right.get ++ serviceResultYearThree.right.get.drop(1)).distinct,
            showInsolvencyContent = false,
            None
          ))
        }
      }
    }

    "either of the service call parameters fail" should {

      "return None" in new Test {
        override val serviceResultYearOne = Left(VatLiabilitiesError)
        target.generateViewModel(
          serviceResultYearOne,
          serviceResultYearTwo,
          serviceResultYearThree,
          showPreviousPaymentsTab = false,
          None,
          showInsolvencyContent = false,
          None
        ) shouldBe None
      }
    }
  }
}
