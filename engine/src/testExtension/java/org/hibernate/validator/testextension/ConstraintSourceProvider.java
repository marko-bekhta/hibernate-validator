/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import jakarta.validation.ConstraintValidator;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import org.junit.jupiter.params.support.AnnotationConsumer;

/**
 * {@link ArgumentsProvider} that resolves constraint annotation instances into initialized
 * {@link ConstraintValidator} instances for use with {@code @ParameterizedTest}.
 * <p>
 * The source method (named by {@link ConstraintSource#value()}) can return:
 * <ul>
 *   <li>A single constraint annotation</li>
 *   <li>A {@link Stream}, {@link Collection}, or array of constraint annotations</li>
 *   <li>A {@link Stream}, {@link Collection}, or array of {@link Arguments} (mixed mode)</li>
 * </ul>
 *
 * @see ConstraintSource
 */
public class ConstraintSourceProvider implements ArgumentsProvider, AnnotationConsumer<ConstraintSource> {

	private ConstraintSource constraintSource;

	@Override
	public void accept(ConstraintSource constraintSource) {
		this.constraintSource = constraintSource;
	}

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
		Class<?> testClass = context.getRequiredTestClass();
		Method testMethod = context.getRequiredTestMethod();

		// Find and invoke the source method
		Method sourceMethod = ConstraintSourceSupport.findSourceMethod( testClass, constraintSource.value() );
		Object result = ConstraintSourceSupport.invokeSourceMethod( sourceMethod, context );

		if ( result == null ) {
			throw new IllegalStateException(
					"Source method '" + constraintSource.value() + "()' returned null"
			);
		}

		// Determine the validated type from the test method parameter
		int constraintAt = constraintSource.constraintAt();
		Type validatedType = extractValidatedTypeFromMethod( testMethod, constraintAt );

		// Convert the result to a stream of Arguments
		Stream<?> stream = toStream( result );

		return stream.map( item -> resolveItem( item, validatedType, constraintAt, context ) );
	}

	private Arguments resolveItem(Object item, Type validatedType, int constraintAt, ExtensionContext context) {
		if ( item instanceof Arguments ) {
			// Mixed arguments mode: replace annotation at constraintAt with validator
			Object[] args = ( (Arguments) item ).get();
			if ( constraintAt < 0 || constraintAt >= args.length ) {
				throw new IllegalArgumentException(
						"constraintAt=" + constraintAt + " is out of bounds for Arguments of length " + args.length
				);
			}
			Object[] resolved = args.clone();
			Annotation annotation = (Annotation) resolved[constraintAt];
			resolved[constraintAt] = ConstraintSourceSupport.createValidator( annotation, validatedType, context );
			return Arguments.of( resolved );
		}
		else if ( item instanceof Annotation ) {
			// Single annotation mode: create validator and wrap in Arguments
			Annotation annotation = (Annotation) item;
			ConstraintValidator<?, ?> validator = ConstraintSourceSupport.createValidator(
					annotation, validatedType, context
			);
			return Arguments.of( validator );
		}
		else {
			throw new IllegalStateException(
					"Source method returned an unsupported element type: " + item.getClass().getName()
							+ ". Expected an Annotation or Arguments instance."
			);
		}
	}

	private Type extractValidatedTypeFromMethod(Method testMethod, int constraintAt) {
		Type[] genericTypes = testMethod.getGenericParameterTypes();
		if ( constraintAt < genericTypes.length ) {
			return ConstraintSourceSupport.extractValidatedType( genericTypes[constraintAt] );
		}
		return Object.class;
	}

	@SuppressWarnings("unchecked")
	private Stream<?> toStream(Object result) {
		if ( result instanceof Stream ) {
			return (Stream<?>) result;
		}
		if ( result instanceof Collection ) {
			return ( (Collection<?>) result ).stream();
		}
		if ( result instanceof Object[] ) {
			return Arrays.stream( (Object[]) result );
		}
		// Single element: wrap in a single-element stream
		return Stream.of( result );
	}
}
