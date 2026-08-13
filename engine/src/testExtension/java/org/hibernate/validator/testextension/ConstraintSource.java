/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Specifies a source method that provides constraint annotation instances for test methods.
 * The annotation instances are resolved into initialized {@link jakarta.validation.ConstraintValidator}
 * instances that can be injected into test method parameters.
 * <p>
 * Can be used at class level (default for all {@code @Test} methods) or at method level (override).
 * <p>
 * Three modes of operation:
 * <ul>
 *   <li><b>Single config</b> (with {@code @Test}): source method returns a single annotation instance.
 *       The {@link ConstraintValidatorParameterResolver} resolves the validator parameter.</li>
 *   <li><b>Parameterized</b> (with {@code @ParameterizedTest}): source method returns a
 *       {@code Stream}, {@code Collection}, or array of annotation instances.</li>
 *   <li><b>Mixed arguments</b> (with {@code @ParameterizedTest}): source method returns a
 *       {@code Stream}, {@code Collection}, or array of {@link org.junit.jupiter.params.provider.Arguments}.
 *       The annotation at position {@link #constraintAt()} is resolved into a validator.</li>
 * </ul>
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(ConstraintSourceProvider.class)
public @interface ConstraintSource {

	/**
	 * The name of the source method that provides constraint annotation instances.
	 */
	String value();

	/**
	 * The position in the {@link org.junit.jupiter.params.provider.Arguments} tuple where the
	 * constraint annotation is located. Only used in mixed arguments mode.
	 * Defaults to 0.
	 */
	int constraintAt() default 0;
}
