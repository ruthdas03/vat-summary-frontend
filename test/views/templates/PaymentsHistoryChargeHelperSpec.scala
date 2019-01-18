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

import uk.gov.hmrc.play.test.UnitSpec
import views.templates.PaymentsHistoryChargeHelper

class PaymentsHistoryChargeHelperSpec extends UnitSpec {

  "Calling getChargeType" when {

    "the lookup string is a valid charge type" should {

      "return the charge type associated with the lookup string" in {

        val result = PaymentsHistoryChargeHelper.getChargeType("VAT Return Debit Charge")

        result shouldBe Some(PaymentsHistoryChargeHelper.VatReturnDebitCharge)

      }

      "return a central assessment charge type" in {

        val result = PaymentsHistoryChargeHelper.getChargeType("VAT Central Assessment")

        result shouldBe Some(PaymentsHistoryChargeHelper.VatCentralAssessment)

      }

      "return a default surcharge charge type" in {

        val result = PaymentsHistoryChargeHelper.getChargeType("VAT Default Surcharge")

        result shouldBe Some(PaymentsHistoryChargeHelper.VatDefaultSurcharge)

      }

      "return a VAT OA default interest charge type" in {

        val result = PaymentsHistoryChargeHelper.getChargeType("VAT OA Default Interest")

        result shouldBe Some(PaymentsHistoryChargeHelper.OADefaultInterest)

      }

      "return a VAT Officers Assessment Further Interest charge type" in {

        val result = PaymentsHistoryChargeHelper.getChargeType("VAT OA Further Interest")

        result shouldBe Some(PaymentsHistoryChargeHelper.VatOfficersAssessmentFurtherInterest)
      }

      "return a VAT Additional Assessment charge type" in {

        val result = PaymentsHistoryChargeHelper.getChargeType("VAT Additional Assessment")

        result shouldBe Some(PaymentsHistoryChargeHelper.VatAdditionalAssessment)
      }

      "return a VAT AA Default Interest charge type" in {

        val result = PaymentsHistoryChargeHelper.getChargeType("VAT AA Default Interest")

        result shouldBe Some(PaymentsHistoryChargeHelper.VatAADefaultInterest)
      }

      "return a VAT AA Further Interest charge type" in {

        val result = PaymentsHistoryChargeHelper.getChargeType("VAT AA Further Interest")

        result shouldBe Some(PaymentsHistoryChargeHelper.VatAAFurtherInterest)
      }

      "return a VAT Statutory Interest charge type" in {

        val result = PaymentsHistoryChargeHelper.getChargeType("VAT Statutory Interest")

        result shouldBe Some(PaymentsHistoryChargeHelper.VatStatutoryInterestCharge)
      }
    }

    "the lookup String is an invalid charge type" should {

      "return a None" in {
        
        val result = PaymentsHistoryChargeHelper.getChargeType("invalid")

        result shouldBe None

      }
    }
  }
}
