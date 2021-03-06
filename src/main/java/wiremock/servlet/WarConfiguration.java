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

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import wiremock.common.*;
import wiremock.core.MappingsSaver;
import wiremock.core.Options;
import wiremock.extension.Extension;
import wiremock.http.CaseInsensitiveKey;
import wiremock.http.HttpServerFactory;
import wiremock.http.ThreadPoolFactory;
import wiremock.http.trafficlistener.DoNothingWiremockNetworkTrafficListener;
import wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import wiremock.security.Authenticator;
import wiremock.security.NoAuthenticator;
import wiremock.standalone.JsonFileMappingsSource;
import wiremock.standalone.MappingsLoader;
import wiremock.verification.notmatched.NotMatchedRenderer;
import wiremock.verification.notmatched.PlainTextStubNotMatchedRenderer;

public class WarConfiguration implements Options {

  private static final String FILE_SOURCE_ROOT_KEY = "WireMockFileSourceRoot";

  private final ServletContext servletContext;

  public WarConfiguration(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @Override
  public int portNumber() {
    return 0;
  }

  @Override
  public HttpsSettings httpsSettings() {
    return new HttpsSettings.Builder().build();
  }

  @Override
  public JettySettings jettySettings() {
    return null;
  }

  @Override
  public int containerThreads() {
    return 0;
  }

  @Override
  public boolean browserProxyingEnabled() {
    return false;
  }

  @Override
  public ProxySettings proxyVia() {
    return ProxySettings.NO_PROXY;
  }

  @Override
  public FileSource filesRoot() {
    String fileSourceRoot = servletContext.getInitParameter(FILE_SOURCE_ROOT_KEY);
    return new ServletContextFileSource(servletContext, fileSourceRoot);
  }

  @Override
  public MappingsLoader mappingsLoader() {
    return new JsonFileMappingsSource(filesRoot().child("mappings"));
  }

  @Override
  public MappingsSaver mappingsSaver() {
    return new NotImplementedMappingsSaver();
  }

  @Override
  public Notifier notifier() {
    return null;
  }

  @Override
  public boolean requestJournalDisabled() {
    return false;
  }

  @Override
  public Optional<Integer> maxRequestJournalEntries() {
    String str = servletContext.getInitParameter("maxRequestJournalEntries");
    if (str == null) {
      return Optional.absent();
    }
    return Optional.of(Integer.parseInt(str));
  }

  @Override
  public String bindAddress() {
    return null;
  }

  @Override
  public List<CaseInsensitiveKey> matchingHeaders() {
    return Collections.emptyList();
  }

  @Override
  public boolean shouldPreserveHostHeader() {
    return false;
  }

  @Override
  public String proxyHostHeader() {
    return null;
  }

  @Override
  public HttpServerFactory httpServerFactory() {
    return null;
  }

  @Override
  public ThreadPoolFactory threadPoolFactory() {
    return null;
  }

  @Override
  public <T extends Extension> Map<String, T> extensionsOfType(Class<T> extensionType) {
    return Collections.emptyMap();
  }

  @Override
  public WiremockNetworkTrafficListener networkTrafficListener() {
    return new DoNothingWiremockNetworkTrafficListener();
  }

  @Override
  public Authenticator getAdminAuthenticator() {
    return new NoAuthenticator();
  }

  @Override
  public boolean getHttpsRequiredForAdminApi() {
    return false;
  }

  @Override
  public NotMatchedRenderer getNotMatchedRenderer() {
    return new PlainTextStubNotMatchedRenderer();
  }

  @Override
  public AsynchronousResponseSettings getAsynchronousResponseSettings() {
    return new AsynchronousResponseSettings(false, 0);
  }
}
