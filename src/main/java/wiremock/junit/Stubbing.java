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
package wiremock.junit;

import wiremock.client.MappingBuilder;
import wiremock.matching.RequestPatternBuilder;
import wiremock.matching.StringValuePattern;
import wiremock.stubbing.ServeEvent;
import wiremock.stubbing.StubMapping;
import wiremock.verification.LoggedRequest;
import wiremock.verification.NearMiss;
import java.util.List;
import java.util.UUID;

public interface Stubbing {

    StubMapping givenThat(MappingBuilder mappingBuilder);
    StubMapping stubFor(MappingBuilder mappingBuilder);
    void editStub(MappingBuilder mappingBuilder);
    void removeStub(MappingBuilder mappingBuilder);
    void removeStub(StubMapping mappingBuilder);

    List<StubMapping> getStubMappings();
    StubMapping getSingleStubMapping(UUID id);

    List<StubMapping> findStubMappingsByMetadata(StringValuePattern pattern);
    void removeStubMappingsByMetadata(StringValuePattern pattern);

    void verify(RequestPatternBuilder requestPatternBuilder);
    void verify(int count, RequestPatternBuilder requestPatternBuilder);
    List<LoggedRequest> findAll(RequestPatternBuilder requestPatternBuilder);
    List<ServeEvent> getAllServeEvents();

    void setGlobalFixedDelay(int milliseconds);

    List<LoggedRequest> findAllUnmatchedRequests();
    List<NearMiss> findNearMissesForAllUnmatchedRequests();
    List<NearMiss> findNearMissesFor(LoggedRequest loggedRequest);
    List<NearMiss> findAllNearMissesFor(RequestPatternBuilder requestPatternBuilder);
}
