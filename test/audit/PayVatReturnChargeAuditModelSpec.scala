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

package audit

import _root_.audit.models.PayVatReturnChargeAuditModel
import _root_.models.User
import _root_.models.payments.{PaymentDetailsModel, ReturnDebitCharge}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

class PayVatReturnChargeAuditModelSpec extends AnyWordSpecLike with Matchers {

  val paymentDetails = PaymentDetailsModel(
    "vat",
    "taxRef",
    77777,
    10,
    2018,
    "2018-01-01",
    "/homepage",
    "/vat-overview",
    ReturnDebitCharge,
    "2018-01-01",
    chargeReference = None
  )

  val user = User("999999999")

  val paymentRedirectUrl = "/payment/pay-my-vat"

  "PayVatReturnChargeAuditModel" should {

    "be constructed correctly" in {
      val auditModel = PayVatReturnChargeAuditModel(user, paymentDetails, paymentRedirectUrl)

      val expected: Map[String, String] = Map(
        "vrn" -> "999999999",
        "taxType" -> "vat",
        "taxReference" -> "taxRef",
        "amountInPence" -> "77777",
        "taxPeriodMonth" -> "10",
        "taxPeriodYear" -> "2018",
        "returnUrl" -> "/homepage",
        "backUrl" -> "/vat-overview",
        "paymentRedirectUrl" -> "/payment/pay-my-vat",
        "chargeType" -> ReturnDebitCharge.value,
        "dueDate" -> "2018-01-01"
      )

      auditModel.detail shouldBe expected
    }
  }
}
