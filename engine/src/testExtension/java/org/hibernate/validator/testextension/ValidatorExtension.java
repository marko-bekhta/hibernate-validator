/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ValidatorExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

	static final ExtensionContext.Namespace NAMESPACE =
			ExtensionContext.Namespace.create( ValidatorExtension.class );

	static final String FACTORY_KEY = "factory";
	static final String VALIDATOR_KEY = "validator";
	static final String COMPONENTS_KEY = "components";
	static final String INTEGRATION_KEY = "integration";
	private static final String CLOSEABLE_KEY = "closeable";

	@Override
	public void beforeAll(ExtensionContext context) {
		if ( isPerClass( context ) ) {
			setup( context );
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		if ( !isPerClass( context ) ) {
			setup( context );
		}
	}

	@Override
	public void afterAll(ExtensionContext context) {
		// Cleanup is handled automatically by the store's CloseableResource
	}

	@Override
	public void afterEach(ExtensionContext context) {
		// Cleanup is handled automatically by the store's CloseableResource
	}

	/**
	 * Looks up a value from the extension store, checking the current context first,
	 * then the parent context (needed when setup was done in beforeAll for PER_CLASS lifecycle).
	 */
	static Object getFromStore(ExtensionContext context, String key) {
		Object value = context.getStore( NAMESPACE ).get( key );
		if ( value != null ) {
			return value;
		}
		return context.getParent()
				.map( parent -> parent.getStore( NAMESPACE ).get( key ) )
				.orElse( null );
	}

	private boolean isPerClass(ExtensionContext context) {
		return context.getTestInstanceLifecycle()
				.map( l -> l == TestInstance.Lifecycle.PER_CLASS )
				.orElse( false );
	}

	private void setup(ExtensionContext context) {
		Object testInstance = context.getTestInstance().orElse( null );
		Class<?> testClass = context.getRequiredTestClass();

		// 1. Discover the active integration
		ValidatorTestIntegration integration = discoverIntegration( context );

		// 2. Scan for @ProducesValidatorComponent methods, call them, cache instances
		Map<Class<?>, Object> components = new LinkedHashMap<>();
		List<Method> producerMethods = findAnnotatedMethods( testClass, ProducesValidatorComponent.class );
		for ( Method method : producerMethods ) {
			try {
				method.setAccessible( true );
				Object instance = Modifier.isStatic( method.getModifiers() ) ? null : testInstance;
				Object value = method.invoke( instance );
				if ( value != null ) {
					components.put( method.getReturnType(), value );
				}
			}
			catch (Exception e) {
				throw new RuntimeException( "Failed to invoke @ProducesValidatorComponent method: " + method, e );
			}
		}

		// 3. Scan for @ConfigureValidator methods
		List<Method> configMethods = findAnnotatedMethods( testClass, ConfigureValidator.class );

		// 4. Delegate factory creation to the integration
		ValidatorFactory factory = integration.createValidatorFactory( context, testInstance, components, configMethods );
		Validator validator = factory.getValidator();

		// 5. Store in the extension context
		ExtensionContext.Store store = context.getStore( NAMESPACE );
		store.put( CLOSEABLE_KEY, (ExtensionContext.Store.CloseableResource) factory::close );
		store.put( FACTORY_KEY, factory );
		store.put( VALIDATOR_KEY, validator );
		store.put( COMPONENTS_KEY, components );
		store.put( INTEGRATION_KEY, integration );
	}

	private ValidatorTestIntegration discoverIntegration(ExtensionContext context) {
		ServiceLoader<ValidatorTestIntegration> loader = ServiceLoader.load( ValidatorTestIntegration.class );
		for ( ValidatorTestIntegration integration : loader ) {
			if ( integration.isActive( context ) ) {
				return integration;
			}
		}
		return new DefaultValidatorTestIntegration();
	}

	private List<Method> findAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotation) {
		List<Method> result = new ArrayList<>();
		Class<?> current = clazz;
		while ( current != null && current != Object.class ) {
			for ( Method method : current.getDeclaredMethods() ) {
				if ( method.isAnnotationPresent( annotation ) ) {
					result.add( method );
				}
			}
			current = current.getSuperclass();
		}
		return result;
	}
}
