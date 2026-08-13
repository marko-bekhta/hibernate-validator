/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testutil;

import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ assertion class for {@link TestConstraintValidatorContext}.
 */
public class TestConstraintValidatorContextAssert
		extends AbstractAssert<TestConstraintValidatorContextAssert, TestConstraintValidatorContext> {

	TestConstraintValidatorContextAssert(TestConstraintValidatorContext actual) {
		super( actual, TestConstraintValidatorContextAssert.class );
	}

	public static TestConstraintValidatorContextAssert assertThat(TestConstraintValidatorContext context) {
		return new TestConstraintValidatorContextAssert( context );
	}

	public TestConstraintValidatorContextAssert hasDefaultViolationDisabled() {
		isNotNull();
		if ( !actual.isDefaultViolationDisabled() ) {
			failWithMessage( "Expected default violation to be disabled but it was enabled" );
		}
		return this;
	}

	public TestConstraintValidatorContextAssert hasDefaultViolationEnabled() {
		isNotNull();
		if ( actual.isDefaultViolationDisabled() ) {
			failWithMessage( "Expected default violation to be enabled but it was disabled" );
		}
		return this;
	}

	public TestConstraintValidatorContextAssert hasViolations(int count) {
		isNotNull();
		List<TestConstraintViolation> violations = actual.getTestViolations();
		if ( violations.size() != count ) {
			failWithMessage( "Expected <%d> violations but found <%d>",
					count, violations.size() );
		}
		return this;
	}

	public TestConstraintValidatorContextAssert hasNoViolations() {
		return hasViolations( 0 );
	}

	public TestConstraintValidatorContextAssert violation(int index,
			Consumer<TestConstraintViolationAssert> assertions) {
		isNotNull();
		List<TestConstraintViolation> violations = actual.getTestViolations();
		if ( index < 0 || index >= violations.size() ) {
			failWithMessage( "Violation index <%d> is out of bounds (size: <%d>)",
					index, violations.size() );
		}
		assertions.accept( new TestConstraintViolationAssert( violations.get( index ) ) );
		return this;
	}
}
