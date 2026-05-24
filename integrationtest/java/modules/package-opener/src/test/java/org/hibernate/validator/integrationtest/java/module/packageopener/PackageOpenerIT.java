/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.java.module.packageopener;

import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertThat;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.violationOf;

import java.lang.reflect.InaccessibleObjectException;
import java.util.Set;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidator;
import org.hibernate.validator.PredefinedScopeHibernateValidatorConfiguration;
import org.hibernate.validator.integrationtest.java.module.packageopener.closedmodel.ClosedBean;
import org.hibernate.validator.integrationtest.java.module.packageopener.model.CascadedBean;
import org.hibernate.validator.integrationtest.java.module.packageopener.model.PrivateFieldBean;

import org.testng.annotations.Test;

public class PackageOpenerIT {

	@Test
	public void packageOpenerAllowsAccessToPrivateFields() {
		Validator validator = Validation.byProvider( HibernateValidator.class )
				.configure()
				.buildValidatorFactory()
				.getValidator();

		assertThat( validator.validate( new PrivateFieldBean() ) ).containsOnlyViolations(
				violationOf( NotNull.class ),
				violationOf( Positive.class )
		);
	}

	@Test
	public void packageOpenerWorksWithDefaultProvider() {
		Validator validator = Validation.byDefaultProvider()
				.configure()
				.buildValidatorFactory()
				.getValidator();

		assertThat( validator.validate( new PrivateFieldBean() ) ).containsOnlyViolations(
				violationOf( NotNull.class ),
				violationOf( Positive.class )
		);
	}

	@Test(expectedExceptions = InaccessibleObjectException.class)
	public void validationFailsWhenPackageNotOpenedToJakartaValidation() {
		Validator validator = Validation.byProvider( HibernateValidator.class )
				.configure()
				.buildValidatorFactory()
				.getValidator();

		validator.validate( new ClosedBean() );
	}

	@Test
	public void packageOpenerAllowsCascadedValidation() {
		Validator validator = Validation.byProvider( HibernateValidator.class )
				.configure()
				.buildValidatorFactory()
				.getValidator();

		assertThat( validator.validate( new CascadedBean( null, new PrivateFieldBean() ) ) ).containsOnlyViolations(
				violationOf( NotNull.class ),
				violationOf( NotNull.class ),
				violationOf( Positive.class )
		);
	}

	@Test
	public void packageOpenerWorksWithPredefinedScope() {
		PredefinedScopeHibernateValidatorConfiguration configuration = Validation
				.byProvider( PredefinedScopeHibernateValidator.class )
				.configure()
				.initializeBeanMetaData( Set.of( PrivateFieldBean.class ) );

		Validator validator = configuration.buildValidatorFactory().getValidator();

		assertThat( validator.validate( new PrivateFieldBean() ) ).containsOnlyViolations(
				violationOf( NotNull.class ),
				violationOf( Positive.class )
		);
	}
}
