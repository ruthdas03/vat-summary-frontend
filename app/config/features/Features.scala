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

package config.features

import config.ConfigKeys
import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class Features @Inject()(implicit config: Configuration) {

  val userResearchBanner = new Feature(ConfigKeys.userResearchBannerFeature)
  val staticDateEnabled = new Feature(ConfigKeys.staticDateEnabledFeature)
  val vatOptOutEnabled = new Feature(ConfigKeys.vatOptOutServiceFeature)
  val mtdSignUp = new Feature(ConfigKeys.mtdSignUpFeature)
  val displayCovidMessage = new Feature(ConfigKeys.displayCovidMessageFeature)
  val r17Content = new Feature(ConfigKeys.r17Content)
  val directDebitInterrupt = new Feature(ConfigKeys.directDebitInterrupt)
}
