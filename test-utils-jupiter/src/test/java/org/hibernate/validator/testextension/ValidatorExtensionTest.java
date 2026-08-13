/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.testutil.TestConstraintValidatorContext;

import org.junit.jupiter.api.Test;

@ValidatorTest
class ValidatorExtensionTest {

	@Test
	void validatorIsInjected(Validator validator) {
		assertNotNull( validator, "Validator should be injected by the extension" );
	}

	@Test
	void validatorFactoryIsInjected(ValidatorFactory factory) {
		assertNotNull( factory, "ValidatorFactory should be injected by the extension" );
	}

	@Test
	void constraintValidatorContextIsInjected(ConstraintValidatorContext context) {
		assertNotNull( context, "ConstraintValidatorContext should be injected by the extension" );
	}

	@Test
	void testConstraintValidatorContextIsInjected(TestConstraintValidatorContext context) {
		assertNotNull( context, "TestConstraintValidatorContext should be injected by the extension" );
	}
}
