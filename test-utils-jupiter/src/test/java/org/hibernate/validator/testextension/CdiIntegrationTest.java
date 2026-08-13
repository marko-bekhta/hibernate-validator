/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.Produces;

import org.junit.jupiter.api.Test;

@ValidatorTest
class CdiIntegrationTest {

	@Produces
	GreetingService greetingService() {
		return name -> "Hello " + name;
	}

	@Test
	void producedBeanIsInjectable(GreetingService service) {
		assertNotNull( service, "CDI-produced GreetingService should be injectable as a test parameter" );
		assertEquals( "Hello World", service.greet( "World" ) );
	}

	/**
	 * Simple service interface for CDI injection testing.
	 */
	interface GreetingService {
		String greet(String name);
	}
}
