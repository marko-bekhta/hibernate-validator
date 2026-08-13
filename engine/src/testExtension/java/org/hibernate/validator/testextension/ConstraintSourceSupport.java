/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorDescriptor;
import org.hibernate.validator.internal.metadata.core.ConstraintHelper;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Shared logic for resolving constraint annotations into initialized
 * {@link ConstraintValidator} instances.
 */
final class ConstraintSourceSupport {

	private ConstraintSourceSupport() {
	}

	/**
	 * Finds a method by name on the test class or its superclasses.
	 * The method must take no parameters.
	 */
	static Method findSourceMethod(Class<?> testClass, String methodName) {
		Class<?> current = testClass;
		while ( current != null && current != Object.class ) {
			for ( Method method : current.getDeclaredMethods() ) {
				if ( method.getName().equals( methodName ) && method.getParameterCount() == 0 ) {
					return method;
				}
			}
			current = current.getSuperclass();
		}
		throw new IllegalArgumentException(
				"No source method '" + methodName + "()' found on " + testClass.getName()
						+ " or its superclasses"
		);
	}

	/**
	 * Invokes the source method, handling both static and instance methods.
	 */
	static Object invokeSourceMethod(Method method, ExtensionContext context) {
		try {
			method.setAccessible( true );
			Object instance = Modifier.isStatic( method.getModifiers() )
					? null
					: context.getTestInstance().orElse( null );
			return method.invoke( instance );
		}
		catch (Exception e) {
			throw new RuntimeException( "Failed to invoke source method: " + method, e );
		}
	}

	/**
	 * Extracts the validated type (T) from a {@code ConstraintValidator<A, T>} parameter type.
	 */
	static Type extractValidatedType(Type parameterType) {
		if ( parameterType instanceof ParameterizedType ) {
			ParameterizedType pType = (ParameterizedType) parameterType;
			if ( pType.getRawType() == ConstraintValidator.class ) {
				Type[] typeArgs = pType.getActualTypeArguments();
				if ( typeArgs.length == 2 ) {
					return typeArgs[1];
				}
			}
		}
		return Object.class;
	}

	/**
	 * Creates and initializes a {@link ConstraintValidator} for the given constraint annotation.
	 *
	 * @param annotation the constraint annotation instance
	 * @param validatedType the validated type (T in {@code ConstraintValidator<A, T>})
	 * @param context the extension context (for accessing the ValidatorFactory)
	 */
	@SuppressWarnings("unchecked")
	static <A extends Annotation> ConstraintValidator<A, ?> createValidator(
			A annotation, Type validatedType, ExtensionContext context) {
		Class<A> annotationType = (Class<A>) annotation.annotationType();

		// Get the ConstraintHelper from the ValidatorFactory
		ConstraintHelper constraintHelper = getConstraintHelper( context );

		// Find all validator descriptors for this annotation type
		List<ConstraintValidatorDescriptor<A>> descriptors = constraintHelper.getAllValidatorDescriptors( annotationType );

		if ( descriptors.isEmpty() ) {
			throw new IllegalStateException(
					"No ConstraintValidator found for @" + annotationType.getSimpleName()
			);
		}

		// Find the matching descriptor based on validated type
		ConstraintValidatorDescriptor<A> descriptor = findMatchingDescriptor( descriptors, validatedType, annotationType );

		// Get the ConstraintValidatorFactory from the store, or create a default one
		// (needed when provideArguments() is called before beforeEach() for @ParameterizedTest)
		ConstraintValidatorFactory cvFactory = getConstraintValidatorFactory( context );

		// Create and initialize the validator
		ConstraintValidator<A, ?> validator = descriptor.newInstance( cvFactory );
		validator.initialize( annotation );

		return validator;
	}

	/**
	 * Finds the {@link ConstraintSource} annotation on the method or class, with method-level
	 * taking precedence over class-level.
	 */
	static ConstraintSource findConstraintSource(ExtensionContext context) {
		// Check method-level first
		ConstraintSource methodLevel = context.getRequiredTestMethod().getAnnotation( ConstraintSource.class );
		if ( methodLevel != null ) {
			return methodLevel;
		}
		// Check class-level
		return context.getRequiredTestClass().getAnnotation( ConstraintSource.class );
	}

	private static ConstraintValidatorFactory getConstraintValidatorFactory(ExtensionContext context) {
		ValidatorFactory factory = (ValidatorFactory) ValidatorExtension.getFromStore(
				context, ValidatorExtension.FACTORY_KEY
		);
		if ( factory != null ) {
			return factory.getConstraintValidatorFactory();
		}
		// Fallback for @ParameterizedTest where provideArguments() is called before beforeEach().
		// Create a default ValidatorFactory to get the ConstraintValidatorFactory.
		ValidatorFactory defaultFactory = Validation.byDefaultProvider().configure().buildValidatorFactory();
		return defaultFactory.getConstraintValidatorFactory();
	}

	private static final String CONSTRAINT_HELPER_KEY = "constraintHelper";

	private static ConstraintHelper getConstraintHelper(ExtensionContext context) {
		// Cache the ConstraintHelper in the extension store to avoid recreating it
		Object cached = ValidatorExtension.getFromStore( context, CONSTRAINT_HELPER_KEY );
		if ( cached instanceof ConstraintHelper ) {
			return (ConstraintHelper) cached;
		}
		ConstraintHelper helper = ConstraintHelper.forAllBuiltinConstraints();
		context.getStore( ValidatorExtension.NAMESPACE ).put( CONSTRAINT_HELPER_KEY, helper );
		return helper;
	}

	private static <A extends Annotation> ConstraintValidatorDescriptor<A> findMatchingDescriptor(
			List<ConstraintValidatorDescriptor<A>> descriptors, Type validatedType, Class<A> annotationType) {
		if ( descriptors.size() == 1 ) {
			return descriptors.get( 0 );
		}

		Class<?> requestedClass = toRawClass( validatedType );

		// Find the most specific match
		ConstraintValidatorDescriptor<A> bestMatch = null;
		Class<?> bestMatchType = null;

		for ( ConstraintValidatorDescriptor<A> descriptor : descriptors ) {
			Class<?> descriptorType = toRawClass( descriptor.getValidatedType() );
			if ( descriptorType.isAssignableFrom( requestedClass ) ) {
				if ( bestMatch == null || bestMatchType.isAssignableFrom( descriptorType ) ) {
					bestMatch = descriptor;
					bestMatchType = descriptorType;
				}
			}
		}

		if ( bestMatch != null ) {
			return bestMatch;
		}

		throw new IllegalStateException(
				"No ConstraintValidator for @" + annotationType.getSimpleName()
						+ " found that handles type " + validatedType
						+ ". Available validators handle: "
						+ descriptors.stream()
								.map( d -> d.getValidatedType().toString() )
								.reduce( (a, b) -> a + ", " + b )
								.orElse( "none" )
		);
	}

	private static Class<?> toRawClass(Type type) {
		if ( type instanceof Class<?> ) {
			return (Class<?>) type;
		}
		if ( type instanceof ParameterizedType ) {
			return (Class<?>) ( (ParameterizedType) type ).getRawType();
		}
		return Object.class;
	}
}
