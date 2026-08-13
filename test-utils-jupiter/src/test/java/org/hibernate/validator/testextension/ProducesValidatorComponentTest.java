/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import jakarta.validation.ClockProvider;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

@ValidatorTest
class ProducesValidatorComponentTest {

	private static final Instant FIXED_INSTANT = Instant.parse( "2020-01-01T00:00:00Z" );

	@ProducesValidatorComponent
	ClockProvider fixedClockProvider() {
		return () -> Clock.fixed( FIXED_INSTANT, ZoneId.of( "UTC" ) );
	}

	@Test
	void producedComponentConfiguresFactory(Validator validator) {
		assertNotNull( validator, "Validator should be created with produced components" );
	}

	@Test
	void producedComponentIsInjectableAsParameter(ClockProvider clockProvider) {
		assertNotNull( clockProvider, "Produced ClockProvider should be injectable" );
		Clock clock = clockProvider.getClock();
		assertEquals( FIXED_INSTANT, clock.instant(),
				"Injected ClockProvider should return the fixed clock" );
	}
}
