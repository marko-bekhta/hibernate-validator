/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;

@ValidatorTest
class ValidatorProducerTest implements ValidatorProducer {

	@Override
	public ValidatorFactory produceValidatorFactory() {
		return Validation.buildDefaultValidatorFactory();
	}

	@Test
	void validatorFromProducerIsInjected(Validator validator) {
		assertNotNull( validator, "Validator should come from the producer" );
	}

	@Test
	void validatorFromProducerWorks(Validator validator) {
		Set<ConstraintViolation<TestBean>> violations = validator.validate( new TestBean() );
		assertTrue( violations.size() > 0, "Validation should find violations on null field" );
	}

	@Test
	void factoryFromProducerIsInjected(ValidatorFactory factory) {
		assertNotNull( factory, "ValidatorFactory should come from the producer" );
	}

	static class TestBean {
		@NotNull
		String name;
	}
}
