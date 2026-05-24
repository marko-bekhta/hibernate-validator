/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
module org.hibernate.validator.integrationtest.java.module.packageopener.constraint.annotation {

	requires jakarta.validation;
	requires org.hibernate.validator;
	requires org.glassfish.expressly;

	opens org.hibernate.validator.integrationtest.java.module.packageopener.constraint.annotation.constraint to jakarta.validation;
	opens org.hibernate.validator.integrationtest.java.module.packageopener.constraint.annotation.model to jakarta.validation;

}
