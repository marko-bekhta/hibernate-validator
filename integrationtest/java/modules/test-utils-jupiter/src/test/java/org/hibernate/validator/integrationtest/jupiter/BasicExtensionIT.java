/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.jupiter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.testextension.HibernateTestConstraintValidatorContext;
import org.hibernate.validator.testextension.ValidatorTest;

import org.junit.jupiter.api.Test;

@ValidatorTest
class BasicExtensionIT {

	@Test
	void validatorInjected(Validator validator) {
		assertNotNull( validator, "Validator should be injected by the extension" );
	}

	@Test
	void factoryInjected(ValidatorFactory factory) {
		assertNotNull( factory, "ValidatorFactory should be injected by the extension" );
	}

	@Test
	void contextInjected(ConstraintValidatorContext context) {
		assertNotNull( context, "ConstraintValidatorContext should be injected by the extension" );
	}

	@Test
	void hibernateContextInjected(HibernateTestConstraintValidatorContext context) {
		assertNotNull( context, "HibernateTestConstraintValidatorContext should be injected by the extension" );
	}
}
