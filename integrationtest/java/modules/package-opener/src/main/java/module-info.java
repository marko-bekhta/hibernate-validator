/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
module org.hibernate.validator.integrationtest.java.module.packageopener {

	requires jakarta.validation;
	requires org.hibernate.validator;
	requires org.glassfish.expressly;

	// Portable opens: opens to jakarta.validation, NOT to org.hibernate.validator.
	// The PackageOpener mechanism forwards this access to the provider module at runtime.
	opens org.hibernate.validator.integrationtest.java.module.packageopener.model to jakarta.validation;

}
