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
package wiremock;

import static wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.List;
import java.util.UUID;
import wiremock.admin.model.GetScenariosResult;
import wiremock.admin.model.GetServeEventsResult;
import wiremock.admin.model.ListStubMappingsResult;
import wiremock.admin.model.SingleServedStubResult;
import wiremock.admin.model.SingleStubMappingResult;
import wiremock.client.MappingBuilder;
import wiremock.client.WireMock;
import wiremock.common.FatalStartupException;
import wiremock.common.FileSource;
import wiremock.common.Notifier;
import wiremock.common.ProxySettings;
import wiremock.core.Admin;
import wiremock.core.Container;
import wiremock.core.Options;
import wiremock.core.WireMockApp;
import wiremock.global.GlobalSettings;
import wiremock.global.GlobalSettingsHolder;
import wiremock.http.HttpServer;
import wiremock.http.HttpServerFactory;
import wiremock.http.RequestListener;
import wiremock.http.StubRequestHandler;
import wiremock.junit.Stubbing;
import wiremock.matching.RequestPattern;
import wiremock.matching.RequestPatternBuilder;
import wiremock.matching.StringValuePattern;
import wiremock.recording.RecordSpec;
import wiremock.recording.RecordSpecBuilder;
import wiremock.recording.RecordingStatusResult;
import wiremock.recording.SnapshotRecordResult;
import wiremock.standalone.MappingsLoader;
import wiremock.stubbing.ServeEvent;
import wiremock.stubbing.StubMapping;
import wiremock.stubbing.StubMappingJsonRecorder;
import wiremock.verification.FindNearMissesResult;
import wiremock.verification.FindRequestsResult;
import wiremock.verification.LoggedRequest;
import wiremock.verification.NearMiss;
import wiremock.verification.VerificationResult;

public class WireMockServer implements Container, Stubbing, Admin {

  private final WireMockApp wireMockApp;
  private final StubRequestHandler stubRequestHandler;

  private final HttpServer httpServer;
  private final Notifier notifier;

  private final Options options;

  protected final WireMock client;

  public WireMockServer(Options options) {
    this.options = options;
    this.notifier = options.notifier();

    wireMockApp = new WireMockApp(options, this);

    this.stubRequestHandler = wireMockApp.buildStubRequestHandler();
    HttpServerFactory httpServerFactory = options.httpServerFactory();
    httpServer =
        httpServerFactory.buildHttpServer(
            options, wireMockApp.buildAdminRequestHandler(), stubRequestHandler);

    client = new WireMock(wireMockApp);
  }

  public WireMockServer(
      int port,
      Integer httpsPort,
      FileSource fileSource,
      boolean enableBrowserProxying,
      ProxySettings proxySettings,
      Notifier notifier) {
    this(
        wireMockConfig()
            .port(port)
            .httpsPort(httpsPort)
            .fileSource(fileSource)
            .enableBrowserProxying(enableBrowserProxying)
            .proxyVia(proxySettings)
            .notifier(notifier));
  }

  public WireMockServer(
      int port, FileSource fileSource, boolean enableBrowserProxying, ProxySettings proxySettings) {
    this(
        wireMockConfig()
            .port(port)
            .fileSource(fileSource)
            .enableBrowserProxying(enableBrowserProxying)
            .proxyVia(proxySettings));
  }

  public WireMockServer(int port, FileSource fileSource, boolean enableBrowserProxying) {
    this(
        wireMockConfig()
            .port(port)
            .fileSource(fileSource)
            .enableBrowserProxying(enableBrowserProxying));
  }

  public WireMockServer(int port) {
    this(wireMockConfig().port(port));
  }

  public WireMockServer(int port, Integer httpsPort) {
    this(wireMockConfig().port(port).httpsPort(httpsPort));
  }

  public WireMockServer() {
    this(wireMockConfig());
  }

  public void loadMappingsUsing(final MappingsLoader mappingsLoader) {
    wireMockApp.loadMappingsUsing(mappingsLoader);
  }

  public GlobalSettingsHolder getGlobalSettingsHolder() {
    return wireMockApp.getGlobalSettingsHolder();
  }

  public void addMockServiceRequestListener(RequestListener listener) {
    stubRequestHandler.addRequestListener(listener);
  }

