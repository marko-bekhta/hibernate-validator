/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import jakarta.validation.ValidatorFactory;

public interface ValidatorProducer {
	ValidatorFactory produceValidatorFactory();
}
