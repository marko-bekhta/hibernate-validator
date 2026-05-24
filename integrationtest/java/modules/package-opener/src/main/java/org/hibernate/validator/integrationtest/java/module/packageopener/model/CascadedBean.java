/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.java.module.packageopener.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class CascadedBean {

	@NotNull
	private String label;

	@Valid
	private PrivateFieldBean nested;

	public CascadedBean(String label, PrivateFieldBean nested) {
		this.label = label;
		this.nested = nested;
	}
}
