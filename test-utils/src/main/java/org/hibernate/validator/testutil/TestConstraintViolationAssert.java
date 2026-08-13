/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testutil;

import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ assertion class for {@link TestConstraintViolation}.
 */
public class TestConstraintViolationAssert
		extends AbstractAssert<TestConstraintViolationAssert, TestConstraintViolation> {

	TestConstraintViolationAssert(TestConstraintViolation actual) {
		super( actual, TestConstraintViolationAssert.class );
	}

	public static TestConstraintViolationAssert assertThat(TestConstraintViolation violation) {
		return new TestConstraintViolationAssert( violation );
	}

	public TestConstraintViolationAssert hasMessageTemplate(String expected) {
		isNotNull();
		if ( !actual.getMessageTemplate().equals( expected ) ) {
			failWithMessage( "Expected message template <%s> but was <%s>",
					expected, actual.getMessageTemplate() );
		}
		return this;
	}

	public TestConstraintViolationAssert hasPropertyPath(String expected) {
		isNotNull();
		if ( !actual.getPropertyPath().equals( expected ) ) {
			failWithMessage( "Expected property path <%s> but was <%s>",
					expected, actual.getPropertyPath() );
		}
		return this;
	}
}
