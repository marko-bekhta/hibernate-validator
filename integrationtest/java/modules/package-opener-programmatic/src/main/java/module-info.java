/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
module org.hibernate.validator.integrationtest.java.module.packageopener.programmatic {

	requires jakarta.validation;
	requires org.hibernate.validator;
	requires org.glassfish.expressly;

	opens org.hibernate.validator.integrationtest.java.module.packageopener.programmatic.model to jakarta.validation;

}
