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

import static wiremock.core.WireMockConfiguration.wireMockConfig;

import wiremock.WireMockServer;
import wiremock.client.VerificationException;
import wiremock.client.WireMock;
import wiremock.core.Options;
import wiremock.verification.LoggedRequest;
import wiremock.verification.NearMiss;
import java.util.List;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class WireMockRule extends WireMockServer implements MethodRule, TestRule {

    private final boolean failOnUnmatchedRequests;

    public WireMockRule(Options options) {
        this(options, true);
    }

    public WireMockRule(Options options, boolean failOnUnmatchedRequests) {
        super(options);
        this.failOnUnmatchedRequests = failOnUnmatchedRequests;
    }

    public WireMockRule(int port) {
		this(wireMockConfig().port(port));
	}

    public WireMockRule(int port, Integer httpsPort) {
        this(wireMockConfig().port(port).httpsPort(httpsPort));
    }
	
	public WireMockRule() {
		this(wireMockConfig());
	}

    @Override
    public Statement apply(final Statement base, Description description) {
        return apply(base, null, null);
    }

	@Override
	public Statement apply(final Statement base, FrameworkMethod method, Object target) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				start();
				WireMock.configureFor("localhost", port());
				try {
                    before();
                    base.evaluate();
                    checkForUnmatchedRequests();
                } finally {
                    after();
                    stop();
                }
			}

		};
	}

    private void checkForUnmatchedRequests() {
        if (failOnUnmatchedRequests) {
            List<LoggedRequest> unmatchedRequests = findAllUnmatchedRequests();
            if (!unmatchedRequests.isEmpty()) {
                List<NearMiss> nearMisses = findNearMissesForAllUnmatchedRequests();
                if (nearMisses.isEmpty()) {
                    throw VerificationException.forUnmatchedRequests(unmatchedRequests);
                } else {
                    throw VerificationException.forUnmatchedNearMisses(nearMisses);
                }
            }
        }
    }

    protected void before() {
        // NOOP
    }

    protected void after() {
        // NOOP
    }
}
