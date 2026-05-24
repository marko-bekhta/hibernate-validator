/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
module org.hibernate.validator.integrationtest.java.module.cdi {

	requires jakarta.cdi;
	requires jakarta.validation;
	requires org.hibernate.validator;
	requires org.hibernate.validator.cdi;


	// HV needs reflection access to constrained beans via PackageOpener
	opens org.hibernate.validator.integrationtest.java.module.cdi.model to jakarta.validation;
	opens org.hibernate.validator.integrationtest.java.module.cdi.constraint to jakarta.validation;

	// we let all know that there's a `ConstraintValidator` "service" to be "loaded"
	provides jakarta.validation.ConstraintValidator with org.hibernate.validator.integrationtest.java.module.cdi.constraint.CarServiceConstraint.Validator;

}
