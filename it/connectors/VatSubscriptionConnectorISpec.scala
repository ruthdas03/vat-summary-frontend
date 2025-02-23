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
import models._
import models.errors.ServerSideError
import stubs.CustomerInfoStub
import stubs.CustomerInfoStub.customerInfoJson
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers._


class VatSubscriptionConnectorISpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    val connector: VatSubscriptionConnector = app.injector.instanceOf[VatSubscriptionConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "calling getCustomerInfo" should {

    "return a user's customer information" in new Test {
      override def setupStubs(): StubMapping = CustomerInfoStub.stubCustomerInfo(
        customerInfoJson(isPartialMigration = false, hasVerifiedEmail = true)
      )

      val expected = Right(CustomerInformation(
        CustomerDetails(
          Some("Betty"),
          Some("Jones"),
          Some("Cheapo Clothing"),
          Some("Cheapo Clothing Ltd"),
          isInsolvent = false,
          Some(true),
          Some("01"),
          Some("2018-01-01"),
          Some("2017-01-01")
        ),
        Address("Bedrock Quarry", Some("Bedrock"), Some("Graveldon"), None, Some("GV2 4BB")),
        Some(Email(Some("bettylucknexttime@gmail.com"), Some(true))),
        isHybridUser = false,
        Some("2017-05-05"),
        Some("2017-05-06"),
        Some("7"),
        Some("10410"),
        Some("MM"),
        Some(List(
          TaxPeriod("2018-01-01", "2018-01-15"),
          TaxPeriod("2018-01-06", "2018-01-28"))
        ),
        Some(TaxPeriod("2018-01-29", "2018-01-31")),
        Some("MTDfB Voluntary"),
        Some(Deregistration(Some(LocalDate.parse("2020-01-01")))),
        Some(ChangeIndicators(deregister = false)),
        isMissingTrader = false,
        mandationStatus = "MTDfB",
        hasPendingPpobChanges = false
      ))

      setupStubs()
      private val result = await(connector.getCustomerInfo("1111"))

      result shouldEqual expected
    }

    "return an HttpError if one is received" in new Test {
      override def setupStubs(): StubMapping = CustomerInfoStub.stubErrorFromApi()

      val message: String = """{"code":"500","message":"INTERNAL_SERVER_ERROR"}"""
      val expected = Left(ServerSideError("500", message))

      setupStubs()
      private val result = await(connector.getCustomerInfo("1111"))

      result shouldEqual expected
    }
  }
}
