/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;

/**
 * A {@link ConstraintValidatorFactory} that uses CDI's {@link BeanManager} to create
 * and inject constraint validator instances, enabling {@code @Inject} dependencies
 * in constraint validators during tests.
 */
class CdiConstraintValidatorFactory implements ConstraintValidatorFactory {

	private final BeanManager beanManager;

	CdiConstraintValidatorFactory(BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Override
	public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
		AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( key );
		InjectionTargetFactory<T> factory = beanManager.getInjectionTargetFactory( annotatedType );
		InjectionTarget<T> injectionTarget = factory.createInjectionTarget( null );
		CreationalContext<T> ctx = beanManager.createCreationalContext( null );
		T instance = injectionTarget.produce( ctx );
		injectionTarget.inject( instance, ctx );
		injectionTarget.postConstruct( instance );
		return instance;
	}

	@Override
	public void releaseInstance(ConstraintValidator<?, ?> instance) {
		// no-op for simplicity
	}
}
