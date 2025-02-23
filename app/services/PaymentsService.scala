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

package services

import java.time.LocalDate
import connectors.{FinancialDataConnector, PaymentsConnector}

import javax.inject.{Inject, Singleton}
import models.errors._
import models.payments.{PaymentDetailsModel, Payments}
import models.viewModels.PaymentsHistoryModel
import models.{DirectDebitStatus, ServiceResponse}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsService @Inject()(financialDataConnector: FinancialDataConnector,
                                paymentsConnector: PaymentsConnector) {

  def getOpenPayments(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[Option[Payments]]] =
    financialDataConnector.getOpenPayments(vrn).map {
      case Right(payments) =>
        val outstandingPayments = payments.financialTransactions.filter(_.outstandingAmount > 0)
        if(outstandingPayments.nonEmpty) {
          Right(Some(Payments(outstandingPayments.sortBy(_.due.toString).reverse)))
        } else {
          Right(None)
        }
      case Left(_) => Left(PaymentsError)
    }

  def getPaymentsHistory(vrn: String, searchYear: Int)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[Seq[PaymentsHistoryModel]]] = {
    val from: LocalDate = LocalDate.parse(s"$searchYear-01-01")
    val to: LocalDate = LocalDate.parse(s"$searchYear-12-31")

    financialDataConnector.getVatLiabilities(vrn, from, to).map {
      case Right(liabilities) => Right(liabilities.sortBy(_.clearedDate.toString).reverse)
      case Left(_) => Left(VatLiabilitiesError)
    }
  }

  def setupPaymentsJourney(journeyDetails: PaymentDetailsModel)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[String]] = {
    paymentsConnector.setupJourney(journeyDetails).map {
      case Right(url) => Right(url)
      case Left(_) => Left(PaymentSetupError)
    }
  }

  def getDirectDebitStatus(vrn: String)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[DirectDebitStatus]] =
    financialDataConnector.getDirectDebitStatus(vrn) map {
      case Right(directDebitStatus) => Right(directDebitStatus)
      case Left(_) => Left(DirectDebitStatusError)
    }
}
