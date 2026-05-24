/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.java.module.packageopener.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PrivateFieldBean {

	@NotNull
	private String name;

	@Positive
	private int count = -1;

	public PrivateFieldBean() {
	}
}
