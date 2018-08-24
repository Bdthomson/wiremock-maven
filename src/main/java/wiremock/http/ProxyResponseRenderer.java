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

import static wiremock.common.HttpClientUtils.getEntityAsByteArrayAndCloseStream;
import static wiremock.http.RequestMethod.PATCH;
import static wiremock.http.RequestMethod.POST;
import static wiremock.http.RequestMethod.PUT;
import static wiremock.http.Response.response;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import wiremock.common.KeyStoreSettings;
import wiremock.common.ProxySettings;
import wiremock.global.GlobalSettingsHolder;

public class ProxyResponseRenderer implements ResponseRenderer {

  private static final int MINUTES = 1000 * 60;
  private static final String TRANSFER_ENCODING = "transfer-encoding";
  private static final String CONTENT_ENCODING = "content-encoding";
  private static final String CONTENT_LENGTH = "content-length";
  private static final String HOST_HEADER = "host";

  private final HttpClient client;
  private final boolean preserveHostHeader;
  private final String hostHeaderValue;
  private final GlobalSettingsHolder globalSettingsHolder;

  public ProxyResponseRenderer(
      ProxySettings proxySettings,
      KeyStoreSettings trustStoreSettings,
      boolean preserveHostHeader,
      String hostHeaderValue,
      GlobalSettingsHolder globalSettingsHolder) {
    this.globalSettingsHolder = globalSettingsHolder;
    client = HttpClientFactory.createClient(1000, 5 * MINUTES, proxySettings, trustStoreSettings);

    this.preserveHostHeader = preserveHostHeader;
    this.hostHeaderValue = hostHeaderValue;
  }

  @Override
  public Response render(ResponseDefinition responseDefinition) {
    HttpUriRequest httpRequest = getHttpRequestFor(responseDefinition);
    addRequestHeaders(httpRequest, responseDefinition);

    try {
      addBodyIfPostPutOrPatch(httpRequest, responseDefinition);
      HttpResponse httpResponse = client.execute(httpRequest);

      return response()
          .status(httpResponse.getStatusLine().getStatusCode())
          .headers(headersFrom(httpResponse, responseDefinition))
          .body(getEntityAsByteArrayAndCloseStream(httpResponse))
          .fromProxy(true)
          .configureDelay(
              globalSettingsHolder.get().getFixedDelay(),
              globalSettingsHolder.get().getDelayDistribution(),
              responseDefinition.getFixedDelayMilliseconds(),
              responseDefinition.getDelayDistribution())
          .chunkedDribbleDelay(responseDefinition.getChunkedDribbleDelay())
          .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private HttpHeaders headersFrom(
      HttpResponse httpResponse, ResponseDefinition responseDefinition) {
    List<HttpHeader> httpHeaders = new LinkedList<HttpHeader>();
    for (Header header : httpResponse.getAllHeaders()) {
      httpHeaders.add(new HttpHeader(header.getName(), header.getValue()));
    }

    if (responseDefinition.getHeaders() != null) {
      httpHeaders.addAll(responseDefinition.getHeaders().all());
    }

    return new HttpHeaders(httpHeaders);
  }

  public static HttpUriRequest getHttpRequestFor(ResponseDefinition response) {
    final RequestMethod method = response.getOriginalRequest().getMethod();
    final String url = response.getProxyUrl();
    return HttpClientFactory.getHttpRequestFor(method, url);
  }

  private void addRequestHeaders(HttpRequest httpRequest, ResponseDefinition response) {
    Request originalRequest = response.getOriginalRequest();
    for (String key : originalRequest.getAllHeaderKeys()) {
      if (headerShouldBeTransferred(key)) {
        if (!HOST_HEADER.equalsIgnoreCase(key) || preserveHostHeader) {
          List<String> values = originalRequest.header(key).values();
          for (String value : values) {
            httpRequest.addHeader(key, value);
          }
        } else {
          if (hostHeaderValue != null) {
            httpRequest.addHeader(key, hostHeaderValue);
          } else if (response.getProxyBaseUrl() != null) {
            httpRequest.addHeader(key, URI.create(response.getProxyBaseUrl()).getAuthority());
          }
        }
      }
    }

    if (response.getAdditionalProxyRequestHeaders() != null) {
      for (String key : response.getAdditionalProxyRequestHeaders().keys()) {
        httpRequest.setHeader(
            key, response.getAdditionalProxyRequestHeaders().getHeader(key).firstValue());
      }
    }
  }

  private static boolean headerShouldBeTransferred(String key) {
    return !ImmutableList.of(CONTENT_LENGTH, TRANSFER_ENCODING, "connection")
        .contains(key.toLowerCase());
  }

  private static void addBodyIfPostPutOrPatch(HttpRequest httpRequest, ResponseDefinition response)
      throws UnsupportedEncodingException {
    Request originalRequest = response.getOriginalRequest();
    if (originalRequest.getMethod().isOneOf(PUT, POST, PATCH)) {
      HttpEntityEnclosingRequest requestWithEntity = (HttpEntityEnclosingRequest) httpRequest;
      requestWithEntity.setEntity(buildEntityFrom(originalRequest));
    }
  }

  private static HttpEntity buildEntityFrom(Request originalRequest) {
    ContentTypeHeader contentTypeHeader = originalRequest.contentTypeHeader().or("text/plain");
    ContentType contentType =
        ContentType.create(
            contentTypeHeader.mimeTypePart(), contentTypeHeader.encodingPart().or("utf-8"));

    if (originalRequest.containsHeader(TRANSFER_ENCODING)
        && originalRequest.header(TRANSFER_ENCODING).firstValue().equals("chunked")) {
      return applyGzipWrapperIfRequired(
          originalRequest,
          new InputStreamEntity(
              new ByteArrayInputStream(originalRequest.getBody()), -1, contentType));
    }

    return applyGzipWrapperIfRequired(
        originalRequest, new ByteArrayEntity(originalRequest.getBody()));
  }

  private static HttpEntity applyGzipWrapperIfRequired(
      Request originalRequest, HttpEntity content) {
    if (originalRequest.containsHeader(CONTENT_ENCODING)
        && originalRequest.header(CONTENT_ENCODING).firstValue().contains("gzip")) {
      return new GzipCompressingEntity(content);
    }

    return content;
  }
}
