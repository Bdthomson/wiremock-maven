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
import static wiremock.client.ResponseDefinitionBuilder.jsonResponse;

import wiremock.admin.AdminTask;
import wiremock.admin.model.PathParams;
import wiremock.common.Json;
import wiremock.core.Admin;
import wiremock.http.Request;
import wiremock.http.ResponseDefinition;
import wiremock.recording.RecordSpec;
import wiremock.recording.SnapshotRecordResult;

public class SnapshotTask implements AdminTask {

  @Override
  public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
    RecordSpec recordSpec =
        request.getBody().length == 0
            ? RecordSpec.DEFAULTS
            : Json.read(request.getBodyAsString(), RecordSpec.class);

    SnapshotRecordResult result = admin.snapshotRecord(recordSpec);
    return jsonResponse(result, HTTP_OK);
  }
}
