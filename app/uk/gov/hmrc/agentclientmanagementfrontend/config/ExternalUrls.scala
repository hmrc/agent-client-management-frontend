/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmanagementfrontend.config

import java.net.URL

import javax.inject.{Inject, Named, Singleton}

@Singleton
class ExternalUrls @Inject()(
  @Named("contact-frontend-baseUrl") val contactFrontendBaseUrl: URL,
  @Named("appName") val appName: String) {

  val contactFrontendUrl: String = s"$contactFrontendBaseUrl/contact/problem_reports_"

  val contactFrontendAjaxUrl: String =
    s"${contactFrontendUrl}ajax?service=$appName"

  val contactFrontendNonJsUrl: String =
    s"${contactFrontendUrl}nonjs?service=$appName"
}
