/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.testutil.Annotations;

import org.junit.jupiter.api.Test;

/**
 * Tests class-level {@link ConstraintSource} with regular {@code @Test} methods.
 */
@ValidatorTest
@ConstraintSource("sizeConstraint")
class ConstraintSourceTest {

	static Size sizeConstraint() {
		return Annotations.builder( Size.class )
				.attribute( "min", 2 )
				.attribute( "max", 5 )
				.build();
	}

	@Test
	void validatorIsInjected(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		assertNotNull( validator, "ConstraintValidator should be injected" );
		assertNotNull( context, "ConstraintValidatorContext should be injected" );
	}

	@Test
	void nullIsValid(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		assertTrue( validator.isValid( null, context ), "null should be valid for @Size" );
	}

	@Test
	void validStringAccepted(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		assertTrue( validator.isValid( "abc", context ), "'abc' should be valid for @Size(min=2, max=5)" );
	}

	@Test
	void tooShortStringRejected(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		assertFalse( validator.isValid( "a", context ), "'a' should be invalid for @Size(min=2, max=5)" );
	}

	@Test
	void tooLongStringRejected(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		assertFalse( validator.isValid( "abcdef", context ),
				"'abcdef' should be invalid for @Size(min=2, max=5)" );
	}
}
