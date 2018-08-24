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

import static com.github.tomakehurst.wiremock.core.WireMockApp.FILES_ROOT;
import static com.github.tomakehurst.wiremock.core.WireMockApp.MAPPINGS_ROOT;
import static java.lang.System.out;
import static wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static wiremock.client.WireMock.anyUrl;
import static wiremock.http.RequestMethod.ANY;
import static wiremock.matching.RequestPatternBuilder.newRequestPattern;

import wiremock.WireMockServer;
import wiremock.common.FatalStartupException;
import wiremock.common.FileSource;
import wiremock.http.ResponseDefinition;
import wiremock.matching.RequestPattern;
import wiremock.stubbing.StubMapping;
import wiremock.stubbing.StubMappings;

public class WireMockServerRunner {

  private static final String BANNER =
      " /$$      /$$ /$$                     /$$      /$$                     /$$      \n"
          + "| $$  /$ | $$|__/                    | $$$    /$$$                    | $$      \n"
          + "| $$ /$$$| $$ /$$  /$$$$$$   /$$$$$$ | $$$$  /$$$$  /$$$$$$   /$$$$$$$| $$   /$$\n"
          + "| $$/$$ $$ $$| $$ /$$__  $$ /$$__  $$| $$ $$/$$ $$ /$$__  $$ /$$_____/| $$  /$$/\n"
          + "| $$$$_  $$$$| $$| $$  \\__/| $$$$$$$$| $$  $$$| $$| $$  \\ $$| $$      | $$$$$$/ \n"
          + "| $$$/ \\  $$$| $$| $$      | $$_____/| $$\\  $ | $$| $$  | $$| $$      | $$_  $$ \n"
          + "| $$/   \\  $$| $$| $$      |  $$$$$$$| $$ \\/  | $$|  $$$$$$/|  $$$$$$$| $$ \\  $$\n"
          + "|__/     \\__/|__/|__/       \\_______/|__/     |__/ \\______/  \\_______/|__/  \\__/";

  static {
    System.setProperty("org.mortbay.log.class", "wiremock.jetty.LoggerAdapter");
  }

  private WireMockServer wireMockServer;

  public void run(String... args) {
    CommandLineOptions options = new CommandLineOptions(args);
    if (options.help()) {
      out.println(options.helpText());
      return;
    }

    FileSource fileSource = options.filesRoot();
    fileSource.createIfNecessary();
    FileSource filesFileSource = fileSource.child(FILES_ROOT);
    filesFileSource.createIfNecessary();
    FileSource mappingsFileSource = fileSource.child(MAPPINGS_ROOT);
    mappingsFileSource.createIfNecessary();

    wireMockServer = new WireMockServer(options);

    if (options.recordMappingsEnabled()) {
      wireMockServer.enableRecordMappings(mappingsFileSource, filesFileSource);
    }

    if (options.specifiesProxyUrl()) {
      addProxyMapping(options.proxyUrl());
    }

    try {
      wireMockServer.start();
      options.setResultingPort(wireMockServer.port());
      if (!options.bannerDisabled()) {
        out.println(BANNER);
        out.println();
      } else {
        out.println();
        out.println("The WireMock server is started .....");
      }
      out.println(options);
    } catch (FatalStartupException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private void addProxyMapping(final String baseUrl) {
    wireMockServer.loadMappingsUsing(
        new MappingsLoader() {
          @Override
          public void loadMappingsInto(StubMappings stubMappings) {
            RequestPattern requestPattern = newRequestPattern(ANY, anyUrl()).build();
            ResponseDefinition responseDef = responseDefinition().proxiedFrom(baseUrl).build();

            StubMapping proxyBasedMapping = new StubMapping(requestPattern, responseDef);
            proxyBasedMapping.setPriority(
                10); // Make it low priority so that existing stubs will take precedence
            stubMappings.addMapping(proxyBasedMapping);
          }
        });
  }

  public void stop() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  public boolean isRunning() {
    return wireMockServer.isRunning();
  }

  public int port() {
    return wireMockServer.port();
  }

  public static void main(String... args) {
    new WireMockServerRunner().run(args);
  }
}
