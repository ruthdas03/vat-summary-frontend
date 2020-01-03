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

package models.viewModels

import java.time.LocalDate

import models.{Address, CustomerInformation, TaxPeriod}

case class VatCertificateViewModel(
                                    vrn: String,
                                    registrationDate: Option[LocalDate],
                                    certificateDate: LocalDate,
                                    businessName: Option[String],
                                    tradingName: Option[String],
                                    businessTypeMsgKey: String,
                                    tradeClassification: String,
                                    ppob: Address,
                                    returnPeriodMsgKey: String,
                                    nonStdTaxPeriods: Option[Seq[TaxPeriod]],
                                    firstNonNSTPPeriod: Option[TaxPeriod]
                                  )

object VatCertificateViewModel {
  def fromCustomerInformation(vrn: String, customerInformation: CustomerInformation): VatCertificateViewModel = {
    VatCertificateViewModel(
      vrn, customerInformation.registrationDate.map(LocalDate.parse(_)), LocalDate.now(),
      customerInformation.organisationName, customerInformation.tradingName,
      customerInformation.partyTypeMessageKey, customerInformation.sicCode,
      customerInformation.businessAddress, customerInformation.returnPeriodMessageKey,
      customerInformation.nonStdTaxPeriods, customerInformation.firstNonNSTPPeriod
    )
  }
}
