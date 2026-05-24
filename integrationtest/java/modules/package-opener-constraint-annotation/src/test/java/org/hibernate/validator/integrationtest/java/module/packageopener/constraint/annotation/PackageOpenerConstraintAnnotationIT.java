/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.java.module.packageopener.constraint.annotation;

import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertThat;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.violationOf;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.integrationtest.java.module.packageopener.constraint.annotation.constraint.CustomConstraint;
import org.hibernate.validator.integrationtest.java.module.packageopener.constraint.annotation.model.ConstrainedBean;

import org.testng.annotations.Test;

public class PackageOpenerConstraintAnnotationIT {

	@Test
	public void packageOpenerAllowsAccessToCustomConstraintAnnotation() {
		Validator validator = Validation.byProvider( HibernateValidator.class )
				.configure()
				.buildValidatorFactory()
				.getValidator();

		assertThat( validator.validate( new ConstrainedBean() ) ).containsOnlyViolations(
				violationOf( CustomConstraint.class )
		);
	}

	@Test
	public void packageOpenerWorksWithDefaultProvider() {
		Validator validator = Validation.byDefaultProvider()
				.configure()
				.buildValidatorFactory()
				.getValidator();

		assertThat( validator.validate( new ConstrainedBean() ) ).containsOnlyViolations(
				violationOf( CustomConstraint.class )
		);
	}
}