  public void enableRecordMappings(FileSource mappingsFileSource, FileSource filesFileSource) {
    addMockServiceRequestListener(
        new StubMappingJsonRecorder(
            mappingsFileSource, filesFileSource, wireMockApp, options.matchingHeaders()));
    notifier.info("Recording mappings to " + mappingsFileSource.getPath());
  }

  public void stop() {
    httpServer.stop();
  }

  public void start() {
    try {
      httpServer.start();
    } catch (Exception e) {
      throw new FatalStartupException(e);
    }
  }

  /**
   * Gracefully shutdown the server.
   *
   * <p>This method assumes it is being called as the result of an incoming HTTP request.
   */
  @Override
  public void shutdown() {
    final WireMockServer server = this;
    Thread shutdownThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  // We have to sleep briefly to finish serving the shutdown request before stopping
                  // the server, as
                  // there's no support in Jetty for shutting down after the current request.
                  // See http://stackoverflow.com/questions/4650713
                  Thread.sleep(100);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
                server.stop();
              }
            });
    shutdownThread.start();
  }

  public int port() {
    //    com.google.common.base.Preconditions
    //    com.google.common.base.Preconditions.checkState(isRunning(),
    //        "Not listening on HTTP port. The WireMock server is most likely stopped");
    return httpServer.port();
  }

  public int httpsPort() {
    //    com.google.common.base.Preconditions.checkState(isRunning() && options.httpsSettings()
    //            .enabled(),
    //        "Not listening on HTTPS port. Either HTTPS is not enabled or the WireMock server is
    // stopped.");
    return httpServer.httpsPort();
  }

  public String url(String path) {
    boolean https = options.httpsSettings().enabled();
    String protocol = https ? "https" : "http";
    int port = https ? httpsPort() : port();

    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    return String.format("%s://localhost:%d%s", protocol, port, path);
  }

  public boolean isRunning() {
    return httpServer.isRunning();
  }

  @Override
  public StubMapping givenThat(MappingBuilder mappingBuilder) {
    return client.register(mappingBuilder);
  }

  @Override
  public StubMapping stubFor(MappingBuilder mappingBuilder) {
    return givenThat(mappingBuilder);
  }

  @Override
  public void editStub(MappingBuilder mappingBuilder) {
    client.editStubMapping(mappingBuilder);
  }

  @Override
  public void removeStub(MappingBuilder mappingBuilder) {
    client.removeStubMapping(mappingBuilder);
  }

  @Override
  public void removeStub(StubMapping stubMapping) {
    client.removeStubMapping(stubMapping);
  }

  @Override
  public List<StubMapping> getStubMappings() {
    return client.allStubMappings().getMappings();
  }

  @Override
  public StubMapping getSingleStubMapping(UUID id) {
    return client.getStubMapping(id).getItem();
  }

  @Override
  public List<StubMapping> findStubMappingsByMetadata(StringValuePattern pattern) {
    return client.findAllStubsByMetadata(pattern);
  }

  @Override
  public void removeStubMappingsByMetadata(StringValuePattern pattern) {
    client.removeStubsByMetadataPattern(pattern);
  }

  @Override
  public void removeStubMapping(StubMapping stubMapping) {
    wireMockApp.removeStubMapping(stubMapping);
  }

  @Override
  public void verify(RequestPatternBuilder requestPatternBuilder) {
    client.verifyThat(requestPatternBuilder);
  }

  @Override
  public void verify(int count, RequestPatternBuilder requestPatternBuilder) {
    client.verifyThat(count, requestPatternBuilder);
  }

  @Override
  public List<LoggedRequest> findAll(RequestPatternBuilder requestPatternBuilder) {
    return client.find(requestPatternBuilder);
  }

  @Override
  public List<ServeEvent> getAllServeEvents() {
    return client.getServeEvents();
  }

  @Override
  public void setGlobalFixedDelay(int milliseconds) {
    client.setGlobalFixedDelayVariable(milliseconds);
  }

  @Override
  public List<LoggedRequest> findAllUnmatchedRequests() {
    return client.findAllUnmatchedRequests();
  }

  @Override
  public List<NearMiss> findNearMissesForAllUnmatchedRequests() {
    return client.findNearMissesForAllUnmatchedRequests();
  }

  @Override
  public List<NearMiss> findAllNearMissesFor(RequestPatternBuilder requestPatternBuilder) {
    return client.findAllNearMissesFor(requestPatternBuilder);
  }

  @Override
  public List<NearMiss> findNearMissesFor(LoggedRequest loggedRequest) {
    return client.findTopNearMissesFor(loggedRequest);
  }

  @Override
  public void addStubMapping(StubMapping stubMapping) {
    wireMockApp.addStubMapping(stubMapping);
  }

  @Override
  public void editStubMapping(StubMapping stubMapping) {
    wireMockApp.editStubMapping(stubMapping);
  }

  @Override
  public ListStubMappingsResult listAllStubMappings() {
    return wireMockApp.listAllStubMappings();
  }

  @Override
  public SingleStubMappingResult getStubMapping(UUID id) {
    return wireMockApp.getStubMapping(id);
  }

  @Override
  public void saveMappings() {
    wireMockApp.saveMappings();
  }

  @Override
  public void resetAll() {
    wireMockApp.resetAll();
  }

  @Override
  public void resetRequests() {
    wireMockApp.resetRequests();
  }

  @Override
  public void resetToDefaultMappings() {
    wireMockApp.resetToDefaultMappings();
  }

  @Override
  public GetServeEventsResult getServeEvents() {
    return wireMockApp.getServeEvents();
  }

  @Override
  public SingleServedStubResult getServedStub(UUID id) {
    return wireMockApp.getServedStub(id);
  }

  @Override
  public void resetScenarios() {
    wireMockApp.resetScenarios();
  }

  @Override
  public void resetMappings() {
    wireMockApp.resetMappings();
  }

  @Override
  public VerificationResult countRequestsMatching(RequestPattern requestPattern) {
    return wireMockApp.countRequestsMatching(requestPattern);
  }

  @Override
  public FindRequestsResult findRequestsMatching(RequestPattern requestPattern) {
    return wireMockApp.findRequestsMatching(requestPattern);
  }

  @Override
  public FindRequestsResult findUnmatchedRequests() {
    return wireMockApp.findUnmatchedRequests();
  }

  @Override
  public void updateGlobalSettings(GlobalSettings newSettings) {
    wireMockApp.updateGlobalSettings(newSettings);
  }

  @Override
  public FindNearMissesResult findNearMissesForUnmatchedRequests() {
    return wireMockApp.findNearMissesForUnmatchedRequests();
  }

  @Override
  public GetScenariosResult getAllScenarios() {
    return wireMockApp.getAllScenarios();
  }

  @Override
  public FindNearMissesResult findTopNearMissesFor(LoggedRequest loggedRequest) {
    return wireMockApp.findTopNearMissesFor(loggedRequest);
  }

  @Override
  public FindNearMissesResult findTopNearMissesFor(RequestPattern requestPattern) {
    return wireMockApp.findTopNearMissesFor(requestPattern);
  }

  @Override
  public void startRecording(String targetBaseUrl) {
    wireMockApp.startRecording(targetBaseUrl);
  }

  @Override
  public void startRecording(RecordSpec spec) {
    wireMockApp.startRecording(spec);
  }

  @Override
  public void startRecording(RecordSpecBuilder recordSpec) {
    wireMockApp.startRecording(recordSpec);
  }

  @Override
  public SnapshotRecordResult stopRecording() {
    return wireMockApp.stopRecording();
  }

  @Override
  public RecordingStatusResult getRecordingStatus() {
    return wireMockApp.getRecordingStatus();
  }

  @Override
  public SnapshotRecordResult snapshotRecord() {
    return wireMockApp.snapshotRecord();
  }

  @Override
  public SnapshotRecordResult snapshotRecord(RecordSpecBuilder spec) {
    return wireMockApp.snapshotRecord(spec);
  }

  @Override
  public SnapshotRecordResult snapshotRecord(RecordSpec spec) {
    return wireMockApp.snapshotRecord(spec);
  }

  @Override
  public Options getOptions() {
    return options;
  }

  @Override
  public void shutdownServer() {
    shutdown();
  }

  @Override
  public ListStubMappingsResult findAllStubsByMetadata(StringValuePattern pattern) {
    return wireMockApp.findAllStubsByMetadata(pattern);
  }

  @Override
  public void removeStubsByMetadata(StringValuePattern pattern) {
    wireMockApp.removeStubsByMetadata(pattern);
  }
}
