/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.util.Map;

import org.hibernate.validator.testutil.TestConstraintViolation;

/**
 * A {@link TestConstraintViolation} extended with Hibernate Validator-specific recorded data.
 */
public interface HibernateTestConstraintViolation extends TestConstraintViolation {

	Map<String, Object> getMessageParameters();

	Map<String, Object> getExpressionVariables();
}
