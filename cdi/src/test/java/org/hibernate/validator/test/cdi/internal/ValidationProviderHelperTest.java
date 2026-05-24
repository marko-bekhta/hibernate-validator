/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.test.cdi.internal;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ValidationProviderResolver;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.spi.BootstrapState;

import org.hibernate.validator.HibernateValidatorFactory;
import org.hibernate.validator.cdi.internal.ValidationProviderHelper;
import org.hibernate.validator.internal.engine.ConfigurationImpl;
import org.hibernate.validator.test.cdi.internal.injection.MyValidationProvider;

import org.testng.annotations.Test;

public class ValidationProviderHelperTest {

	@Test
	public void testExpectedCdiTypesFactory() {
		assertThat( ValidationProviderHelper.forHibernateValidator().determineValidatorFactoryCdiTypes() )
				.containsOnly(
						HibernateValidatorFactory.class,
						ValidatorFactory.class,
						AutoCloseable.class,
						Object.class
				);

		assertThat( ValidationProviderHelper.forDefaultProvider( new MyValidationProvider.MyValidatorFactory( new ConfigurationImpl( new MyValidationProvider(), boostrapState() ) ) )
				.determineValidatorFactoryCdiTypes() )
				.containsOnly(
						MyValidationProvider.MyValidatorFactory.class,
						ValidatorFactory.class,
						AutoCloseable.class,
						Object.class
				);

	}

	@Test
	public void testExpectedCdiTypesValidator() {
		assertThat( ValidationProviderHelper.forHibernateValidator().determineValidatorCdiTypes() )
				.containsOnly(
						Validator.class,
						ExecutableValidator.class,
						Object.class
				);

		assertThat( ValidationProviderHelper.forDefaultProvider( new MyValidationProvider.MyValidatorFactory( new ConfigurationImpl( new MyValidationProvider(), boostrapState() ) ) )
				.determineValidatorCdiTypes() )
				.containsOnly(
						MyValidationProvider.MyValidator.class,
						Validator.class,
						Object.class
				);

	}

	private BootstrapState boostrapState() {
		return new BootstrapState() {

			@Override
			public ValidationProviderResolver getValidationProviderResolver() {
				return null;
			}

			@Override
			public ValidationProviderResolver getDefaultValidationProviderResolver() {
				return null;
			}
		};
	}
}
