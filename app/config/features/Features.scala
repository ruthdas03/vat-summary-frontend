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

package config.features

import config.ConfigKeys
import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class Features @Inject()(config: Configuration) {

  val userResearchBanner = new Feature(ConfigKeys.userResearchBannerFeature, config)
  val vatCertNSTPs = new Feature(ConfigKeys.vatCertNSTPsFeature, config)
  val staticDateEnabled = new Feature(ConfigKeys.staticDateEnabledFeature, config)
  val paymentsAndRepaymentsEnabled = new Feature(ConfigKeys.paymentsAndRepaymentsFeature, config)
  val vatOptOutEnabled = new Feature(ConfigKeys.vatOptOutServiceFeature, config)
  val enableVatObligationsService = new Feature(ConfigKeys.useVatObligationsService, config)
  val useDirectDebitDummyPage = new Feature(ConfigKeys.useDirectDebitDummyPageFeature, config)
  val useLanguageSelector = new Feature(ConfigKeys.useLanguageSelectorFeature, config)
  val submitReturnFeatures = new Feature(ConfigKeys.submitReturnFeatures, config)
  val agentAccess = new Feature(ConfigKeys.agentAccessFeature, config)
  val mtdSignUp = new Feature(ConfigKeys.mtdSignUpFeature, config)
  val ddCollectionInProgressEnabled = new Feature(ConfigKeys.ddCollectionInProgressFeature, config)

}
