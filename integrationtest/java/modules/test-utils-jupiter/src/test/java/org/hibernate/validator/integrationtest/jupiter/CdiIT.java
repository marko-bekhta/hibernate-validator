/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.jupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.Produces;

import org.hibernate.validator.testextension.ValidatorTest;

import org.junit.jupiter.api.Test;

@ValidatorTest
class CdiIT {

	@Produces
	GreetingService greetingService() {
		return name -> "Hello " + name;
	}

	@Test
	void producedBeanIsInjectable(GreetingService service) {
		assertNotNull( service, "CDI-produced GreetingService should be injectable as a test parameter" );
		assertEquals( "Hello World", service.greet( "World" ) );
	}

	interface GreetingService {
		String greet(String name);
	}
}
