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
package wiremock.servlet;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.Collections.list;
import static wiremock.common.Encoding.encodeBase64;
import static wiremock.common.Exceptions.throwUnchecked;
import static wiremock.common.Strings.stringFromBytes;
import static wiremock.common.Urls.splitQuery;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import wiremock.common.Gzip;
import wiremock.http.ContentTypeHeader;
import wiremock.http.Cookie;
import wiremock.http.HttpHeader;
import wiremock.http.HttpHeaders;
import wiremock.http.QueryParameter;
import wiremock.http.Request;
import wiremock.http.RequestMethod;
import wiremock.jetty9.JettyUtils;

public class WireMockHttpServletRequestAdapter implements Request {

  public static final String ORIGINAL_REQUEST_KEY = "wiremock.ORIGINAL_REQUEST";

  private final HttpServletRequest request;
  private byte[] cachedBody;
  private String urlPrefixToRemove;
  private Collection<Part> cachedMultiparts;

  public WireMockHttpServletRequestAdapter(HttpServletRequest request) {
    this.request = request;
  }

  public WireMockHttpServletRequestAdapter(HttpServletRequest request, String urlPrefixToRemove) {
    this.request = request;
    this.urlPrefixToRemove = urlPrefixToRemove;
  }

  @Override
  public String getUrl() {
    String url = request.getRequestURI();

    String contextPath = request.getContextPath();
    if (!isNullOrEmpty(contextPath) && url.startsWith(contextPath)) {
      url = url.substring(contextPath.length());
    }
    if (!isNullOrEmpty(urlPrefixToRemove) && url.startsWith(urlPrefixToRemove)) {
      url = url.substring(urlPrefixToRemove.length());
    }

    return withQueryStringIfPresent(url);
  }

  @Override
  public String getAbsoluteUrl() {
    return withQueryStringIfPresent(request.getRequestURL().toString());
  }

  private String withQueryStringIfPresent(String url) {
    return url + (isNullOrEmpty(request.getQueryString()) ? "" : "?" + request.getQueryString());
  }

  @Override
  public RequestMethod getMethod() {
    return RequestMethod.fromString(request.getMethod().toUpperCase());
  }

  @Override
  public String getScheme() {
    return request.getScheme();
  }

  @Override
  public String getHost() {
    return request.getServerName();
  }

  @Override
  public int getPort() {
    return request.getServerPort();
  }

  @Override
  public String getClientIp() {
    String forwardedForHeader = this.getHeader("X-Forwarded-For");

    if (forwardedForHeader != null && forwardedForHeader.length() > 0) {
      return forwardedForHeader;
    }

    return request.getRemoteAddr();
  }

