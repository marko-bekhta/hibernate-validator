/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import jakarta.validation.Configuration;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * SPI for integrating the {@link ValidatorExtension} with external frameworks.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}. The first
 * integration whose {@link #isActive(ExtensionContext)} returns {@code true} is used.
 * If none is active, the built-in {@link DefaultValidatorTestIntegration} is used as a fallback.
 */
public interface ValidatorTestIntegration {

	/**
	 * Returns {@code true} if this integration should handle the given extension context.
	 */
	boolean isActive(ExtensionContext context);

	/**
	 * Creates a {@link ValidatorFactory} for the given test context.
	 *
	 * @param context the JUnit extension context
	 * @param testInstance the test class instance (may be {@code null} for static-only tests)
	 * @param components instances produced by {@link ProducesValidatorComponent} methods
	 * @param configurerMethods methods annotated with {@link ConfigureValidator}
	 * @return a configured {@link ValidatorFactory}
	 */
	default ValidatorFactory createValidatorFactory(
			ExtensionContext context,
			Object testInstance,
			Map<Class<?>, Object> components,
			List<Method> configurerMethods) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not support ValidatorFactory bootstrapping."
		);
	}

	/**
	 * Applies produced component instances to the given configuration by matching
	 * their types to the appropriate Jakarta Validation configuration methods.
	 *
	 * @param config the validator configuration
	 * @param components instances produced by {@link ProducesValidatorComponent} methods
	 */
	default void applyComponents(Configuration<?> config, Map<Class<?>, Object> components) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not support @ProducesValidatorComponent. "
						+ "Configure your ValidatorFactory through your framework instead."
		);
	}

	/**
	 * Invokes {@link ConfigureValidator}-annotated methods on the test instance,
	 * passing the given configuration.
	 *
	 * @param config the validator configuration
	 * @param testInstance the test class instance
	 * @param configurerMethods methods annotated with {@link ConfigureValidator}
	 */
	default void configure(Configuration<?> config, Object testInstance, List<Method> configurerMethods) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not support @ConfigureValidator."
		);
	}

	/**
	 * Resolves a bean of the given type. Used as a fallback by {@link ComponentParameterResolver}
	 * when the type is not found in the produced components map.
	 *
	 * @param type the bean type to resolve
	 * @param <T> the bean type
	 * @return the resolved bean instance, or {@code null} if not found
	 */
	default <T> T resolveBean(Class<T> type) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not support bean resolution."
		);
	}
}
