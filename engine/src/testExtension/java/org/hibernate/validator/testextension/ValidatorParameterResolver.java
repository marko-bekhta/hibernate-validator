/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class ValidatorParameterResolver implements ParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> type = parameterContext.getParameter().getType();
		return Validator.class.equals( type ) || ValidatorFactory.class.equals( type );
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> type = parameterContext.getParameter().getType();
		if ( Validator.class.equals( type ) ) {
			return ValidatorExtension.getFromStore( extensionContext, ValidatorExtension.VALIDATOR_KEY );
		}
		if ( ValidatorFactory.class.equals( type ) ) {
			return ValidatorExtension.getFromStore( extensionContext, ValidatorExtension.FACTORY_KEY );
		}
		return null;
	}
}
