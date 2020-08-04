/*
 * Copyright 2020 HM Revenue & Customs
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

package common

import views.templates.payments.PaymentMessageHelper._

object MessageLookup {

  object SessionTimeout {
    val title: String = "Your session has timed out"
    val instructions: String = "To view your VAT summary, you'll have to sign in using your Government Gateway ID."
  }

  object Unauthorised {
    val title: String = "Unauthorised access"
    val instructions: String = "Here are some instructions about what you should do next."
  }

  object PaymentMessages {

    private val datePeriodShort: String = "1 Jan to 1 Feb 2018"
    private val datePeriodLong: String = "1 January to 1 February 2018"

    //scalastyle:off
    def getMessagesForChargeType(chargeType: String, useLongDateFormat: Boolean = false): (String, String) = {

      val datePeriod = if(useLongDateFormat) datePeriodLong else datePeriodShort

      chargeType match {
        case DefaultInterest.name => ("Default interest", s"based on our assessment of your tax for the period $datePeriod")
        case FurtherInterest.name => ("Further interest", s"based on our assessment of your tax for the period $datePeriod")
        case VatReturnCreditCharge.name => ("Repayment from HMRC", s"for your $datePeriod return")
        case VatReturnDebitCharge.name => ("Return", s"for the period $datePeriod")
        case VatOfficerAssessmentCreditCharge.name => ("VAT officer’s assessment", "for overpaying by this amount")
        case VatOfficerAssessmentDebitCharge.name => ("VAT officer’s assessment", "for underpaying by this amount")
        case VatCentralAssessment.name => ("Estimate", s"for your $datePeriod return")
        case VatDefaultSurcharge.name => ("Surcharge", s"for late payment of your $datePeriod return")
        case VatErrorCorrectionDebitCharge.name => ("Error correction payment", s"for correcting your $datePeriod return")
        case VatErrorCorrectionCreditCharge.name => ("Error correction repayment from HMRC", s"for correcting your $datePeriod return")
        case VatRepaymentSupplement.name => ("Late repayment compensation from HMRC", s"we took too long to repay your $datePeriod return")
        case OADefaultInterest.name => ("VAT officer's assessment interest", s"interest charged on the officer's assessment")
        case VatBnpRegPre2010Charge.name => ("Penalty for late registration", "because you should have been registered for VAT earlier")
        case VatBnpRegPost2010Charge.name => ("Penalty for late registration", "because you should have been registered for VAT earlier")
        case VatFtnMatPre2010Charge.name => ("Failure to notify penalty", "you did not tell us you are no longer exempt from VAT registration")
        case VatFtnMatPost2010Charge.name => ("Failure to notify penalty", "you did not tell us you are no longer exempt from VAT registration")
        case VatMiscPenaltyCharge.name => ("VAT general penalty", "")
        case VatOfficersAssessmentFurtherInterest.name => ("VAT officer’s assessment further interest", "further interest charged on the officer’s assessment")
        case VatAdditionalAssessment.name => ("Additional assessment", s"additional assessment based on further information for the period $datePeriod")
        case VatAADefaultInterest.name => ("Additional assessment interest", s"interest charged on additional tax assessed for the period $datePeriod")
        case VatAAFurtherInterest.name => ("Additional assessment further interest", s"further interest charged on additional tax assessed for the period $datePeriod")
        case VatAAReturnDebitCharge.name => ("Annual accounting balance", s"for the period $datePeriod")
        case VatAAReturnCreditCharge.name => ("Annual accounting repayment", s"for the period $datePeriod")
        case VatAAMonthlyInstalment.name => ("Annual accounting monthly instalment", s"for the period $datePeriod")
        case VatAAQuarterlyInstalments.name => ("Annual accounting quarterly instalment", s"for the period $datePeriod")
        case VatStatutoryInterestCharge.name => ("Statutory interest", "interest paid because of an error by HMRC")
        case VatSecurityDepositRequest.name => ("Security deposit requirement", "because you have not paid VAT in your current or previous business(es)")
        case VatEcNoticeFurtherInterest.name => ("Error correction further interest", "further interest charged on assessed amount")
        case CivilEvasionPenalty.name => ("VAT civil evasion penalty", "because we have identified irregularities involving dishonesty")
        case VatInaccuraciesInECSales.name => ("Inaccuracies penalty", "because you have provided inaccurate information in your EC sales list")
        case VatFailureToSubmitECSales.name => ("EC sales list penalty", "because you have not submitted an EC sales list or you have submitted it late")
        case FtnEachPartner.name => ("Failure to notify penalty", "because you did not tell us about all the partners and changes in your partnership")
        case VatOAInaccuracies2009.name => ("Inaccuracies penalty", s"because you submitted an inaccurate document for the period $datePeriod")
        case VatInaccuracyAssessmentsPenCharge.name => ("Inaccuracies penalty", s"because you submitted an inaccurate document for the period $datePeriod")
        case VatMpPre2009Charge.name => ("Misdeclaration penalty", "because you have made an incorrect declaration")
        case VatMpRepeatedPre2009Charge.name => ("Misdeclaration repeat penalty", "because you have repeatedly made incorrect declarations")
        case VatInaccuraciesReturnReplacedCharge.name => ("Inaccuracies penalty", s"this is because you have submitted inaccurate information for the period $datePeriod")
        case VatWrongDoingPenaltyCharge.name => ("Wrongdoing penalty", "because you charged VAT when you should not have done")
        case VatPADefaultInterest.name => ("Protective assessment default interest", "interest charged on the protective assessment")
        case VatECDefaultInterest.name => ("Error correction default interest", "interest charged on assessed amount")
        case VatPaFurtherInterest.name => ("Protective assessment further interest", "further interest due on the protective assessment as this was not paid on time")
        case VatCarterPenaltyCharge.name => ("Penalty for not filing correctly", s"because you did not use the correct digital channel for the period $datePeriod")
        case VatFailureToNotifyRCSL.name => ("Failure to notify penalty", "because you failed to notify us of the date you made a reverse charge sale or stopped making supplies")
        case VatFailureToSubmitRCSL.name => ("Reverse Charge sales list penalty", "because you have failed to submit a Reverse Charge sales list")
        case VatCreditReturnOffsetCharge.name => ("Overpayment partial refund", s"partial repayment for period $datePeriod")
        case ProtectiveAssessmentCharge.name => ("Protective assessment", "assessment raised to protect HMRC’s position during an appeal")
        case UnallocatedPaymentCharge.name => ("Unallocated payment", "you made an overpayment, you can have this refunded or leave it on account")
        case RefundsCharge.name => ("Refund payment from HMRC", "as you requested a refund on an overpayment you made")
        case VATPOAInstalmentCharge.name => ("Payment on account instalment", s"for the period $datePeriod")
        case VATPOAReturnDebitCharge.name => ("Payment on account balance", s"for the period $datePeriod")
        case VATPOAReturnCreditCharge.name => ("Payment on account repayment", s"for the period $datePeriod")
        case _ => throw new IllegalArgumentException(s"[MessageLookup][PaymentMessages][getMessagesForChargeType] Charge type not found in message lookup: $chargeType")
      }
    }
    //scalastyle:on
  }

  object CovidMessages {

    val headingPreEnd: String = "We previously set out that you could delay (defer) paying VAT because of coronavirus " +
      "(COVID-19). The VAT deferral period ends on 30 June 2020."

    val headingPostEnd: String = "We previously set out that you could delay (defer) paying VAT because of coronavirus " +
      "(COVID-19). The VAT deferral period ended on 30 June 2020."

    val line1: String = "VAT bills with a payment due date on or after 1 July 2020 must be paid on time and in full."
    val line2link: String = "Payment Support Service"
    val line2: String = s"If you cancelled your Direct Debit, set it up again so you do not miss a payment. Contact our $line2link " +
      "as soon as possible if you cannot pay. You might be able to set up a Time to Pay agreement if you’re struggling to pay a tax bill."
    val line3: String = "You still need to submit VAT Returns, even if your business has temporarily closed."
    val line4: String = "You have until 31 March 2021 to pay VAT bills that were due between 20 March 2020 and 30 June 2020."

  }
}
