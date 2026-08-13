/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class ComponentParameterResolver implements ParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> type = parameterContext.getParameter().getType();
		Map<Class<?>, Object> components = getComponents( extensionContext );
		if ( components != null ) {
			for ( Object value : components.values() ) {
				if ( type.isInstance( value ) ) {
					return true;
				}
			}
		}
		// Fallback: try the integration's bean resolution
		ValidatorTestIntegration integration = getIntegration( extensionContext );
		if ( integration != null ) {
			try {
				Object resolved = integration.resolveBean( type );
				return resolved != null;
			}
			catch (UnsupportedOperationException e) {
				// Integration does not support bean resolution
			}
		}
		return false;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> type = parameterContext.getParameter().getType();
		Map<Class<?>, Object> components = getComponents( extensionContext );
		if ( components != null ) {
			for ( Object value : components.values() ) {
				if ( type.isInstance( value ) ) {
					return value;
				}
			}
		}
		// Fallback: try the integration's bean resolution
		ValidatorTestIntegration integration = getIntegration( extensionContext );
		if ( integration != null ) {
			try {
				return integration.resolveBean( type );
			}
			catch (UnsupportedOperationException e) {
				// Integration does not support bean resolution
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Map<Class<?>, Object> getComponents(ExtensionContext context) {
		return (Map<Class<?>, Object>) ValidatorExtension.getFromStore( context, ValidatorExtension.COMPONENTS_KEY );
	}

	private ValidatorTestIntegration getIntegration(ExtensionContext context) {
		return (ValidatorTestIntegration) ValidatorExtension.getFromStore( context, ValidatorExtension.INTEGRATION_KEY );
	}
}
