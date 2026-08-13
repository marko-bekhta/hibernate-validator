/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.testutil.Annotations;

import org.junit.jupiter.params.ParameterizedTest;

/**
 * Tests {@link ConstraintSource} with {@code @ParameterizedTest} and a stream of constraint annotations.
 */
@ValidatorTest
class ConstraintSourceParameterizedTest {

	static Stream<Size> sizeConfigs() {
		return Stream.of(
				Annotations.builder( Size.class )
						.attribute( "min", 1 )
						.attribute( "max", 3 )
						.build(),
				Annotations.builder( Size.class )
						.attribute( "min", 5 )
						.attribute( "max", 10 )
						.build()
		);
	}

	@ParameterizedTest
	@ConstraintSource("sizeConfigs")
	void validatorIsCreatedForEachConfig(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		assertNotNull( validator, "ConstraintValidator should be injected" );
		// Null is valid for all @Size configurations
		assertTrue( validator.isValid( null, context ), "null should be valid for @Size" );
	}

	@ParameterizedTest
	@ConstraintSource("sizeConfigs")
	void emptyStringBehaviorDependsOnConfig(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		// Empty string (length 0):
		// - For min=1, max=3: invalid (too short)
		// - For min=5, max=10: invalid (too short)
		// Both configs reject empty strings since min >= 1
		assertFalse( validator.isValid( "", context ),
				"Empty string should be invalid when min >= 1" );
	}
}
