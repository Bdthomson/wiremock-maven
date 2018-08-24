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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public abstract class PathPatternJsonSerializer<T extends PathPattern> extends JsonSerializer<T> {

  @Override
  public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    gen.writeStartObject();

    if (value.isSimple()) {
      gen.writeStringField(value.getName(), value.getExpected());
    } else {
      gen.writeObjectFieldStart(value.getName());
      gen.writeStringField("expression", value.getExpected());
      gen.writeStringField(
          value.getValuePattern().getName(), value.getValuePattern().getExpected());
      gen.writeEndObject();
    }

    serializeAdditionalFields(value, gen, serializers);

    gen.writeEndObject();
  }

  protected abstract void serializeAdditionalFields(
      T value, JsonGenerator gen, SerializerProvider serializers) throws IOException;
}
