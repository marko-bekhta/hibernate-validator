/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.jupiter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import jakarta.validation.ClockProvider;

import org.hibernate.validator.testextension.ProducesValidatorComponent;
import org.hibernate.validator.testextension.ValidatorTest;

import org.junit.jupiter.api.Test;

@ValidatorTest
class ProducesComponentIT {

	@ProducesValidatorComponent
	ClockProvider clockProvider() {
		return () -> Clock.fixed( Instant.parse( "2024-01-01T00:00:00Z" ), ZoneOffset.UTC );
	}

	@Test
	void componentIsInjected(ClockProvider clock) {
		assertNotNull( clock, "Produced ClockProvider should be injectable" );
	}
}
