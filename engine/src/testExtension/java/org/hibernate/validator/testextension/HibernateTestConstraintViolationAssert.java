/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.util.Map;
import java.util.Objects;

import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ assertion class for {@link HibernateTestConstraintViolation}.
 */
public class HibernateTestConstraintViolationAssert
		extends AbstractAssert<HibernateTestConstraintViolationAssert, HibernateTestConstraintViolation> {

	HibernateTestConstraintViolationAssert(HibernateTestConstraintViolation actual) {
		super( actual, HibernateTestConstraintViolationAssert.class );
	}

	public static HibernateTestConstraintViolationAssert assertThat(HibernateTestConstraintViolation violation) {
		return new HibernateTestConstraintViolationAssert( violation );
	}

	public HibernateTestConstraintViolationAssert hasMessageTemplate(String expected) {
		isNotNull();
		if ( !actual.getMessageTemplate().equals( expected ) ) {
			failWithMessage( "Expected message template <%s> but was <%s>",
					expected, actual.getMessageTemplate() );
		}
		return this;
	}

	public HibernateTestConstraintViolationAssert hasPropertyPath(String expected) {
		isNotNull();
		if ( !actual.getPropertyPath().equals( expected ) ) {
			failWithMessage( "Expected property path <%s> but was <%s>",
					expected, actual.getPropertyPath() );
		}
		return this;
	}

	public HibernateTestConstraintViolationAssert hasMessageParameter(String name, Object value) {
		isNotNull();
		Map<String, Object> params = actual.getMessageParameters();
		if ( !params.containsKey( name ) ) {
			failWithMessage(
					"Expected message parameter <%s> to be present but it was not. Existing parameters: %s",
					name, params.keySet() );
		}
		Object actualValue = params.get( name );
		if ( !Objects.equals( actualValue, value ) ) {
			failWithMessage( "Expected message parameter <%s> to have value <%s> but was <%s>",
					name, value, actualValue );
		}
		return this;
	}

	public HibernateTestConstraintViolationAssert hasExpressionVariable(String name, Object value) {
		isNotNull();
		Map<String, Object> vars = actual.getExpressionVariables();
		if ( !vars.containsKey( name ) ) {
			failWithMessage(
					"Expected expression variable <%s> to be present but it was not. Existing variables: %s",
					name, vars.keySet() );
		}
		Object actualValue = vars.get( name );
		if ( !Objects.equals( actualValue, value ) ) {
			failWithMessage( "Expected expression variable <%s> to have value <%s> but was <%s>",
					name, value, actualValue );
		}
		return this;
	}
}
