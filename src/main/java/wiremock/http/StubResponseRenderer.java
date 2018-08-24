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
package wiremock.http;

import static wiremock.core.WireMockApp.FILES_ROOT;
import static wiremock.http.Response.response;

import java.util.List;
import wiremock.common.BinaryFile;
import wiremock.common.FileSource;
import wiremock.extension.ResponseTransformer;
import wiremock.global.GlobalSettingsHolder;

public class StubResponseRenderer implements ResponseRenderer {

  private final FileSource fileSource;
  private final GlobalSettingsHolder globalSettingsHolder;
  private final ProxyResponseRenderer proxyResponseRenderer;
  private final List<ResponseTransformer> responseTransformers;

  public StubResponseRenderer(
      FileSource fileSource,
      GlobalSettingsHolder globalSettingsHolder,
      ProxyResponseRenderer proxyResponseRenderer,
      List<ResponseTransformer> responseTransformers) {
    this.fileSource = fileSource;
    this.globalSettingsHolder = globalSettingsHolder;
    this.proxyResponseRenderer = proxyResponseRenderer;
    this.responseTransformers = responseTransformers;
  }

  @Override
  public Response render(ResponseDefinition responseDefinition) {
    if (!responseDefinition.wasConfigured()) {
      return Response.notConfigured();
    }

    Response response = buildResponse(responseDefinition);
    return applyTransformations(
        responseDefinition.getOriginalRequest(),
        responseDefinition,
        response,
        responseTransformers);
  }

  private Response buildResponse(ResponseDefinition responseDefinition) {
    if (responseDefinition.isProxyResponse()) {
      return proxyResponseRenderer.render(responseDefinition);
    } else {
      Response.Builder responseBuilder = renderDirectly(responseDefinition);
      return responseBuilder.build();
    }
  }

  private Response applyTransformations(
      Request request,
      ResponseDefinition responseDefinition,
      Response response,
      List<ResponseTransformer> transformers) {
    if (transformers.isEmpty()) {
      return response;
    }

    ResponseTransformer transformer = transformers.get(0);
    Response newResponse =
        transformer.applyGlobally() || responseDefinition.hasTransformer(transformer)
            ? transformer.transform(
                request,
                response,
                fileSource.child(FILES_ROOT),
                responseDefinition.getTransformerParameters())
            : response;

    return applyTransformations(
        request, responseDefinition, newResponse, transformers.subList(1, transformers.size()));
  }

  private Response.Builder renderDirectly(ResponseDefinition responseDefinition) {
    Response.Builder responseBuilder =
        response()
            .status(responseDefinition.getStatus())
            .statusMessage(responseDefinition.getStatusMessage())
            .headers(responseDefinition.getHeaders())
            .fault(responseDefinition.getFault())
            .configureDelay(
                globalSettingsHolder.get().getFixedDelay(),
                globalSettingsHolder.get().getDelayDistribution(),
                responseDefinition.getFixedDelayMilliseconds(),
                responseDefinition.getDelayDistribution())
            .chunkedDribbleDelay(responseDefinition.getChunkedDribbleDelay());

    if (responseDefinition.specifiesBodyFile()) {
      BinaryFile bodyFile = fileSource.getBinaryFileNamed(responseDefinition.getBodyFileName());
      responseBuilder.body(bodyFile);
    } else if (responseDefinition.specifiesBodyContent()) {
      if (responseDefinition.specifiesBinaryBodyContent()) {
        responseBuilder.body(responseDefinition.getByteBody());
      } else {
        responseBuilder.body(responseDefinition.getByteBody());
      }
    }

    return responseBuilder;
  }
}
