/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.jupiter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.testextension.ConstraintSource;
import org.hibernate.validator.testextension.ValidatorTest;
import org.hibernate.validator.testutil.Annotations;

import org.junit.jupiter.api.Test;

/**
 * Tests class-level {@link ConstraintSource} with regular {@code @Test} methods.
 */
@ValidatorTest
@ConstraintSource("sizeConstraint")
class ConstraintSourceIT {

	static Size sizeConstraint() {
		return Annotations.builder( Size.class )
				.attribute( "min", 2 )
				.attribute( "max", 5 )
				.build();
	}

	@Test
	void validatorIsInitialized(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		assertNotNull( validator, "ConstraintValidator should be injected" );
		assertTrue( validator.isValid( "abc", context ),
				"'abc' should be valid for @Size(min=2, max=5)" );
		assertFalse( validator.isValid( "a", context ),
				"'a' should be invalid for @Size(min=2, max=5)" );
	}
}
