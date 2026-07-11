/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.engine;

import jakarta.validation.Payload;

import org.hibernate.validator.Incubating;

/**
 * Payload markers to override the default {@link org.hibernate.validator.cfg.ArrayConstraintBehavior} on a
 * per-constraint basis for array-typed fields, parameters, and return values.
 * <p>
 * Usage follows the same pattern as {@link jakarta.validation.valueextraction.Unwrapping}:
 * <pre>
 * // Force element validation regardless of global config:
 * &#64;Email(payload = ArrayValidationTarget.Element.class)
 * private String[] emails;
 *
 * // Force array validation regardless of global config:
 * &#64;Size(min = 2, payload = ArrayValidationTarget.Array.class)
 * private String[] names;
 * </pre>
 * These payloads may only be used on array-typed declarations. Using them on non-array types will result
 * in a {@code ValidationException}.
 *
 * @since 9.2
 */
@Incubating
public interface ArrayValidationTarget {

	/**
	 * Apply the constraint to each element of the array, overriding the global
	 * {@link org.hibernate.validator.cfg.ArrayConstraintBehavior}.
	 *
	 * @since 9.2
	 */
	interface Element extends Payload {
	}

	/**
	 * Apply the constraint to the array itself, overriding the global
	 * {@link org.hibernate.validator.cfg.ArrayConstraintBehavior}.
	 *
	 * @since 9.2
	 */
	interface Array extends Payload {
	}
}
