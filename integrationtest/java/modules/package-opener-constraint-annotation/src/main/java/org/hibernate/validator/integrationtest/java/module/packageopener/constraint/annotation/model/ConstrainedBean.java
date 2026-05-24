/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.java.module.packageopener.constraint.annotation.model;

import org.hibernate.validator.integrationtest.java.module.packageopener.constraint.annotation.constraint.CustomConstraint;

@CustomConstraint
public class ConstrainedBean {

	private String name;

	public ConstrainedBean() {
	}
}
