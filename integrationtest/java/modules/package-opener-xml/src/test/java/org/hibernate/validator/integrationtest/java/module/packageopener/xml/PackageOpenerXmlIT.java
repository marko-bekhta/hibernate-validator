/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.java.module.packageopener.xml;

import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertThat;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.violationOf;

import java.io.InputStream;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.integrationtest.java.module.packageopener.xml.model.XmlConstrainedBean;

import org.testng.annotations.Test;

public class PackageOpenerXmlIT {

	@Test
	public void packageOpenerAllowsXmlConstraintMapping() {
		InputStream mappingStream = PackageOpenerXmlIT.class.getResourceAsStream( "constraint-mapping.xml" );

		Validator validator = Validation.byProvider( HibernateValidator.class )
				.configure()
				.addMapping( mappingStream )
				.buildValidatorFactory()
				.getValidator();

		assertThat( validator.validate( new XmlConstrainedBean() ) ).containsOnlyViolations(
				violationOf( NotNull.class ),
				violationOf( Positive.class )
		);
	}

	@Test
	public void packageOpenerAllowsXmlConstraintMappingWithDefaultProvider() {
		InputStream mappingStream = PackageOpenerXmlIT.class.getResourceAsStream( "constraint-mapping.xml" );

		Validator validator = Validation.byDefaultProvider()
				.configure()
				.addMapping( mappingStream )
				.buildValidatorFactory()
				.getValidator();

		assertThat( validator.validate( new XmlConstrainedBean() ) ).containsOnlyViolations(
				violationOf( NotNull.class ),
				violationOf( Positive.class )
		);
	}
}
