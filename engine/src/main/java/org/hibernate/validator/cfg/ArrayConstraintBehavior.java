/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.cfg;

import org.hibernate.validator.Incubating;

/**
 * Controls how constraint annotations on array-typed fields, parameters, and return values are interpreted.
 * <p>
 * When an annotation like {@code @Size} supports both {@code FIELD} and {@code TYPE_USE} element types and is placed
 * on an array (e.g. {@code @Size(min = 2) String[] names}), its target is ambiguous: it could apply to the array
 * itself (BV 1.1 behavior) or to each element (JLS type-use semantics).
 * <p>
 * This enum selects the default interpretation. Individual constraints can override the default using
 * {@link org.hibernate.validator.engine.ArrayValidationTarget.Element ArrayValidationTarget.Element} or
 * {@link org.hibernate.validator.engine.ArrayValidationTarget.Array ArrayValidationTarget.Array} payloads.
 *
 * @since 9.2
 */
@Incubating
public enum ArrayConstraintBehavior {

	/**
	 * Follow JLS §9.7.4 semantics (default).
	 * <p>
	 * Leftmost-position annotations ({@code @Email String[] emails}) target the element type and validate each
	 * array element. Between-brackets annotations ({@code String @Size(min = 2) [] emails}) target the array type
	 * and validate the array itself.
	 */
	JLS( "jls" ),

	/**
	 * Preserve Bean Validation 1.1 behavior.
	 * <p>
	 * All constraint annotations on an array-typed declaration validate the array itself, regardless of their
	 * position. This matches the pre-Java 8 interpretation.
	 */
	LEGACY( "legacy" );

	private final String externalRepresentation;

	ArrayConstraintBehavior(String externalRepresentation) {
		this.externalRepresentation = externalRepresentation;
	}

	public static ArrayConstraintBehavior of(String value) {
		for ( ArrayConstraintBehavior behavior : values() ) {
			if ( behavior.externalRepresentation.equals( value ) ) {
				return behavior;
			}
		}
		return ArrayConstraintBehavior.valueOf( value );
	}
}
