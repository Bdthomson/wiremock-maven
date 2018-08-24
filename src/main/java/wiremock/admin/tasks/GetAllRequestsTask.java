/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wiremock.admin.tasks;

import static java.net.HttpURLConnection.HTTP_OK;
import static wiremock.client.ResponseDefinitionBuilder.responseDefinition;

import wiremock.admin.AdminTask;
import wiremock.admin.LimitAndSinceDatePaginator;
import wiremock.admin.model.GetServeEventsResult;
import wiremock.admin.model.PathParams;
import wiremock.common.Json;
import wiremock.core.Admin;
import wiremock.http.Request;
import wiremock.http.ResponseDefinition;

public class GetAllRequestsTask implements AdminTask {

  @Override
  public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
    GetServeEventsResult serveEventsResult = admin.getServeEvents();
    GetServeEventsResult result =
        new GetServeEventsResult(
            LimitAndSinceDatePaginator.fromRequest(serveEventsResult.getRequests(), request),
            serveEventsResult.isRequestJournalDisabled());

    return responseDefinition()
        .withStatus(HTTP_OK)
        .withBody(Json.write(result))
        .withHeader("Content-Type", "application/json")
        .build();
  }
}
