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
package wiremock.standalone;

import static com.google.common.collect.Iterables.filter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static wiremock.common.AbstractFileSource.byFileExtension;
import static wiremock.core.WireMockApp.FILES_ROOT;
import static wiremock.core.WireMockApp.MAPPINGS_ROOT;

import wiremock.client.ResponseDefinitionBuilder;
import wiremock.client.WireMock;
import wiremock.common.BinaryFile;
import wiremock.common.ContentTypes;
import wiremock.common.FileSource;
import wiremock.common.TextFile;
import wiremock.http.ContentTypeHeader;
import wiremock.http.HttpHeaders;
import wiremock.stubbing.StubMapping;

public class RemoteMappingsLoader {

  private final FileSource mappingsFileSource;
  private final FileSource filesFileSource;
  private final WireMock wireMock;

  public RemoteMappingsLoader(FileSource fileSource, WireMock wireMock) {
    this.mappingsFileSource = fileSource.child(MAPPINGS_ROOT);
    this.filesFileSource = fileSource.child(FILES_ROOT);
    this.wireMock = wireMock;
  }

  public void load() {
    Iterable<TextFile> mappingFiles =
        filter(mappingsFileSource.listFilesRecursively(), byFileExtension("json"));
    for (TextFile mappingFile : mappingFiles) {
      StubMapping mapping = StubMapping.buildFrom(mappingFile.readContentsAsString());
      convertBodyFromFileIfNecessary(mapping);
      wireMock.register(mapping);
    }
  }

  private void convertBodyFromFileIfNecessary(StubMapping mapping) {
    String bodyFileName = mapping.getResponse().getBodyFileName();
    if (bodyFileName != null) {
      ResponseDefinitionBuilder responseDefinitionBuilder =
          ResponseDefinitionBuilder.like(mapping.getResponse()).withBodyFile(null);

      String extension = substringAfterLast(bodyFileName, ".");
      String mimeType = getMimeType(mapping);

      if (ContentTypes.determineIsText(extension, mimeType)) {
        TextFile bodyFile = filesFileSource.getTextFileNamed(bodyFileName);
        responseDefinitionBuilder.withBody(bodyFile.readContentsAsString());
      } else {
        BinaryFile bodyFile = filesFileSource.getBinaryFileNamed(bodyFileName);
        responseDefinitionBuilder.withBody(bodyFile.readContents());
      }

      mapping.setResponse(responseDefinitionBuilder.build());
    }
  }

  private String getMimeType(StubMapping mapping) {
    HttpHeaders responseHeaders = mapping.getResponse().getHeaders();
    if (responseHeaders != null) {
      ContentTypeHeader contentTypeHeader = responseHeaders.getContentTypeHeader();
      return contentTypeHeader != null ? contentTypeHeader.mimeTypePart() : null;
    }

    return null;
  }
}
