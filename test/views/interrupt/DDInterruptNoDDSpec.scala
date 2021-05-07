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

package views.interrupt

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.ViewBaseSpec
import views.html.interrupt.DDInterruptNoDD

class DDInterruptNoDDSpec extends ViewBaseSpec {

  object Selectors {
    val title = "title"
    val text = "h1"
  }

  lazy val DDInterruptView: DDInterruptNoDD = injector.instanceOf[DDInterruptNoDD]

  "The DD interrupt screen for users" should {

    lazy val view = DDInterruptView()
    implicit lazy val document: Document = Jsoup.parse(view.body)

    "have the correct title" in {

      elementText(Selectors.title) shouldBe "Direct debit interrupt screen - VAT - GOV.UK"

    }

    "have the correct text" in {

      elementText(Selectors.text) shouldBe "Direct debit interrupt screen for migrated users with no existing DD"

    }
  }
}
