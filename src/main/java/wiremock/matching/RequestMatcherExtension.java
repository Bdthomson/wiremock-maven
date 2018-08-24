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
package wiremock.matching;

import static wiremock.matching.MatchResult.exactMatch;
import static wiremock.matching.MatchResult.noMatch;

import wiremock.extension.Extension;
import wiremock.extension.Parameters;
import wiremock.http.Request;

public abstract class RequestMatcherExtension extends RequestMatcher implements Extension {

  @Override
  public MatchResult match(Request request) {
    return match(request, Parameters.empty());
  }

  public abstract MatchResult match(Request request, Parameters parameters);

  @Override
  public String getName() {
    return "inline";
  }

  public static final RequestMatcherExtension ALWAYS =
      new RequestMatcherExtension() {
        @Override
        public MatchResult match(Request request, Parameters parameters) {
          return exactMatch();
        }
      };

  public static final RequestMatcherExtension NEVER =
      new RequestMatcherExtension() {
        @Override
        public MatchResult match(Request request, Parameters parameters) {
          return noMatch();
        }
      };
}
