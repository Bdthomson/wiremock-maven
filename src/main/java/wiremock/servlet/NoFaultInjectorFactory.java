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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import wiremock.core.FaultInjector;

public class NoFaultInjectorFactory implements FaultInjectorFactory {

  @Override
  public FaultInjector buildFaultInjector(
      HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
    return new NoFaultInjector(httpServletResponse);
  }
}
