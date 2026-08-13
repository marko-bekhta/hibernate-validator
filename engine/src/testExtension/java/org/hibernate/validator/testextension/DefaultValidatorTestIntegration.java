/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ClockProvider;
import jakarta.validation.Configuration;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.valueextraction.ValueExtractor;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Built-in {@link ValidatorTestIntegration} that provides standalone Jakarta Bean Validation
 * bootstrapping. This is the fallback used when no framework-specific integration is active.
 * <p>
 * When methods annotated with {@code jakarta.enterprise.inject.Produces} are detected on the
 * test class hierarchy and Weld SE is on the classpath, this integration automatically boots
 * a CDI container and uses a CDI-aware {@link ConstraintValidatorFactory}.
 */
public class DefaultValidatorTestIntegration implements ValidatorTestIntegration {

	private static final String CDI_PRODUCES_CLASS = "jakarta.enterprise.inject.Produces";
	private static final String WELD_CLASS = "org.jboss.weld.environment.se.Weld";

	private Map<Class<?>, Object> producedComponents;
	private Object cdiContainer;
	private Object cdiBeanManager;
	private Set<Class<?>> cdiProducedTypes;

	@Override
	public boolean isActive(ExtensionContext context) {
		return true;
	}

	@Override
	public ValidatorFactory createValidatorFactory(
			ExtensionContext context,
			Object testInstance,
			Map<Class<?>, Object> components,
			List<Method> configurerMethods) {
		this.producedComponents = components;

		if ( testInstance instanceof ValidatorProducer ) {
			return ( (ValidatorProducer) testInstance ).produceValidatorFactory();
		}

		// Scan for @jakarta.enterprise.inject.Produces methods
		Class<?> testClass = context.getRequiredTestClass();
		Set<Class<?>> producedTypes = scanForCdiProduces( testClass );

		Configuration<?> config = Validation.byDefaultProvider().configure();
		applyComponents( config, components );

		if ( !producedTypes.isEmpty() ) {
			bootCdiContainer( testClass, context );
			this.cdiProducedTypes = producedTypes;
			config.constraintValidatorFactory(
					new CdiConstraintValidatorFactory( getBeanManager() )
			);
		}

		configure( config, testInstance, configurerMethods );
		return config.buildValidatorFactory();
	}

	@Override
	public void applyComponents(Configuration<?> config, Map<Class<?>, Object> components) {
		for ( Object value : components.values() ) {
			applyComponent( config, value );
		}
	}

