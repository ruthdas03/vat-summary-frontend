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

package models

import java.time.LocalDate

import common.FinancialTransactionsConstants
import models.payments._
import models.viewModels.PaymentsHistoryModel
import play.api.libs.json._
import uk.gov.hmrc.play.test.UnitSpec

class PaymentsHistoryModelSpec extends UnitSpec {

  val reads: Reads[Seq[PaymentsHistoryModel]] = PaymentsHistoryModel.reads

  implicit val writes: Writes[PaymentsHistoryModel] = Json.writes[PaymentsHistoryModel]

  "reads" when {

    "one item per period is returned" should {

      val testJson: JsValue = Json.parse(
        s"""{
           |    "idType" : "VRN",
           |    "idNumber" : "555555555",
           |    "regimeType" : "VATC",
           |    "processingDate" : "2018-03-07T09:30:00.000Z",
           |    "financialTransactions" : [
           |      {
           |        "chargeType" : "$ReturnDebitCharge",
           |        "mainType" : "VAT Return Charge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-08-01",
           |        "taxPeriodTo" : "2018-10-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-01-10",
           |            "dueDate" : "2018-12-07",
           |            "amount" : 150
           |          }
           |        ]
           |      },
           |      {
           |        "chargeType" : "$ReturnCreditCharge",
           |        "mainType" : "VAT Return Charge",
           |        "periodKey" : "17BB",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-05-01",
           |        "taxPeriodTo" : "2018-07-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 600,
           |        "outstandingAmount" : 600,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-03-10",
           |            "dueDate" : "2018-09-07",
           |            "amount" : 600
           |          }
           |        ]
           |      }
           |    ]
           |  }""".stripMargin
      )

      val expectedSeq: Seq[PaymentsHistoryModel] = Seq(
        PaymentsHistoryModel(
          chargeType = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 8, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 10, 31)),
          amount = 150,
          clearedDate = Some(LocalDate.of(2018, 1, 10))
        ),
        PaymentsHistoryModel(
          chargeType = ReturnCreditCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 5, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 7, 31)),
          amount = 600,
          clearedDate = Some(LocalDate.of(2018, 3, 10))
        )
      )

      "return one item per period" in {
        Json.fromJson(testJson)(reads) shouldBe JsSuccess(expectedSeq)
      }
    }

    "multiple items per period is returned" should {

      val testJson: JsValue = Json.parse(
        s"""{
           |    "idType" : "VRN",
           |    "idNumber" : "555555555",
           |    "regimeType" : "VATC",
           |    "processingDate" : "2018-03-07T09:30:00.000Z",
           |    "financialTransactions" : [
           |      {
           |        "chargeType" : "$ReturnDebitCharge",
           |       "mainType" : "VAT Return Charge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-08-01",
           |        "taxPeriodTo" : "2018-10-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-01-10",
           |            "dueDate" : "2018-12-07",
           |            "amount" : 150
           |          }
           |        ]
           |      },
           |      {
           |        "chargeType" : "$DefaultSurcharge",
           |       "mainType" : "$DefaultSurcharge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-08-01",
           |        "taxPeriodTo" : "2018-10-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-01-10",
           |            "dueDate" : "2018-12-07",
           |            "amount" : 150
           |          }
           |        ]
           |      },
           |      {
           |        "chargeType" : "$CentralAssessmentCharge",
           |       "mainType" : "$CentralAssessmentCharge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-08-01",
           |        "taxPeriodTo" : "2018-10-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-01-10",
           |            "dueDate" : "2018-12-07",
           |            "amount" : 150
           |          }
           |        ]
           |      },
           |      {
           |        "chargeType" : "${ReturnCreditCharge}",
           |        "mainType" : "VAT Return Charge",
           |        "periodKey" : "17BB",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-05-01",
           |        "taxPeriodTo" : "2018-07-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 600,
           |        "outstandingAmount" : 600,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-03-10",
           |            "dueDate" : "2018-09-07",
           |            "amount" : 600
           |          }
           |        ]
           |      },
           |      {
           |        "chargeType" : "$ErrorCorrectionCreditCharge",
           |        "mainType" : "VAT Error Correction",
           |        "periodKey" : "17BB",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-05-01",
           |        "taxPeriodTo" : "2018-07-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 600,
           |        "outstandingAmount" : 600,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-03-10",
           |            "dueDate" : "2018-09-07",
           |            "amount" : 600
           |          }
           |        ]
           |      },
           |      {
           |        "chargeType" : "$ErrorCorrectionDebitCharge",
           |        "mainType" : "VAT Error Correction",
           |        "periodKey" : "17BB",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-05-01",
           |        "taxPeriodTo" : "2018-07-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 600,
           |        "outstandingAmount" : 600,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-03-10",
           |            "dueDate" : "2018-09-07",
           |            "amount" : 600
           |          }
           |        ]
           |      }
           |    ]
           |  }""".stripMargin
      )

      val expectedSeq: Seq[PaymentsHistoryModel] = Seq(
        PaymentsHistoryModel(
          chargeType = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 8, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 10, 31)),
          amount = 150,
          clearedDate = Some(LocalDate.of(2018, 1, 10))
        ),
        PaymentsHistoryModel(
          chargeType = DefaultSurcharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 8, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 10, 31)),
          amount = 150,
          clearedDate = Some(LocalDate.of(2018, 1, 10))
        ),
        PaymentsHistoryModel(
          chargeType = CentralAssessmentCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 8, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 10, 31)),
          amount = 150,
          clearedDate = Some(LocalDate.of(2018, 1, 10))
        ),
        PaymentsHistoryModel(
          chargeType = ReturnCreditCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 5, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 7, 31)),
          amount = 600,
          clearedDate = Some(LocalDate.of(2018, 3, 10))
        ),
        PaymentsHistoryModel(
          chargeType = ErrorCorrectionCreditCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 5, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 7, 31)),
          amount = 600,
          clearedDate = Some(LocalDate.of(2018, 3, 10))
        ),
        PaymentsHistoryModel(
          chargeType = ErrorCorrectionDebitCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 5, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 7, 31)),
          amount = 600,
          clearedDate = Some(LocalDate.of(2018, 3, 10))
        )
      )
      "return multiple items per period" in {
        Json.fromJson(testJson)(reads) shouldBe JsSuccess(expectedSeq)
      }
    }

    "one item per period with different charge types is returned" should {

      val testJson: JsValue = Json.parse(
        s"""{
           |    "idType" : "VRN",
           |    "idNumber" : 555555555,
           |    "regimeType" : "VATC",
           |    "processingDate" : "2018-03-07T09:30:00.000Z",
           |    "financialTransactions" : [
           |      {
           |        "chargeType" : "$ReturnDebitCharge",
           |        "mainType" : "VAT Return Charge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-08-01",
           |        "taxPeriodTo" : "2018-10-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-01-10",
           |            "dueDate" : "2018-12-07",
           |            "amount" : 150
           |          }
           |        ]
           |      },
           |      {
           |        "chargeType" : "$ReturnCreditCharge",
           |        "mainType" : "VAT Return Charge",
           |        "periodKey" : "17BB",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-05-01",
           |        "taxPeriodTo" : "2018-07-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 600,
           |        "outstandingAmount" : 600,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-04-10",
           |            "dueDate" : "2018-09-07",
           |            "amount" : 600
           |          }
           |        ]
           |      }
           |    ]
           |  }""".stripMargin
      )

      val expectedSeq = Seq(
        PaymentsHistoryModel(
          chargeType = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 8, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 10, 31)),
          amount = 150,
          clearedDate = Some(LocalDate.of(2018, 1, 10))
        ),
        PaymentsHistoryModel(
          chargeType = ReturnCreditCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 5, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 7, 31)),
          amount = 600,
          clearedDate = Some(LocalDate.of(2018, 4, 10))
        )
      )

      "return the correct models" in {
        Json.fromJson(testJson)(reads) shouldBe JsSuccess(expectedSeq)
      }
    }

    "there is multiple items in one period" should {

      val testJson: JsValue = Json.parse(
        s"""{
           |    "idType" : "VRN",
           |    "idNumber" : 555555555,
           |    "regimeType" : "VATC",
           |    "processingDate" : "2018-03-07T09:30:00.000Z",
           |    "financialTransactions" : [
           |      {
           |        "chargeType" : "$ReturnDebitCharge",
           |        "mainType" : "VAT Return Charge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-08-01",
           |        "taxPeriodTo" : "2018-10-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-01-10",
           |            "dueDate" : "2018-12-07",
           |            "amount" : 150
           |          },
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-03-10",
           |            "dueDate" : "2018-12-07",
           |            "amount" : 100
           |          }
           |        ]
           |      }
           |    ]
           |  }""".stripMargin

      )

      val expectedSeq: Seq[PaymentsHistoryModel] = Seq(
        PaymentsHistoryModel(
          chargeType = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 8, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 10, 31)),
          amount = 150,
          clearedDate = Some(LocalDate.of(2018, 1, 10))
        ),
        PaymentsHistoryModel(
          chargeType = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 8, 1)),
          taxPeriodTo = Some(LocalDate.of(2018, 10, 31)),
          amount = 100,
          clearedDate = Some(LocalDate.of(2018, 3, 10))
        )
      )

      "return multiple charges" in {
        Json.fromJson(testJson)(reads) shouldBe JsSuccess(expectedSeq)
      }
    }

    "financial transactions is empty" should {

      val testJson: JsValue = Json.parse(
        """
          |{
          |   "financialTransactions" : [
          |
          |   ]
          |}
        """.stripMargin
      )

      val expectedSeq: Seq[PaymentsHistoryModel] = Seq.empty

      "return empty sequence" in {
        Json.fromJson(testJson)(reads) shouldBe JsSuccess(expectedSeq)
      }
    }

    "there is no financialTransactions block" should {
      val testJson: JsValue = Json.parse("""{ "abc" : "xyz" }""")

      "throw a JsResultException" in {
        intercept[JsResultException](Json.fromJson(testJson)(reads))
      }
    }

    "there is no items block" should {

      val testJson: JsValue = Json.parse(
        s"""{
           |    "idType" : "VRN",
           |    "idNumber" : 555555555,
           |    "regimeType" : "VATC",
           |    "processingDate" : "2018-03-07T09:30:00.000Z",
           |    "financialTransactions" : [
           |      {
           |        "chargeType" : "$ReturnDebitCharge",
           |        "mainType" : "VAT Return Charge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-08-01",
           |        "taxPeriodTo" : "2018-10-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150
           |      }
           |    ]
           |  }""".stripMargin
      )

      "throw a JsResultException" in {
        intercept[JsResultException](Json.fromJson(testJson)(reads))
      }
    }

    "there is no amount" should {
      val testJson = Json.parse(
        s"""{
           |    "idType" : "VRN",
           |    "idNumber" : 555555555,
           |    "regimeType" : "VATC",
           |    "processingDate" : "2018-03-07T09:30:00.000Z",
           |    "financialTransactions" : [
           |      {
           |        "chargeType" : "$ReturnDebitCharge",
           |       "mainType" : "VAT Return Charge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-08-01",
           |        "taxPeriodTo" : "2018-10-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-01-10",
           |            "dueDate" : "2018-12-07"
           |          }
           |        ]
           |      }
           |    ]
           |  }""".stripMargin
      )

      "throw a JsResultException" in {
        intercept[JsResultException](Json.fromJson(testJson)(reads))
      }
    }

    "there is no dateFrom" should {

      val testJson: JsValue = Json.parse(
        s"""{
           |    "idType" : "VRN",
           |    "idNumber" : 555555555,
           |    "regimeType" : "VATC",
           |    "processingDate" : "2018-03-07T09:30:00.000Z",
           |    "financialTransactions" : [
           |      {
           |        "chargeType" : "$ReturnDebitCharge",
           |        "mainType" : "VAT Return Charge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodTo" : "2018-10-31",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-01-10",
           |            "dueDate" : "2018-12-07",
           |            "amount" : 150
           |          }
           |        ]
           |      }
           |    ]
           |  }""".stripMargin
      )

      val expectedSeq: Seq[PaymentsHistoryModel] = Seq(
        PaymentsHistoryModel(
          chargeType = ReturnDebitCharge,
          taxPeriodFrom = None,
          taxPeriodTo = Some(LocalDate.of(2018, 10, 31)),
          amount = 150,
          clearedDate = Some(LocalDate.of(2018, 1, 10)))
      )

      "parse successfully" in {
        Json.fromJson(testJson)(reads) shouldBe JsSuccess(expectedSeq)
      }
    }

    "there is no dateTo" should {

      val testJson: JsValue = Json.parse(
        s"""{
           |    "idType" : "VRN",
           |    "idNumber" : 555555555,
           |    "regimeType" : "VATC",
           |    "processingDate" : "2018-03-07T09:30:00.000Z",
           |    "financialTransactions" : [
           |      {
           |        "chargeType" : "$ReturnDebitCharge",
           |       "mainType" : "VAT Return Charge",
           |        "periodKey" : "17AA",
           |        "periodKeyDescription" : "ABCD",
           |        "taxPeriodFrom" : "2018-08-01",
           |        "businessPartner" : "0",
           |        "contractAccountCategory" : "99",
           |        "contractAccount" : "X",
           |        "contractObjectType" : "ABCD",
           |        "contractObject" : "0",
           |        "sapDocumentNumber" : "0",
           |        "sapDocumentNumberItem" : "0",
           |        "chargeReference" : "XD002750002155",
           |        "mainTransaction" : "1234",
           |        "subTransaction" : "5678",
           |        "originalAmount" : 150,
           |        "outstandingAmount" : 150,
           |        "items" : [
           |          {
           |            "subItem" : "000",
           |            "clearingDate" : "2018-01-10",
           |            "dueDate" : "2018-12-07",
           |            "amount" : 150
           |          }
           |        ]
           |      }
           |    ]
           |  }""".stripMargin
      )

      val expectedSeq: Seq[PaymentsHistoryModel] = Seq(
        PaymentsHistoryModel(
          chargeType = ReturnDebitCharge,
          taxPeriodFrom = Some(LocalDate.of(2018, 8, 1)),
          taxPeriodTo = None,
          amount = 150,
          clearedDate = Some(LocalDate.of(2018, 1, 10)))
      )

      "parse successfully" in {
        Json.fromJson(testJson)(reads) shouldBe JsSuccess(expectedSeq)
      }
    }

    s"charge type is $PaymentOnAccount" when {

      "clearing reason is empty" should {

        val testJson: JsValue = Json.parse(
          s"""{
             |    "idType" : "VRN",
             |    "idNumber" : "555555555",
             |    "regimeType" : "VATC",
             |    "processingDate" : "2018-03-07T09:30:00.000Z",
             |    "financialTransactions" : [
             |      {
             |        "chargeType" : "$PaymentOnAccount",
             |        "mainType" : "Payment on account",
             |        "businessPartner" : "0",
             |        "contractAccountCategory" : "99",
             |        "contractAccount" : "X",
             |        "contractObjectType" : "ABCD",
             |        "contractObject" : "0",
             |        "sapDocumentNumber" : "0",
             |        "sapDocumentNumberItem" : "0",
             |        "mainTransaction" : "1234",
             |        "subTransaction" : "5678",
             |        "originalAmount" : -5050,
             |        "outstandingAmount" : -5050,
             |        "items" : [
             |          {
             |            "subItem": "000",
             |            "dueDate": "2010-12-04",
             |            "amount": -4,
             |            "paymentReference": "654378944",
             |            "paymentAmount": 6000,
             |            "paymentMethod": "BANK GIRO RECEIPTS",
             |            "paymentLot": "RP11",
             |            "paymentLotItem": "000001"
             |          }
             |        ]
             |      }
             |    ]
             |  }""".stripMargin
        )

        val expectedSeq: Seq[PaymentsHistoryModel] = Seq(
          PaymentsHistoryModel(
            UnallocatedPayment,
            None,
            None,
            -5050,
            Some(LocalDate.of(2010, 12, 4)))
        )

        s"return $UnallocatedPayment as the charge type" in {
          Json.fromJson(testJson)(reads) shouldBe JsSuccess(expectedSeq)
        }
      }

      "clearing reason is filled" should {

        val testJson: JsValue = Json.parse(
          s"""{
             |    "idType" : "VRN",
             |    "idNumber" : "555555555",
             |    "regimeType" : "VATC",
             |    "processingDate" : "2018-03-07T09:30:00.000Z",
             |    "financialTransactions" : [
             |      {
             |        "chargeType" : "$PaymentOnAccount",
             |        "mainType" : "Payment on account",
             |        "businessPartner" : "0",
             |        "contractAccountCategory" : "99",
             |        "contractAccount" : "X",
             |        "contractObjectType" : "ABCD",
             |        "contractObject" : "0",
             |        "sapDocumentNumber" : "0",
             |        "sapDocumentNumberItem" : "0",
             |        "mainTransaction" : "1234",
             |        "subTransaction" : "5678",
             |        "originalAmount" : -5050,
             |        "outstandingAmount" : -5050,
             |        "items" : [
             |          {
             |            "subItem": "000",
             |            "dueDate": "2010-12-04",
             |            "amount": -5050,
             |            "paymentReference": "654378944",
             |            "paymentAmount": 6000,
             |            "paymentMethod": "BANK GIRO RECEIPTS",
             |            "paymentLot": "RP11",
             |            "paymentLotItem": "000001",
             |            "clearingDate": "2018-05-04",
             |            "clearingReason": "some clearing reason"
             |          }
             |        ]
             |      }
             |    ]
             |  }""".stripMargin
        )

        val expectedSeq: Seq[PaymentsHistoryModel] = Seq()

        "return an empty sequence" in {
          Json.fromJson(testJson)(reads) shouldBe JsSuccess(expectedSeq)
        }
      }
    }
  }

  "generatePaymentModel" when {

    "Charge type is not Payment on account" should {

      val chargeType = ReturnDebitCharge
      val subItem = TransactionSubItem(9, Some(LocalDate.of(2018, 1, 1)), None)
      val transaction: JsValue = Json.parse(
        """ {
          |   "taxPeriodFrom" : "2018-08-01",
          |   "taxPeriodTo" : "2018-11-01"
          | }
        """.stripMargin
      )

      val result = PaymentsHistoryModel.generatePaymentModel(chargeType, subItem, transaction).get

      "return the same charge type" in {
        result.chargeType shouldBe chargeType
      }

      "return a from date" in {
        result.taxPeriodFrom shouldBe Some(LocalDate.of(2018, 8, 1))
      }

      "return a to date" in {
        result.taxPeriodTo shouldBe Some(LocalDate.of(2018, 11, 1))
      }

      "return the amount of the sub item" in {
        result.amount shouldBe subItem.amount
      }

      "return the cleared date of the sub item" in {
        result.clearedDate shouldBe subItem.clearingDate
      }
    }

    "Charge type is Payment on account" when {

      val chargeType = PaymentOnAccount

      "clearing reason is defined" should {

        val subItem = TransactionSubItem(9000, Some(LocalDate.of(2018, 1, 1)), Some("some clearing reason"))
        val transaction: JsValue = Json.parse(
          """ {
            |   "taxPeriodFrom" : "2018-08-01",
            |   "taxPeriodTo" : "2018-11-01"
            | }
          """.stripMargin
        )

        val result = PaymentsHistoryModel.generatePaymentModel(chargeType, subItem, transaction)

        "return None" in {
          result shouldBe None
        }
      }

      "clearing reason is not defined" should {

        val subItem = TransactionSubItem(9, None, None, Some(LocalDate.of(2018, 1, 1)))
        val transaction: JsValue = Json.parse(
          """ {
            |   "outstandingAmount" : "1000"
            | }
          """.stripMargin
        )

        val result = PaymentsHistoryModel.generatePaymentModel(chargeType, subItem, transaction).get

        "return an UnallocatedPayment" in {
          result.chargeType shouldBe UnallocatedPayment
        }

        "not return a from date" in {
          result.taxPeriodFrom shouldBe None
        }

        "not return a to date" in {
          result.taxPeriodTo shouldBe None
        }

        "use the outstanding amount as the amount" in {
          result.amount shouldBe 1000
        }

        "use the due date of the sub item as cleared date" in {
          result.clearedDate shouldBe subItem.dueDate
        }
      }
    }
  }
}