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

package connectors

import java.time.LocalDate
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationBaseSpec
import models.{DDIDetails, DirectDebitStatus}
import models.errors.BadRequestError
import models.payments.{PaymentWithPeriod, Payments, ReturnCreditCharge, ReturnDebitCharge}
import models.viewModels.PaymentsHistoryModel
import stubs.FinancialDataStub
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers._


class FinancialDataConnectorISpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    val connector: FinancialDataConnector = app.injector.instanceOf[FinancialDataConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "calling getOpenPayments with a status of 'O'" should {

    "return all outstanding payments for a given period" in new Test {
      override def setupStubs(): StubMapping = FinancialDataStub.stubOutstandingTransactions

      val expected = Right(Payments(Seq(
        PaymentWithPeriod(
          chargeType = ReturnDebitCharge,
          periodFrom = LocalDate.parse("2015-03-01"),
          periodTo = LocalDate.parse("2015-03-31"),
          due = LocalDate.parse("2019-01-15"),
          outstandingAmount = 10000,
          periodKey = "15AC",
          ddCollectionInProgress = false,
          chargeReference = Some("XD002750002155")
        ),
        PaymentWithPeriod(
          chargeType = ReturnDebitCharge,
          periodFrom = LocalDate.parse("2015-03-01"),
          periodTo = LocalDate.parse("2015-03-31"),
          due = LocalDate.parse("2019-01-16"),
          outstandingAmount = 10000,
          periodKey = "15AC",
          ddCollectionInProgress = false,
          chargeReference = Some("XD002750002155")
        )
      )))

      setupStubs()
      private val result = await(connector.getOpenPayments("123456789"))

      result shouldEqual expected
    }

    "return an empty list of payments" in new Test {
      override def setupStubs(): StubMapping = FinancialDataStub.stubNoPayments

      val expected = Right(Payments(Seq.empty))

      setupStubs()
      private val result = await(connector.getOpenPayments("123456789"))

      result shouldEqual expected
    }
  }

  "calling getOpenPayments with an invalid VRN" should {

    "return an BadRequestError" in new Test {
      override def setupStubs(): StubMapping = FinancialDataStub.stubInvalidVrn

      val expected = Left(BadRequestError(
        code = "INVALID_VRN",
        errorResponse = "VRN was invalid!"
      ))

      setupStubs()
      private val result = await(connector.getOpenPayments("111"))

      result shouldEqual expected
    }
  }

  "calling getVatLiabilities" should {

    "return a PaymentsHistoryModel" in new Test {
      override def setupStubs(): StubMapping = FinancialDataStub.stubPaidTransactions

      val expected = Right(Seq(
        PaymentsHistoryModel(
          chargeType    =  ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.parse("2018-08-01")),
          taxPeriodTo   = Some(LocalDate.parse("2018-10-31")),
          amount        = 150,
          clearedDate   = Some(LocalDate.parse("2018-01-10"))
        ),
        PaymentsHistoryModel(
          chargeType    =  ReturnCreditCharge,
          taxPeriodFrom = Some(LocalDate.parse("2018-05-01")),
          taxPeriodTo   = Some(LocalDate.parse("2018-07-31")),
          amount        = -600,
          clearedDate   = Some(LocalDate.parse("2018-03-10"))
        )
      ))

      setupStubs()
      private val result = await(connector.getVatLiabilities(
        "5555555555",
        LocalDate.parse("2018-01-01"),
        LocalDate.parse("2018-12-31")
      ))

      result shouldEqual expected
    }
  }

  "calling getDirectDebitStatus with a valid VRN" should {
    "return a DirectDebitStatus" in new Test {
      override def setupStubs(): StubMapping = FinancialDataStub.stubSuccessfulDirectDebit

      val expected = Right(DirectDebitStatus(directDebitMandateFound = true, Some(Seq(DDIDetails("2018-01-01")))))

      setupStubs()
      private val result = await(connector.getDirectDebitStatus("111111111"))

      result shouldEqual expected
    }
  }

  "calling getDirectDebitStatus with an invalid VRN" should {

    "return an BadRequestError" in new Test {
      override def setupStubs(): StubMapping = FinancialDataStub.stubInvalidVrnDirectDebit

      val expected = Left(BadRequestError(
        code = "INVALID_VRN",
        errorResponse = "VRN was invalid!"
      ))

      setupStubs()
      private val result = await(connector.getDirectDebitStatus("111"))

      result shouldEqual expected
    }
  }
}
