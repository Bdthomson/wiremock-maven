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
package wiremock.extension.responsetemplating;

import static com.google.common.base.MoreObjects.firstNonNull;
import static wiremock.common.Exceptions.throwUnchecked;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.AssignHelper;
import com.github.jknack.handlebars.helper.NumberHelper;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import wiremock.client.ResponseDefinitionBuilder;
import wiremock.common.FileSource;
import wiremock.common.TextFile;
import wiremock.extension.Parameters;
import wiremock.extension.ResponseDefinitionTransformer;
import wiremock.extension.responsetemplating.helpers.WireMockHelpers;
import wiremock.http.HttpHeader;
import wiremock.http.HttpHeaders;
import wiremock.http.Request;
import wiremock.http.ResponseDefinition;

public class ResponseTemplateTransformer extends ResponseDefinitionTransformer {

  public static final String NAME = "response-template";

  private final boolean global;

  private final Handlebars handlebars;

  public ResponseTemplateTransformer(boolean global) {
    this(global, Collections.<String, Helper>emptyMap());
  }

  public ResponseTemplateTransformer(boolean global, String helperName, Helper helper) {
    this(global, ImmutableMap.of(helperName, helper));
  }

  public ResponseTemplateTransformer(boolean global, Map<String, Helper> helpers) {
    this(global, new Handlebars(), helpers);
  }

  public ResponseTemplateTransformer(
      boolean global, Handlebars handlebars, Map<String, Helper> helpers) {
    this.global = global;
    this.handlebars = handlebars;

    for (StringHelpers helper : StringHelpers.values()) {
      if (!helper.name().equals("now")) {
        this.handlebars.registerHelper(helper.name(), helper);
      }
    }

    for (NumberHelper helper : NumberHelper.values()) {
      this.handlebars.registerHelper(helper.name(), helper);
    }

    this.handlebars.registerHelper(AssignHelper.NAME, new AssignHelper());

    // Add all available wiremock helpers
    for (WireMockHelpers helper : WireMockHelpers.values()) {
      this.handlebars.registerHelper(helper.name(), helper);
    }

    for (Map.Entry<String, Helper> entry : helpers.entrySet()) {
      this.handlebars.registerHelper(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public boolean applyGlobally() {
    return global;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public ResponseDefinition transform(
      Request request,
      ResponseDefinition responseDefinition,
      FileSource files,
      Parameters parameters) {
    ResponseDefinitionBuilder newResponseDefBuilder =
        ResponseDefinitionBuilder.like(responseDefinition);
    final ImmutableMap<String, Object> model =
        ImmutableMap.<String, Object>builder()
            .put("parameters", firstNonNull(parameters, Collections.<String, Object>emptyMap()))
            .put("request", RequestTemplateModel.from(request))
            .build();

    if (responseDefinition.specifiesTextBodyContent()) {
      Template bodyTemplate = uncheckedCompileTemplate(responseDefinition.getBody());
      applyTemplatedResponseBody(newResponseDefBuilder, model, bodyTemplate);
    } else if (responseDefinition.specifiesBodyFile()) {
      TextFile file = files.getTextFileNamed(responseDefinition.getBodyFileName());
      Template bodyTemplate = uncheckedCompileTemplate(file.readContentsAsString());
      applyTemplatedResponseBody(newResponseDefBuilder, model, bodyTemplate);
    }

    if (responseDefinition.getHeaders() != null) {
      Iterable<HttpHeader> newResponseHeaders =
          Iterables.transform(
              responseDefinition.getHeaders().all(),
              new Function<HttpHeader, HttpHeader>() {
                @Override
                public HttpHeader apply(HttpHeader input) {
                  List<String> newValues =
                      Lists.transform(
                          input.values(),
                          new Function<String, String>() {
                            @Override
                            public String apply(String input) {
                              Template template = uncheckedCompileTemplate(input);
                              return uncheckedApplyTemplate(template, model);
                            }
                          });

                  return new HttpHeader(input.key(), newValues);
                }
              });
      newResponseDefBuilder.withHeaders(new HttpHeaders(newResponseHeaders));
    }

    if (responseDefinition.getProxyBaseUrl() != null) {
      Template proxyBaseUrlTemplate =
          uncheckedCompileTemplate(responseDefinition.getProxyBaseUrl());
      String newProxyBaseUrl = uncheckedApplyTemplate(proxyBaseUrlTemplate, model);
      newResponseDefBuilder.proxiedFrom(newProxyBaseUrl);
    }

    return newResponseDefBuilder.build();
  }

  private void applyTemplatedResponseBody(
      ResponseDefinitionBuilder newResponseDefBuilder,
      ImmutableMap<String, Object> model,
      Template bodyTemplate) {
    String newBody = uncheckedApplyTemplate(bodyTemplate, model);
    newResponseDefBuilder.withBody(newBody);
  }

  private String uncheckedApplyTemplate(Template template, Object context) {
    try {
      return template.apply(context);
    } catch (IOException e) {
      return throwUnchecked(e, String.class);
    }
  }

  private Template uncheckedCompileTemplate(String content) {
    try {
      return handlebars.compileInline(content);
    } catch (IOException e) {
      return throwUnchecked(e, Template.class);
    }
  }
}
