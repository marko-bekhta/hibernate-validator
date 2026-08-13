/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import jakarta.validation.ConstraintValidator;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * Resolves {@link ConstraintValidator} parameters for regular {@code @Test} methods
 * that have a {@link ConstraintSource} annotation (on the method or on the class).
 * <p>
 * For {@code @ParameterizedTest} methods, the validator is provided as an argument
 * by {@link ConstraintSourceProvider} and this resolver is not needed.
 */
public class ConstraintValidatorParameterResolver implements ParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> type = parameterContext.getParameter().getType();
		if ( !ConstraintValidator.class.equals( type ) ) {
			return false;
		}
		// For @ParameterizedTest methods, the ConstraintSourceProvider handles the validator
		// via ArgumentsProvider — the ParameterizedTestParameterResolver maps the arguments.
		// This resolver only handles regular @Test methods.
		Method method = parameterContext.getDeclaringExecutable() instanceof Method
				? (Method) parameterContext.getDeclaringExecutable()
				: null;
		if ( method != null && method.isAnnotationPresent( ParameterizedTest.class ) ) {
			return false;
		}
		return ConstraintSourceSupport.findConstraintSource( extensionContext ) != null;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		ConstraintSource source = ConstraintSourceSupport.findConstraintSource( extensionContext );
		if ( source == null ) {
			throw new IllegalStateException(
					"@ConstraintSource not found on test method or class"
			);
		}

		Class<?> testClass = extensionContext.getRequiredTestClass();

		// Find and invoke the source method
		Method sourceMethod = ConstraintSourceSupport.findSourceMethod( testClass, source.value() );
		Object result = ConstraintSourceSupport.invokeSourceMethod( sourceMethod, extensionContext );

		if ( result == null ) {
			throw new IllegalStateException(
					"Source method '" + source.value() + "()' returned null"
			);
		}

		if ( !( result instanceof Annotation ) ) {
			throw new IllegalStateException(
					"Source method '" + source.value() + "()' must return an Annotation for @Test methods, "
							+ "but returned " + result.getClass().getName()
			);
		}

		// Extract the validated type from the method parameter
		Type validatedType = ConstraintSourceSupport.extractValidatedType(
				parameterContext.getParameter().getParameterizedType()
		);

		return ConstraintSourceSupport.createValidator( (Annotation) result, validatedType, extensionContext );
	}
}
