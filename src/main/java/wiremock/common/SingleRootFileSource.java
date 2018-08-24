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
package wiremock.common;

import java.io.File;

public class SingleRootFileSource extends AbstractFileSource {

  public SingleRootFileSource(File rootDirectory) {
    super(rootDirectory);
  }

  public SingleRootFileSource(String rootPath) {
    super(new File(rootPath));
  }

  @Override
  public FileSource child(String subDirectoryName) {
    return new SingleRootFileSource(new File(rootDirectory, subDirectoryName));
  }

  @Override
  protected boolean readOnly() {
    return false;
  }

  @Override
  public String toString() {
    return SingleRootFileSource.class.getSimpleName() + ": " + rootDirectory;
  }
}
