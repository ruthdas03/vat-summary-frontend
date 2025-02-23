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

import config.AppConfig
import connectors.httpParsers.ResponseHttpParsers.HttpPostResult
import javax.inject.{Inject, Singleton}
import models.payments.PaymentDetailsModel
import services.MetricsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import utils.LoggerUtil

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsConnector @Inject()(http: HttpClient,
                                  appConfig: AppConfig,
                                  metrics: MetricsService) extends LoggerUtil{

  private[connectors] def vatUrl(chargeType: String): String =
    appConfig.paymentsServiceUrl + appConfig.payViewAndChange + chargeType + "/journey/start"

  def setupJourney(data: PaymentDetailsModel)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpPostResult[String]] = {

    import connectors.httpParsers.PaymentsRedirectUrlHttpParser.PaymentsRedirectUrlReads

    val timer = metrics.postSetupPaymentsJourneyTimer.time()
    val url = vatUrl(data.chargeType.toPathElement)

    http.POST(url, data)
      .map {
        case url@Right(_) =>
          timer.stop()
          url
        case httpError@Left(error) =>
          metrics.postSetupPaymentsJourneyCounter.inc()
          logger.warn("PaymentsConnector received error: " + error.message)
          httpError
      }
  }

}
