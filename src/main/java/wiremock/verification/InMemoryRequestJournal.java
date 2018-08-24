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
package wiremock.verification;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.tryFind;
import static wiremock.matching.RequestPattern.thatMatch;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import wiremock.http.Request;
import wiremock.matching.RequestMatcherExtension;
import wiremock.matching.RequestPattern;
import wiremock.stubbing.ServeEvent;

public class InMemoryRequestJournal implements RequestJournal {

  private final Queue<ServeEvent> serveEvents = new ConcurrentLinkedQueue<ServeEvent>();

  private final Optional<Integer> maxEntries;

  public InMemoryRequestJournal(Optional<Integer> maxEntries) {
    if (maxEntries.isPresent() && maxEntries.get() < 0) {
      throw new IllegalArgumentException(
          "Maximum number of entries of journal must be greater than zero");
    }
    this.maxEntries = maxEntries;
  }

  @Override
  public int countRequestsMatching(RequestPattern requestPattern) {
    return size(filter(getRequests(), thatMatch(requestPattern)));
  }

  @Override
  public List<LoggedRequest> getRequestsMatching(RequestPattern requestPattern) {
    return ImmutableList.copyOf(filter(getRequests(), thatMatch(requestPattern)));
  }

  private Predicate<Request> matchedBy(final RequestPattern requestPattern) {
    return new Predicate<Request>() {
      public boolean apply(Request input) {
        return requestPattern.isMatchedBy(
            input, Collections.<String, RequestMatcherExtension>emptyMap());
      }
    };
  }

  @Override
  public void requestReceived(ServeEvent serveEvent) {
    serveEvents.add(serveEvent);
    removeOldEntries();
  }

  @Override
  public List<ServeEvent> getAllServeEvents() {
    return ImmutableList.copyOf(serveEvents).reverse();
  }

  @Override
  public Optional<ServeEvent> getServeEvent(final UUID id) {
    return tryFind(
        serveEvents,
        new Predicate<ServeEvent>() {
          @Override
          public boolean apply(ServeEvent input) {
            return input.getId().equals(id);
          }
        });
  }

  @Override
  public void reset() {
    serveEvents.clear();
  }

  private Iterable<LoggedRequest> getRequests() {
    return transform(
        serveEvents,
        new Function<ServeEvent, LoggedRequest>() {
          public LoggedRequest apply(ServeEvent input) {
            return input.getRequest();
          }
        });
  }

  private void removeOldEntries() {
    if (maxEntries.isPresent()) {
      while (serveEvents.size() > maxEntries.get()) {
        serveEvents.poll();
      }
    }
  }
}
