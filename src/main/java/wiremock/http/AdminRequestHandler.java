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

import static wiremock.common.LocalNotifier.notifier;
import static wiremock.core.WireMockApp.ADMIN_CONTEXT_ROOT;

import java.net.URI;
import wiremock.admin.AdminRoutes;
import wiremock.admin.AdminTask;
import wiremock.admin.AdminUriTemplate;
import wiremock.admin.NotFoundException;
import wiremock.admin.model.PathParams;
import wiremock.common.InvalidInputException;
import wiremock.core.Admin;
import wiremock.security.Authenticator;
import wiremock.stubbing.ServeEvent;
import wiremock.verification.LoggedRequest;

public class AdminRequestHandler extends AbstractRequestHandler {

  private final AdminRoutes adminRoutes;
  private final Admin admin;
  private final Authenticator authenticator;
  private final boolean requireHttps;

  public AdminRequestHandler(
      AdminRoutes adminRoutes,
      Admin admin,
      ResponseRenderer responseRenderer,
      Authenticator authenticator,
      boolean requireHttps) {
    super(responseRenderer);
    this.adminRoutes = adminRoutes;
    this.admin = admin;
    this.authenticator = authenticator;
    this.requireHttps = requireHttps;
  }

  @Override
  public ServeEvent handleRequest(Request request) {
    if (requireHttps && !URI.create(request.getAbsoluteUrl()).getScheme().equals("https")) {
      notifier().info("HTTPS is required for admin requests, sending upgrade redirect");
      return ServeEvent.of(
          LoggedRequest.createFrom(request),
          ResponseDefinition.notPermitted("HTTPS is required for accessing the admin API"));
    }

    if (!authenticator.authenticate(request)) {
      notifier().info("Authentication failed for " + request.getMethod() + " " + request.getUrl());
      return ServeEvent.of(LoggedRequest.createFrom(request), ResponseDefinition.notAuthorised());
    }

    notifier()
        .info(
            "Received request to " + request.getUrl() + " with body " + request.getBodyAsString());
    String path = URI.create(withoutAdminRoot(request.getUrl())).getPath();

    try {
      AdminTask adminTask = adminRoutes.taskFor(request.getMethod(), path);

      AdminUriTemplate uriTemplate =
          adminRoutes.requestSpecForTask(adminTask.getClass()).getUriTemplate();
      PathParams pathParams = uriTemplate.parse(path);

      return ServeEvent.of(
          LoggedRequest.createFrom(request), adminTask.execute(admin, request, pathParams));
    } catch (NotFoundException e) {
      return ServeEvent.forUnmatchedRequest(LoggedRequest.createFrom(request));
    } catch (InvalidInputException iie) {
      return ServeEvent.forBadRequest(LoggedRequest.createFrom(request), iie.getErrors());
    }
  }

  private static String withoutAdminRoot(String url) {
    return url.replace(ADMIN_CONTEXT_ROOT, "");
  }
}
