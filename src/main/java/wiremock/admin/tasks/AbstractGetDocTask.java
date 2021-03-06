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

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static wiremock.client.ResponseDefinitionBuilder.responseDefinition;

import com.google.common.io.Resources;
import java.io.IOException;
import wiremock.admin.AdminTask;
import wiremock.admin.model.PathParams;
import wiremock.core.Admin;
import wiremock.http.Request;
import wiremock.http.ResponseDefinition;

public abstract class AbstractGetDocTask implements AdminTask {

  @Override
  public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
    try {
      byte[] content = toByteArray(Resources.getResource(getFilePath()).openStream());
      return responseDefinition()
          .withStatus(200)
          .withBody(content)
          .withHeader(CONTENT_TYPE, getMimeType())
          .build();
    } catch (IOException e) {
      return responseDefinition().withStatus(500).build();
    }
  }

  protected abstract String getMimeType();

  protected abstract String getFilePath();
}
