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
package wiremock.verification.notmatched;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import java.util.List;
import wiremock.client.ResponseDefinitionBuilder;
import wiremock.core.Admin;
import wiremock.http.Request;
import wiremock.http.ResponseDefinition;
import wiremock.verification.LoggedRequest;
import wiremock.verification.NearMiss;
import wiremock.verification.diff.Diff;
import wiremock.verification.diff.PlainTextDiffRenderer;

public class PlainTextStubNotMatchedRenderer extends NotMatchedRenderer {

  public static final String CONSOLE_WIDTH_HEADER_KEY = "X-WireMock-Console-Width";

  @Override
  public ResponseDefinition render(Admin admin, Request request) {
    LoggedRequest loggedRequest =
        LoggedRequest.createFrom(request.getOriginalRequest().or(request));
    List<NearMiss> nearMisses = admin.findTopNearMissesFor(loggedRequest).getNearMisses();

    PlainTextDiffRenderer diffRenderer =
        loggedRequest.containsHeader(CONSOLE_WIDTH_HEADER_KEY)
            ? new PlainTextDiffRenderer(
                Integer.parseInt(loggedRequest.getHeader(CONSOLE_WIDTH_HEADER_KEY)))
            : new PlainTextDiffRenderer();

    String body;
    if (nearMisses.isEmpty()) {
      body = "No response could be served as there are no stub mappings in this WireMock instance.";
    } else {
      Diff firstDiff = nearMisses.get(0).getDiff();
      body = diffRenderer.render(firstDiff);
    }

    return ResponseDefinitionBuilder.responseDefinition()
        .withStatus(404)
        .withHeader(CONTENT_TYPE, "text/plain")
        .withBody(body)
        .build();
  }
}
