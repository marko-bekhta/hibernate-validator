/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testutil;

/**
 * A recorded constraint violation captured during constraint validator testing.
 */
public interface TestConstraintViolation {

	String getMessageTemplate();

	String getPropertyPath();
}
