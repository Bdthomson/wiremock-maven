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

import static wiremock.common.Exceptions.throwUnchecked;

import com.google.common.io.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

public class KeyStoreSettings {

  public static final KeyStoreSettings NO_STORE = new KeyStoreSettings(null, null);

  private final String path;
  private final String password;

  public KeyStoreSettings(String path, String password) {
    this.path = path;
    this.password = password;
  }

  public String path() {
    return path;
  }

  public String password() {
    return password;
  }

  public KeyStore loadStore() {
    InputStream instream = null;
    try {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      instream = createInputStream();
      trustStore.load(instream, password.toCharArray());
      return trustStore;
    } catch (Exception e) {
      return throwUnchecked(e, KeyStore.class);
    } finally {
      if (instream != null) {
        try {
          instream.close();
        } catch (IOException ioe) {
          throwUnchecked(ioe);
        }
      }
    }
  }

  private InputStream createInputStream() throws IOException {
    if (new File(path).isFile()) {
      return new FileInputStream(path);
    } else {
      return Resources.getResource(path).openStream();
    }
  }
}