	@Override
	public void configure(Configuration<?> config, Object testInstance, List<Method> configurerMethods) {
		for ( Method method : configurerMethods ) {
			try {
				method.setAccessible( true );
				Object instance = Modifier.isStatic( method.getModifiers() ) ? null : testInstance;
				method.invoke( instance, config );
			}
			catch (Exception e) {
				throw new RuntimeException( "Failed to invoke @ConfigureValidator method: " + method, e );
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T resolveBean(Class<T> type) {
		// First check the directly produced components
		if ( producedComponents != null ) {
			for ( Object value : producedComponents.values() ) {
				if ( type.isInstance( value ) ) {
					return (T) value;
				}
			}
		}
		// Then check CDI container if available, but only for types produced by @Produces methods
		if ( cdiBeanManager != null && cdiProducedTypes != null ) {
			for ( Class<?> producedType : cdiProducedTypes ) {
				if ( type.isAssignableFrom( producedType ) ) {
					return resolveBeanFromCdi( type );
				}
			}
		}
		return null;
	}

	/**
	 * Returns the CDI container (Weld SE container) if one was booted, or {@code null}.
	 */
	Object getCdiContainer() {
		return cdiContainer;
	}

	/**
	 * Shuts down the CDI container if one is active.
	 */
	void shutdownCdi() {
		if ( cdiContainer != null ) {
			try {
				// WeldContainer implements AutoCloseable
				( (AutoCloseable) cdiContainer ).close();
			}
			catch (Exception e) {
				throw new RuntimeException( "Failed to shut down CDI container", e );
			}
			finally {
				cdiContainer = null;
				cdiBeanManager = null;
				cdiProducedTypes = null;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Set<Class<?>> scanForCdiProduces(Class<?> testClass) {
		Set<Class<?>> producedTypes = new LinkedHashSet<>();
		Class<? extends Annotation> producesAnnotation;
		try {
			producesAnnotation = (Class<? extends Annotation>) Class.forName( CDI_PRODUCES_CLASS );
		}
		catch (ClassNotFoundException e) {
			// CDI API not on classpath, no CDI produces possible
			return producedTypes;
		}

		Class<?> current = testClass;
		while ( current != null && current != Object.class ) {
			for ( Method method : current.getDeclaredMethods() ) {
				if ( method.isAnnotationPresent( producesAnnotation ) ) {
					producedTypes.add( method.getReturnType() );
				}
			}
			current = current.getSuperclass();
		}
		return producedTypes;
	}

	private void bootCdiContainer(Class<?> testClass, ExtensionContext context) {
		// Check that Weld SE is on the classpath
		try {
			Class.forName( WELD_CLASS );
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException(
					"Methods annotated with @jakarta.enterprise.inject.Produces were found on "
							+ testClass.getName()
							+ ", but Weld SE (org.jboss.weld.se:weld-se-core) is not on the classpath. "
							+ "Add weld-se-core as a dependency to enable CDI support."
			);
		}

		try {
			// Use reflection to avoid compile-time dependency on Weld SE
			// new Weld().disableDiscovery().addBeanClass(testClass).initialize()
			Class<?> weldClass = Class.forName( WELD_CLASS );
			Object weld = weldClass.getDeclaredConstructor().newInstance();

			// disableDiscovery()
			Method disableDiscovery = weldClass.getMethod( "disableDiscovery" );
			weld = disableDiscovery.invoke( weld );

			// addBeanClass(testClass)
			Method addBeanClass = weldClass.getMethod( "addBeanClass", Class.class );
			weld = addBeanClass.invoke( weld, testClass );

			// initialize()
			Method initialize = weldClass.getMethod( "initialize" );
			Object container = initialize.invoke( weld );

			this.cdiContainer = container;

			// Get the BeanManager from the container
			Method getBeanManager = container.getClass().getMethod( "getBeanManager" );
			this.cdiBeanManager = getBeanManager.invoke( container );

			// Store the container as a CloseableResource in the extension context for lifecycle management
			ExtensionContext.Store store = context.getStore( ValidatorExtension.NAMESPACE );
			store.put( "cdiContainer", (ExtensionContext.Store.CloseableResource) () -> shutdownCdi() );
		}
		catch (IllegalStateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException( "Failed to boot CDI container", e );
		}
	}

	private jakarta.enterprise.inject.spi.BeanManager getBeanManager() {
		return (jakarta.enterprise.inject.spi.BeanManager) cdiBeanManager;
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveBeanFromCdi(Class<T> type) {
		try {
			jakarta.enterprise.inject.spi.BeanManager bm = getBeanManager();
			Set<jakarta.enterprise.inject.spi.Bean<?>> beans = bm.getBeans( type );
			if ( beans.isEmpty() ) {
				return null;
			}
			jakarta.enterprise.inject.spi.Bean<?> bean = bm.resolve( beans );
			jakarta.enterprise.context.spi.CreationalContext<?> ctx = bm.createCreationalContext( bean );
			return (T) bm.getReference( bean, type, ctx );
		}
		catch (Exception e) {
			return null;
		}
	}

	private void applyComponent(Configuration<?> config, Object value) {
		if ( value instanceof MessageInterpolator ) {
			config.messageInterpolator( (MessageInterpolator) value );
		}
		else if ( value instanceof TraversableResolver ) {
			config.traversableResolver( (TraversableResolver) value );
		}
		else if ( value instanceof ConstraintValidatorFactory ) {
			config.constraintValidatorFactory( (ConstraintValidatorFactory) value );
		}
		else if ( value instanceof ClockProvider ) {
			config.clockProvider( (ClockProvider) value );
		}
		else if ( value instanceof ParameterNameProvider ) {
			config.parameterNameProvider( (ParameterNameProvider) value );
		}
		else if ( value instanceof ValueExtractor<?> ) {
			config.addValueExtractor( (ValueExtractor<?>) value );
		}
	}
}