  @Override
  public byte[] getBody() {
    if (cachedBody == null) {
      try {
        byte[] body = toByteArray(request.getInputStream());
        boolean isGzipped = hasGzipEncoding() || Gzip.isGzipped(body);
        cachedBody = isGzipped ? Gzip.unGzip(body) : body;
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }

    return cachedBody;
  }

  private Charset encodingFromContentTypeHeaderOrUtf8() {
    ContentTypeHeader contentTypeHeader = contentTypeHeader();
    if (contentTypeHeader != null) {
      return contentTypeHeader.charset();
    }
    return UTF_8;
  }

  private boolean hasGzipEncoding() {
    String encodingHeader = request.getHeader("Content-Encoding");
    return encodingHeader != null && encodingHeader.contains("gzip");
  }

  @Override
  public String getBodyAsString() {
    return stringFromBytes(getBody(), encodingFromContentTypeHeaderOrUtf8());
  }

  @Override
  public String getBodyAsBase64() {
    return encodeBase64(getBody());
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getHeader(String key) {
    List<String> headerNames = list(request.getHeaderNames());
    for (String currentKey : headerNames) {
      if (currentKey.toLowerCase().equals(key.toLowerCase())) {
        return request.getHeader(currentKey);
      }
    }

    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public HttpHeader header(String key) {
    List<String> headerNames = list(request.getHeaderNames());
    for (String currentKey : headerNames) {
      if (currentKey.toLowerCase().equals(key.toLowerCase())) {
        List<String> valueList = list(request.getHeaders(currentKey));
        if (valueList.isEmpty()) {
          return HttpHeader.empty(key);
        }

        return new HttpHeader(key, valueList);
      }
    }

    return HttpHeader.absent(key);
  }

  @Override
  public ContentTypeHeader contentTypeHeader() {
    return getHeaders().getContentTypeHeader();
  }

  @Override
  public boolean containsHeader(String key) {
    return header(key).isPresent();
  }

  @Override
  public HttpHeaders getHeaders() {
    List<HttpHeader> headerList = newArrayList();
    for (String key : getAllHeaderKeys()) {
      headerList.add(header(key));
    }

    return new HttpHeaders(headerList);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<String> getAllHeaderKeys() {
    LinkedHashSet<String> headerKeys = new LinkedHashSet<String>();
    for (Enumeration<String> headerNames = request.getHeaderNames();
        headerNames.hasMoreElements(); ) {
      headerKeys.add(headerNames.nextElement());
    }

    return headerKeys;
  }

  @Override
  public Map<String, Cookie> getCookies() {
    ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();

    javax.servlet.http.Cookie[] cookies =
        firstNonNull(request.getCookies(), new javax.servlet.http.Cookie[0]);
    for (javax.servlet.http.Cookie cookie : cookies) {
      builder.put(cookie.getName(), cookie.getValue());
    }

    return Maps.transformValues(
        builder.build().asMap(),
        new Function<Collection<String>, Cookie>() {
          @Override
          public Cookie apply(Collection<String> input) {
            return new Cookie(null, ImmutableList.copyOf(input));
          }
        });
  }

  @Override
  public QueryParameter queryParameter(String key) {
    return firstNonNull(
        (splitQuery(request.getQueryString()).get(key)), QueryParameter.absent(key));
  }

  @Override
  public boolean isBrowserProxyRequest() {
    if (!isJetty()) {
      return false;
    }
    if (request instanceof org.eclipse.jetty.server.Request) {
      org.eclipse.jetty.server.Request jettyRequest = (org.eclipse.jetty.server.Request) request;
      return JettyUtils.getUri(jettyRequest).isAbsolute();
    }

    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<Part> getParts() {
    if (!isMultipart()) {
      return null;
    }

    if (cachedMultiparts == null) {
      try {
        String contentTypeHeaderValue = from(contentTypeHeader().values()).join(Joiner.on(" "));
        InputStream inputStream = new ByteArrayInputStream(getBody());
        MultiPartInputStreamParser inputStreamParser =
            new MultiPartInputStreamParser(inputStream, contentTypeHeaderValue, null, null);
        request.setAttribute(
            org.eclipse.jetty.server.Request.__MULTIPART_INPUT_STREAM, inputStreamParser);
        cachedMultiparts =
            from(safelyGetRequestParts())
                .transform(
                    new Function<javax.servlet.http.Part, Part>() {
                      @Override
                      public Part apply(javax.servlet.http.Part input) {
                        return WireMockHttpServletMultipartAdapter.from(input);
                      }
                    })
                .toList();
      } catch (IOException | ServletException exception) {
        return throwUnchecked(exception, Collection.class);
      }
    }

    return (cachedMultiparts.size() > 0) ? cachedMultiparts : null;
  }

  private Collection<javax.servlet.http.Part> safelyGetRequestParts()
      throws IOException, ServletException {
    try {
      return request.getParts();
    } catch (IOException ioe) {
      if (ioe.getMessage().contains("Missing content for multipart")) {
        return Collections.emptyList();
      }

      throw ioe;
    }
  }

  @Override
  public boolean isMultipart() {
    String header = getHeader("Content-Type");
    return (header != null && header.contains("multipart"));
  }

  @Override
  public Part getPart(final String name) {
    if (name == null || name.length() == 0) {
      return null;
    }
    if (cachedMultiparts == null) {
      if (getParts() == null) {
        return null;
      }
    }
    return from(cachedMultiparts)
        .firstMatch(
            new Predicate<Part>() {
              @Override
              public boolean apply(Part input) {
                return name.equals(input.getName());
              }
            })
        .get();
  }

  @Override
  public Optional<Request> getOriginalRequest() {
    Request originalRequest = (Request) request.getAttribute(ORIGINAL_REQUEST_KEY);
    return Optional.fromNullable(originalRequest);
  }

  private boolean isJetty() {
    try {
      getClass("org.eclipse.jetty.server.Request");
      return true;
    } catch (Exception e) {
    }
    return false;
  }

  private void getClass(String type) throws ClassNotFoundException {
    ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
    ClassLoader loader =
        contextCL == null ? WireMockHttpServletRequestAdapter.class.getClassLoader() : contextCL;
    Class.forName(type, false, loader);
  }

  @Override
  public String toString() {
    return request.toString() + getBodyAsString();
  }
}
