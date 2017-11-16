/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import play.api.http.Status
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import play.twirl.api.Html
import services.BtaStubService
import services.EnrolmentsAuthService
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials

import scala.concurrent.{ExecutionContext, Future}

class BtaStubControllerSpec extends ControllerBaseSpec {

  private trait Test {
    val success: Boolean = true
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockService: BtaStubService = mock[BtaStubService]

    def setup(): Any = if(success) {
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.successful(()))

      (mockService.getPartial()(_: Request[AnyContent]))
        .expects(*)
        .returns(Future.successful(Html("Some HTML")))
    }
    else {
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.failed(MissingBearerToken()))
    }

    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)

    def target: BtaStubController = {
      setup()
      new BtaStubController(messages, mockEnrolmentsAuthService, mockService, mockAppConfig)
    }
  }

  "Calling the landingPage action" when {

    "the user is logged in" should {

      "return 200" in new Test {
        private val result = target.landingPage()(fakeRequest)
        status(result) shouldBe Status.OK
      }

      "return HTML" in new Test {
        private val result = target.landingPage()(fakeRequest)
        contentType(result) shouldBe Some("text/html")
      }

      "return charset utf-8" in new Test {
        private val result = target.landingPage()(fakeRequest)
        charset(result) shouldBe Some("utf-8")
      }
    }

    "the user is not logged in" should {

      "return 303" in new Test {
        override val success: Boolean = false
        val result: Future[Result] = target.landingPage()(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect the user to the session timeout page" in new Test {
        override val success: Boolean = false
        val result: Future[Result] = target.landingPage()(fakeRequest)
        redirectLocation(result) shouldBe Some(routes.ErrorsController.sessionTimeout().url)
      }
    }
  }
}