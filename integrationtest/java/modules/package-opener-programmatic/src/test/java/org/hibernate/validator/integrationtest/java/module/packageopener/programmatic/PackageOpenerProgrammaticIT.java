/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.java.module.packageopener.programmatic;

import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertThat;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.violationOf;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.hibernate.validator.cfg.defs.NotNullDef;
import org.hibernate.validator.cfg.defs.PositiveDef;
import org.hibernate.validator.integrationtest.java.module.packageopener.programmatic.model.ProgrammaticBean;

import org.testng.annotations.Test;

public class PackageOpenerProgrammaticIT {

	@Test
	public void packageOpenerAllowsProgrammaticConstraintMapping() {
		HibernateValidatorConfiguration configuration = Validation.byProvider( HibernateValidator.class )
				.configure();

		ConstraintMapping mapping = configuration.createConstraintMapping();
		mapping.type( ProgrammaticBean.class )
				.field( "name" )
				.constraint( new NotNullDef() )
				.field( "count" )
				.constraint( new PositiveDef() );

		configuration.addMapping( mapping );

		Validator validator = configuration.buildValidatorFactory().getValidator();

		assertThat( validator.validate( new ProgrammaticBean() ) ).containsOnlyViolations(
				violationOf( NotNull.class ),
				violationOf( Positive.class )
		);
	}
}
