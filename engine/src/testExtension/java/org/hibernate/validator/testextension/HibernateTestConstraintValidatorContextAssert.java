/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.validator.testutil.TestConstraintViolation;

import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ assertion class for {@link HibernateTestConstraintValidatorContext}.
 */
public class HibernateTestConstraintValidatorContextAssert
		extends AbstractAssert<HibernateTestConstraintValidatorContextAssert, HibernateTestConstraintValidatorContext> {

	HibernateTestConstraintValidatorContextAssert(HibernateTestConstraintValidatorContext actual) {
		super( actual, HibernateTestConstraintValidatorContextAssert.class );
	}

	public static HibernateTestConstraintValidatorContextAssert assertThat(
			HibernateTestConstraintValidatorContext context) {
		return new HibernateTestConstraintValidatorContextAssert( context );
	}

	public HibernateTestConstraintValidatorContextAssert hasMessageParameter(String name, Object value) {
		isNotNull();
		Map<String, Object> params = actual.getMessageParameters();
		if ( !params.containsKey( name ) ) {
			failWithMessage( "Expected message parameter <%s> to be present but it was not. Existing parameters: %s",
					name, params.keySet() );
		}
		Object actualValue = params.get( name );
		if ( !Objects.equals( actualValue, value ) ) {
			failWithMessage( "Expected message parameter <%s> to have value <%s> but was <%s>",
					name, value, actualValue );
		}
		return this;
	}

	public HibernateTestConstraintValidatorContextAssert hasExpressionVariable(String name, Object value) {
		isNotNull();
		Map<String, Object> vars = actual.getExpressionVariables();
		if ( !vars.containsKey( name ) ) {
			failWithMessage( "Expected expression variable <%s> to be present but it was not. Existing variables: %s",
					name, vars.keySet() );
		}
		Object actualValue = vars.get( name );
		if ( !Objects.equals( actualValue, value ) ) {
			failWithMessage( "Expected expression variable <%s> to have value <%s> but was <%s>",
					name, value, actualValue );
		}
		return this;
	}

	public HibernateTestConstraintValidatorContextAssert hasDynamicPayload(Object payload) {
		isNotNull();
		Object actualPayload = actual.getDynamicPayload();
		if ( !Objects.equals( actualPayload, payload ) ) {
			failWithMessage( "Expected dynamic payload <%s> but was <%s>",
					payload, actualPayload );
		}
		return this;
	}

	public HibernateTestConstraintValidatorContextAssert hasDefaultViolationDisabled() {
		isNotNull();
		if ( !actual.isDefaultViolationDisabled() ) {
			failWithMessage( "Expected default violation to be disabled but it was enabled" );
		}
		return this;
	}

	public HibernateTestConstraintValidatorContextAssert hasDefaultViolationEnabled() {
		isNotNull();
		if ( actual.isDefaultViolationDisabled() ) {
			failWithMessage( "Expected default violation to be enabled but it was disabled" );
		}
		return this;
	}

	public HibernateTestConstraintValidatorContextAssert hasViolations(int count) {
		isNotNull();
		List<TestConstraintViolation> violations = actual.getTestViolations();
		if ( violations.size() != count ) {
			failWithMessage( "Expected <%d> violations but found <%d>",
					count, violations.size() );
		}
		return this;
	}

	public HibernateTestConstraintValidatorContextAssert hasNoViolations() {
		return hasViolations( 0 );
	}

	public HibernateTestConstraintValidatorContextAssert hibernateViolation(int index,
			Consumer<HibernateTestConstraintViolationAssert> assertions) {
		isNotNull();
		List<TestConstraintViolation> violations = actual.getTestViolations();
		if ( index < 0 || index >= violations.size() ) {
			failWithMessage( "Violation index <%d> is out of bounds (size: <%d>)",
					index, violations.size() );
		}
		TestConstraintViolation violation = violations.get( index );
		if ( !( violation instanceof HibernateTestConstraintViolation ) ) {
			failWithMessage( "Violation at index <%d> is not a HibernateTestConstraintViolation", index );
		}
		assertions.accept( new HibernateTestConstraintViolationAssert( (HibernateTestConstraintViolation) violation ) );
		return this;
	}
}
