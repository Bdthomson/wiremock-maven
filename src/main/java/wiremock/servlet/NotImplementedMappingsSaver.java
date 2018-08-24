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

import java.util.List;
import wiremock.core.MappingsSaver;
import wiremock.stubbing.StubMapping;

public class NotImplementedMappingsSaver implements MappingsSaver {
  @Override
  public void save(List<StubMapping> stubMappings) {
    throw new UnsupportedOperationException("Saving mappings is not supported");
  }

  @Override
  public void save(StubMapping stubMapping) {
    throw new UnsupportedOperationException("Saving mapping is not supported");
  }

  @Override
  public void remove(StubMapping stubMapping) {
    throw new UnsupportedOperationException("Remove mapping is not supported");
  }

  @Override
  public void removeAll() {
    throw new UnsupportedOperationException("Remove all mappings is not supported");
  }
}
