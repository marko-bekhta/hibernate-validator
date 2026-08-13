/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testutil;

import java.util.List;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;

/**
 * A {@link ConstraintValidatorContext} for unit-testing constraint validators in isolation,
 * without bootstrapping the full validation engine.
 * <p>
 * Records all interactions so they can be inspected after the validator runs.
 */
public interface TestConstraintValidatorContext extends ConstraintValidatorContext {

	boolean isDefaultViolationDisabled();

	List<TestConstraintViolation> getTestViolations();

	static TestConstraintValidatorContext standalone() {
		return standalone( "{default.message}" );
	}

	static TestConstraintValidatorContext standalone(String defaultTemplate) {
		return standalone( defaultTemplate, null );
	}

	static TestConstraintValidatorContext standalone(String defaultTemplate, ClockProvider clockProvider) {
		return new StandaloneTestConstraintValidatorContext( defaultTemplate, clockProvider );
	}
}
