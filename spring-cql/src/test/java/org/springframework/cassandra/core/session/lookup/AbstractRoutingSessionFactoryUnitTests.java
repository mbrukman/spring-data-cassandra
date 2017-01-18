/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cassandra.core.session.lookup;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cassandra.core.session.DefaultSessionFactory;

import com.datastax.driver.core.Session;

/**
 * Unit tests for {@link AbstractRoutingSessionFactory}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractRoutingSessionFactoryUnitTests {

	@Mock Session defaultSession;
	@Mock Session routedSession;

	StubbedRoutingSessionFactory sut;

	@Before
	public void before() throws Exception {

		sut = new StubbedRoutingSessionFactory();

		sut.setDefaultTargetSessionFactory(new DefaultSessionFactory(defaultSession));
	}

	@Test // DATACASS-330
	public void shouldDetermineRoutedRepository() {

		sut.setTargetSessionFactories(Collections.singletonMap("key", new DefaultSessionFactory(routedSession)));
		sut.afterPropertiesSet();

		sut.setLookupKey("key");

		assertThat(sut.getSession()).isSameAs(routedSession);
	}

	@Test // DATACASS-330
	public void shouldFallbackToDefaultSession() {

		sut.setTargetSessionFactories(Collections.singletonMap("key", new DefaultSessionFactory(routedSession)));
		sut.afterPropertiesSet();

		sut.setLookupKey("unknown");

		assertThat(sut.getSession()).isSameAs(defaultSession);
	}

	@Test // DATACASS-330
	public void initializationShouldFailUnsupportedLookupKey() {

		sut.setTargetSessionFactories(Collections.singletonMap("key", new Object()));
		try {
			sut.afterPropertiesSet();
			fail("Missing IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertThat(e).hasMessageContaining("Illegal session factory value.");
		}
	}

	@Test // DATACASS-330
	public void initializationShouldFailUnresolvableKey() {

		sut.setTargetSessionFactories(Collections.singletonMap("key", "value"));
		sut.setSessionFactoryLookup(new MapSessionFactoryLookup());

		try {
			sut.afterPropertiesSet();
			fail("Missing SessionFactoryLookupFailureException");
		} catch (SessionFactoryLookupFailureException e) {
			assertThat(e).hasMessageContaining("No SessionFactory with name [value] registered");
		}
	}

	@Test // DATACASS-330
	public void unresolvableSessionRetrievalShouldFail() {

		sut.setLenientFallback(false);
		sut.setTargetSessionFactories(Collections.singletonMap("key", new DefaultSessionFactory(routedSession)));
		sut.afterPropertiesSet();

		sut.setLookupKey("unknown");

		try {
			sut.getSession();
			fail("Missing IllegalStateException");
		} catch (RuntimeException e) {}
	}

	@Test // DATACASS-330
	public void sessionRetrievalWithoutLookupKeyShouldReturnDefaultSession() {

		sut.setTargetSessionFactories(Collections.singletonMap("key", new DefaultSessionFactory(routedSession)));
		sut.afterPropertiesSet();

		sut.setLookupKey(null);

		assertThat(sut.getSession()).isSameAs(defaultSession);
	}

	@Test // DATACASS-330
	public void shouldLookupFromMap() {

		MapSessionFactoryLookup lookup = new MapSessionFactoryLookup("lookup-key",
				new DefaultSessionFactory(routedSession));

		sut.setSessionFactoryLookup(lookup);
		sut.setTargetSessionFactories(Collections.singletonMap("my-key", "lookup-key"));
		sut.afterPropertiesSet();

		sut.setLookupKey("my-key");

		assertThat(sut.getSession()).isSameAs(routedSession);
	}

	@Test // DATACASS-330
	public void shouldAllowModificationsAfterInitialization() {

		MapSessionFactoryLookup lookup = new MapSessionFactoryLookup();

		sut.setSessionFactoryLookup(lookup);
		sut.setTargetSessionFactories((Map) lookup.getSessionFactories());
		sut.afterPropertiesSet();

		sut.setLookupKey("lookup-key");

		assertThat(sut.getSession()).isSameAs(defaultSession);

		lookup.addSessionFactory("lookup-key", new DefaultSessionFactory(routedSession));
		sut.afterPropertiesSet();

		assertThat(sut.getSession()).isSameAs(routedSession);
	}

	static class StubbedRoutingSessionFactory extends AbstractRoutingSessionFactory {

		String lookupKey;

		void setLookupKey(String lookupKey) {
			this.lookupKey = lookupKey;
		}

		@Override
		protected Object determineCurrentLookupKey() {
			return lookupKey;
		}
	}
}
