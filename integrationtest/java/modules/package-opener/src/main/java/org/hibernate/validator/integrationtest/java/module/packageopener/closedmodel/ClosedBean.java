/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.java.module.packageopener.closedmodel;

import jakarta.validation.constraints.NotNull;

public class ClosedBean {

	@NotNull
	private String name;

	public ClosedBean() {
	}
}
