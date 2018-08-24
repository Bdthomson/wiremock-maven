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
package wiremock.client;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static wiremock.client.WireMock.aResponse;

import java.util.Map;
import java.util.UUID;
import wiremock.common.Metadata;
import wiremock.extension.Parameters;
import wiremock.http.Request;
import wiremock.http.RequestMethod;
import wiremock.http.ResponseDefinition;
import wiremock.matching.*;
import wiremock.stubbing.StubMapping;

class BasicMappingBuilder implements ScenarioMappingBuilder {

  private RequestPatternBuilder requestPatternBuilder;
  private ResponseDefinitionBuilder responseDefBuilder;
  private Integer priority;
  private String scenarioName;
  private String requiredScenarioState;
  private String newScenarioState;
  private UUID id = UUID.randomUUID();
  private String name;
  private boolean isPersistent = false;
  private Map<String, Parameters> postServeActions = newLinkedHashMap();
  private Metadata metadata;

  BasicMappingBuilder(RequestMethod method, UrlPattern urlPattern) {
    requestPatternBuilder = new RequestPatternBuilder(method, urlPattern);
  }

  BasicMappingBuilder(ValueMatcher<Request> requestMatcher) {
    requestPatternBuilder = new RequestPatternBuilder(requestMatcher);
  }

  BasicMappingBuilder(String customRequestMatcherName, Parameters parameters) {
    requestPatternBuilder = new RequestPatternBuilder(customRequestMatcherName, parameters);
  }

  @Override
  public BasicMappingBuilder willReturn(ResponseDefinitionBuilder responseDefBuilder) {
    this.responseDefBuilder = responseDefBuilder;
    return this;
  }

  @Override
  public BasicMappingBuilder atPriority(Integer priority) {
    this.priority = priority;
    return this;
  }

  @Override
  public BasicMappingBuilder withHeader(String key, StringValuePattern headerPattern) {
    requestPatternBuilder.withHeader(key, headerPattern);
    return this;
  }

  @Override
  public BasicMappingBuilder withCookie(String name, StringValuePattern cookieValuePattern) {
    requestPatternBuilder.withCookie(name, cookieValuePattern);
    return this;
  }

  @Override
  public BasicMappingBuilder withQueryParam(String key, StringValuePattern queryParamPattern) {
    requestPatternBuilder.withQueryParam(key, queryParamPattern);
    return this;
  }

  @Override
  public BasicMappingBuilder withQueryParams(Map<String, StringValuePattern> queryParams) {
    for (Map.Entry<String, StringValuePattern> queryParam : queryParams.entrySet())
      requestPatternBuilder.withQueryParam(queryParam.getKey(), queryParam.getValue());
    return this;
  }

  @Override
  public BasicMappingBuilder withRequestBody(ContentPattern<?> bodyPattern) {
    requestPatternBuilder.withRequestBody(bodyPattern);
    return this;
  }

  @Override
  public BasicMappingBuilder withMultipartRequestBody(
      MultipartValuePatternBuilder multipartPatternBuilder) {
    requestPatternBuilder.withRequestBodyPart(multipartPatternBuilder.build());
    return this;
  }

  @Override
  public BasicMappingBuilder inScenario(String scenarioName) {
    checkArgument(scenarioName != null, "Scenario name must not be null");

    this.scenarioName = scenarioName;
    return this;
  }

  @Override
  public BasicMappingBuilder whenScenarioStateIs(String stateName) {
    this.requiredScenarioState = stateName;
    return this;
  }

  @Override
  public BasicMappingBuilder willSetStateTo(String stateName) {
    this.newScenarioState = stateName;
    return this;
  }

  @Override
  public BasicMappingBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  @Override
  public BasicMappingBuilder withName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public ScenarioMappingBuilder persistent() {
    this.isPersistent = true;
    return this;
  }

  @Override
  public BasicMappingBuilder withBasicAuth(String username, String password) {
    requestPatternBuilder.withBasicAuth(new BasicCredentials(username, password));
    return this;
  }

  @Override
  public <P> BasicMappingBuilder withPostServeAction(String extensionName, P parameters) {
    Parameters params =
        parameters instanceof Parameters ? (Parameters) parameters : Parameters.of(parameters);
    postServeActions.put(extensionName, params);
    return this;
  }

  @Override
  public BasicMappingBuilder withMetadata(Map<String, ?> metadataMap) {
    this.metadata = new Metadata(metadataMap);
    return this;
  }

  @Override
  public BasicMappingBuilder withMetadata(Metadata metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public BasicMappingBuilder withMetadata(Metadata.Builder metadata) {
    this.metadata = metadata.build();
    return this;
  }

  @Override
  public StubMapping build() {
    if (scenarioName == null && (requiredScenarioState != null || newScenarioState != null)) {
      throw new IllegalStateException(
          "Scenario name must be specified to require or set a new scenario state");
    }
    RequestPattern requestPattern = requestPatternBuilder.build();
    ResponseDefinition response = firstNonNull(responseDefBuilder, aResponse()).build();
    StubMapping mapping = new StubMapping(requestPattern, response);
    mapping.setPriority(priority);
    mapping.setScenarioName(scenarioName);
    mapping.setRequiredScenarioState(requiredScenarioState);
    mapping.setNewScenarioState(newScenarioState);
    mapping.setUuid(id);
    mapping.setName(name);
    mapping.setPersistent(isPersistent);

    mapping.setPostServeActions(postServeActions.isEmpty() ? null : postServeActions);

    mapping.setMetadata(metadata);

    return mapping;
  }
}
