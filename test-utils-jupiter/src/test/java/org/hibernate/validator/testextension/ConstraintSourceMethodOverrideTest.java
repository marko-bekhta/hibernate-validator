/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.testutil.Annotations;

import org.junit.jupiter.api.Test;

/**
 * Tests method-level {@link ConstraintSource} overriding a class-level one.
 */
@ValidatorTest
@ConstraintSource("lenientSize")
class ConstraintSourceMethodOverrideTest {

	static Size lenientSize() {
		return Annotations.builder( Size.class )
				.attribute( "min", 0 )
				.attribute( "max", 100 )
				.build();
	}

	static Size strictSize() {
		return Annotations.builder( Size.class )
				.attribute( "min", 3 )
				.attribute( "max", 3 )
				.build();
	}

	@Test
	void usesClassLevelSource(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		// lenientSize: min=0, max=100 — a long string should be valid
		assertTrue( validator.isValid( "this is a long string that should be valid", context ),
				"Class-level @ConstraintSource should use lenientSize" );
	}

	@Test
	@ConstraintSource("strictSize")
	void usesMethodLevelOverride(ConstraintValidator<Size, CharSequence> validator,
			ConstraintValidatorContext context) {
		// strictSize: min=3, max=3 — only 3-char strings are valid
		assertTrue( validator.isValid( "abc", context ),
				"'abc' should be valid for @Size(min=3, max=3)" );
		assertFalse( validator.isValid( "ab", context ),
				"'ab' should be invalid for @Size(min=3, max=3)" );
		assertFalse( validator.isValid( "abcd", context ),
				"'abcd' should be invalid for @Size(min=3, max=3)" );
	}
}
