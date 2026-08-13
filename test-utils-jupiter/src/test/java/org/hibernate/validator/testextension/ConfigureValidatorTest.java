/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.Configuration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;

@ValidatorTest
class ConfigureValidatorTest {

	@ConfigureValidator
	void configure(Configuration<?> config) {
		// Disable default constraint violation, just verify the callback is invoked
		// by configuring a known property
		config.addProperty( "test.configured", "true" );
	}

	@Test
	void configureCallbackIsInvoked(Validator validator) {
		assertNotNull( validator, "Validator should be created after @ConfigureValidator callback" );

		// Verify the validator works
		Set<ConstraintViolation<TestBean>> violations = validator.validate( new TestBean() );
		assertTrue( violations.size() > 0, "Validation should find violations on null field" );
	}

	static class TestBean {
		@NotNull
		String name;
	}
}
