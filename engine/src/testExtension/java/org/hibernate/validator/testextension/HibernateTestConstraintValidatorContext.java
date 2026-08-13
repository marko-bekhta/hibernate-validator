/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;
import org.hibernate.validator.testutil.TestConstraintValidatorContext;
import org.hibernate.validator.testutil.TestConstraintViolation;

/**
 * An HV-aware recording {@link ConstraintValidatorContext} that implements both
 * {@link TestConstraintValidatorContext} and {@link HibernateConstraintValidatorContext}.
 * <p>
 * Records all interactions (including HV-specific ones such as message parameters,
 * expression variables and dynamic payload) so they can be inspected after the validator runs.
 */
public class HibernateTestConstraintValidatorContext
		implements TestConstraintValidatorContext, HibernateConstraintValidatorContext {

	private static final ClockProvider DEFAULT_CLOCK_PROVIDER = Clock::systemDefaultZone;
	private static final String DEFAULT_TEMPLATE = "{default.message}";

	private final String defaultTemplate;
	private final ClockProvider clockProvider;
	private final List<TestConstraintViolation> violations = new ArrayList<>();
	private boolean defaultViolationDisabled;

	private final Map<String, Object> messageParameters = new LinkedHashMap<>();
	private final Map<String, Object> expressionVariables = new LinkedHashMap<>();
	private Object dynamicPayload;
	private Object constraintValidatorPayload;

	public HibernateTestConstraintValidatorContext() {
		this( DEFAULT_TEMPLATE, null );
	}

	public HibernateTestConstraintValidatorContext(String defaultTemplate) {
		this( defaultTemplate, null );
	}

	public HibernateTestConstraintValidatorContext(String defaultTemplate, ClockProvider clockProvider) {
		this.defaultTemplate = defaultTemplate != null ? defaultTemplate : DEFAULT_TEMPLATE;
		this.clockProvider = clockProvider != null ? clockProvider : DEFAULT_CLOCK_PROVIDER;
	}

	// -- TestConstraintValidatorContext ------------------------------------------------

	@Override
	public boolean isDefaultViolationDisabled() {
		return defaultViolationDisabled;
	}

	@Override
	public List<TestConstraintViolation> getTestViolations() {
		return Collections.unmodifiableList( violations );
	}

	// -- ConstraintValidatorContext ---------------------------------------------------

	@Override
	public void disableDefaultConstraintViolation() {
		defaultViolationDisabled = true;
	}

	@Override
	public String getDefaultConstraintMessageTemplate() {
		return defaultTemplate;
	}

	@Override
	public ClockProvider getClockProvider() {
		return clockProvider;
	}

	@Override
	public HibernateConstraintViolationBuilder buildConstraintViolationWithTemplate(String messageTemplate) {
		return new RecordingHibernateConstraintViolationBuilder( messageTemplate );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if ( type.isInstance( this ) ) {
			return (T) this;
		}
		throw new ValidationException( "Cannot unwrap to " + type.getName() );
	}

	// -- HibernateConstraintValidatorContext ------------------------------------------

	@Override
	public HibernateConstraintValidatorContext addMessageParameter(String name, Object value) {
		if ( name == null ) {
			throw new IllegalArgumentException( "Message parameter name must not be null" );
		}
		messageParameters.put( name, value );
		return this;
	}

	@Override
	public HibernateConstraintValidatorContext addExpressionVariable(String name, Object value) {
		if ( name == null ) {
			throw new IllegalArgumentException( "Expression variable name must not be null" );
		}
		expressionVariables.put( name, value );
		return this;
	}

	@Override
	public HibernateConstraintValidatorContext withDynamicPayload(Object payload) {
		this.dynamicPayload = payload;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C> C getConstraintValidatorPayload(Class<C> type) {
		if ( constraintValidatorPayload != null && type.isInstance( constraintValidatorPayload ) ) {
			return (C) constraintValidatorPayload;
		}
		return null;
	}

	// -- HV-specific accessors for testing --------------------------------------------

	/**
	 * Returns the message parameters recorded at the context level.
	 */
	public Map<String, Object> getMessageParameters() {
		return Collections.unmodifiableMap( messageParameters );
	}

	/**
	 * Returns the expression variables recorded at the context level.
	 */
	public Map<String, Object> getExpressionVariables() {
		return Collections.unmodifiableMap( expressionVariables );
	}

	/**
	 * Returns the dynamic payload set via {@link #withDynamicPayload(Object)}.
	 */
	public Object getDynamicPayload() {
		return dynamicPayload;
	}

	/**
	 * Sets the constraint validator payload that will be returned by
	 * {@link #getConstraintValidatorPayload(Class)}.
	 */
	public void setConstraintValidatorPayload(Object payload) {
		this.constraintValidatorPayload = payload;
	}

	// -- internals --------------------------------------------------------------------

	private ConstraintValidatorContext addViolation(String messageTemplate, List<String> pathNodes,
			Map<String, Object> violationMessageParameters, Map<String, Object> violationExpressionVariables) {
		StringJoiner joiner = new StringJoiner( "." );
		for ( String node : pathNodes ) {
			if ( node != null ) {
				joiner.add( node );
			}
		}
		String propertyPath = joiner.toString();

		// Merge context-level parameters with violation-level parameters (violation-level wins)
		Map<String, Object> mergedMessageParams = new LinkedHashMap<>( messageParameters );
		mergedMessageParams.putAll( violationMessageParameters );

		Map<String, Object> mergedExpressionVars = new LinkedHashMap<>( expressionVariables );
		mergedExpressionVars.putAll( violationExpressionVariables );

		violations.add( new RecordedHibernateViolation( messageTemplate, propertyPath,
				mergedMessageParams, mergedExpressionVars ) );
		return this;
	}

	// -- Recorded violation -----------------------------------------------------------

	private static final class RecordedHibernateViolation implements HibernateTestConstraintViolation {

		private final String messageTemplate;
		private final String propertyPath;
		private final Map<String, Object> messageParameters;
		private final Map<String, Object> expressionVariables;

		RecordedHibernateViolation(String messageTemplate, String propertyPath,
				Map<String, Object> messageParameters, Map<String, Object> expressionVariables) {
			this.messageTemplate = messageTemplate;
			this.propertyPath = propertyPath;
			this.messageParameters = Collections.unmodifiableMap( new LinkedHashMap<>( messageParameters ) );
			this.expressionVariables = Collections.unmodifiableMap( new LinkedHashMap<>( expressionVariables ) );
		}

		@Override
		public String getMessageTemplate() {
			return messageTemplate;
		}

		@Override
		public String getPropertyPath() {
			return propertyPath;
		}

		@Override
		public Map<String, Object> getMessageParameters() {
			return messageParameters;
		}

		@Override
		public Map<String, Object> getExpressionVariables() {
			return expressionVariables;
		}
	}

	// -- Recording builder ------------------------------------------------------------

	private class RecordingHibernateConstraintViolationBuilder implements HibernateConstraintViolationBuilder {

		private final String messageTemplate;
		private final List<String> pathNodes = new ArrayList<>();
		private final Map<String, Object> builderMessageParameters = new LinkedHashMap<>();
		private final Map<String, Object> builderExpressionVariables = new LinkedHashMap<>();

		RecordingHibernateConstraintViolationBuilder(String messageTemplate) {
			this.messageTemplate = messageTemplate;
		}

		@Override
		public HibernateConstraintViolationBuilder enableExpressionLanguage(ExpressionLanguageFeatureLevel level) {
			// Recording only -- no interpolation in test context
			return this;
		}

		@SuppressWarnings("deprecation")
		@Override
		public NodeBuilderDefinedContext addNode(String name) {
			pathNodes.add( name );
			return new RecordingNodeBuilderDefinedContext();
		}

		@Override
		public NodeBuilderCustomizableContext addPropertyNode(String name) {
			pathNodes.add( name );
			return new RecordingNodeBuilderCustomizableContext();
		}

		@Override
		public LeafNodeBuilderCustomizableContext addBeanNode() {
			return new RecordingLeafNodeBuilderCustomizableContext();
		}

		@Override
		public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
				Class<?> containerType, Integer typeArgumentIndex) {
			pathNodes.add( name );
			return new RecordingContainerElementNodeBuilderCustomizableContext();
		}

		@Override
		public NodeBuilderDefinedContext addParameterNode(int index) {
			pathNodes.add( "<parameter " + index + ">" );
			return new RecordingNodeBuilderDefinedContext();
		}

		@Override
		public ConstraintValidatorContext addConstraintViolation() {
			return HibernateTestConstraintValidatorContext.this.addViolation(
					messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
		}

		// -- inner builder contexts ---------------------------------------------------

		private class RecordingNodeBuilderDefinedContext implements NodeBuilderDefinedContext {

			@SuppressWarnings("deprecation")
			@Override
			public NodeBuilderCustomizableContext addNode(String name) {
				pathNodes.add( name );
				return new RecordingNodeBuilderCustomizableContext();
			}

			@Override
			public NodeBuilderCustomizableContext addPropertyNode(String name) {
				pathNodes.add( name );
				return new RecordingNodeBuilderCustomizableContext();
			}

			@Override
			public LeafNodeBuilderCustomizableContext addBeanNode() {
				return new RecordingLeafNodeBuilderCustomizableContext();
			}

			@Override
			public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
					Class<?> containerType, Integer typeArgumentIndex) {
				pathNodes.add( name );
				return new RecordingContainerElementNodeBuilderCustomizableContext();
			}

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return HibernateTestConstraintValidatorContext.this.addViolation(
						messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
			}
		}

		private class RecordingNodeBuilderCustomizableContext implements NodeBuilderCustomizableContext {

			@Override
			public NodeContextBuilder inIterable() {
				return new RecordingNodeContextBuilder();
			}

			@Override
			public NodeBuilderCustomizableContext inContainer(Class<?> containerClass, Integer typeArgumentIndex) {
				return this;
			}

			@SuppressWarnings("deprecation")
			@Override
			public NodeBuilderCustomizableContext addNode(String name) {
				pathNodes.add( name );
				return new RecordingNodeBuilderCustomizableContext();
			}

			@Override
			public NodeBuilderCustomizableContext addPropertyNode(String name) {
				pathNodes.add( name );
				return new RecordingNodeBuilderCustomizableContext();
			}

			@Override
			public LeafNodeBuilderCustomizableContext addBeanNode() {
				return new RecordingLeafNodeBuilderCustomizableContext();
			}

			@Override
			public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
					Class<?> containerType, Integer typeArgumentIndex) {
				pathNodes.add( name );
				return new RecordingContainerElementNodeBuilderCustomizableContext();
			}

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return HibernateTestConstraintValidatorContext.this.addViolation(
						messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
			}
		}

		private class RecordingNodeContextBuilder implements NodeContextBuilder {

			@Override
			public NodeBuilderDefinedContext atKey(Object key) {
				return new RecordingNodeBuilderDefinedContext();
			}

			@Override
			public NodeBuilderDefinedContext atIndex(Integer index) {
				return new RecordingNodeBuilderDefinedContext();
			}

			@SuppressWarnings("deprecation")
			@Override
			public NodeBuilderCustomizableContext addNode(String name) {
				pathNodes.add( name );
				return new RecordingNodeBuilderCustomizableContext();
			}

			@Override
			public NodeBuilderCustomizableContext addPropertyNode(String name) {
				pathNodes.add( name );
				return new RecordingNodeBuilderCustomizableContext();
			}

			@Override
			public LeafNodeBuilderCustomizableContext addBeanNode() {
				return new RecordingLeafNodeBuilderCustomizableContext();
			}

			@Override
			public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
					Class<?> containerType, Integer typeArgumentIndex) {
				pathNodes.add( name );
				return new RecordingContainerElementNodeBuilderCustomizableContext();
			}

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return HibernateTestConstraintValidatorContext.this.addViolation(
						messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
			}
		}

		private class RecordingLeafNodeBuilderCustomizableContext implements LeafNodeBuilderCustomizableContext {

			@Override
			public LeafNodeContextBuilder inIterable() {
				return new RecordingLeafNodeContextBuilder();
			}

			@Override
			public LeafNodeBuilderCustomizableContext inContainer(Class<?> containerClass, Integer typeArgumentIndex) {
				return this;
			}

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return HibernateTestConstraintValidatorContext.this.addViolation(
						messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
			}
		}

		private class RecordingLeafNodeContextBuilder implements LeafNodeContextBuilder {

			@Override
			public LeafNodeBuilderDefinedContext atKey(Object key) {
				return new RecordingLeafNodeBuilderDefinedContext();
			}

			@Override
			public LeafNodeBuilderDefinedContext atIndex(Integer index) {
				return new RecordingLeafNodeBuilderDefinedContext();
			}

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return HibernateTestConstraintValidatorContext.this.addViolation(
						messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
			}
		}

		private class RecordingLeafNodeBuilderDefinedContext implements LeafNodeBuilderDefinedContext {

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return HibernateTestConstraintValidatorContext.this.addViolation(
						messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
			}
		}

		private class RecordingContainerElementNodeBuilderCustomizableContext
				implements ContainerElementNodeBuilderCustomizableContext {

			@Override
			public ContainerElementNodeContextBuilder inIterable() {
				return new RecordingContainerElementNodeContextBuilder();
			}

			@Override
			public NodeBuilderCustomizableContext addPropertyNode(String name) {
				pathNodes.add( name );
				return new RecordingNodeBuilderCustomizableContext();
			}

			@Override
			public LeafNodeBuilderCustomizableContext addBeanNode() {
				return new RecordingLeafNodeBuilderCustomizableContext();
			}

			@Override
			public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
					Class<?> containerType, Integer typeArgumentIndex) {
				pathNodes.add( name );
				return new RecordingContainerElementNodeBuilderCustomizableContext();
			}

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return HibernateTestConstraintValidatorContext.this.addViolation(
						messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
			}
		}

		private class RecordingContainerElementNodeContextBuilder implements ContainerElementNodeContextBuilder {

			@Override
			public ContainerElementNodeBuilderDefinedContext atKey(Object key) {
				return new RecordingContainerElementNodeBuilderDefinedContext();
			}

			@Override
			public ContainerElementNodeBuilderDefinedContext atIndex(Integer index) {
				return new RecordingContainerElementNodeBuilderDefinedContext();
			}

			@Override
			public NodeBuilderCustomizableContext addPropertyNode(String name) {
				pathNodes.add( name );
				return new RecordingNodeBuilderCustomizableContext();
			}

			@Override
			public LeafNodeBuilderCustomizableContext addBeanNode() {
				return new RecordingLeafNodeBuilderCustomizableContext();
			}

			@Override
			public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
					Class<?> containerType, Integer typeArgumentIndex) {
				pathNodes.add( name );
				return new RecordingContainerElementNodeBuilderCustomizableContext();
			}

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return HibernateTestConstraintValidatorContext.this.addViolation(
						messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
			}
		}

		private class RecordingContainerElementNodeBuilderDefinedContext
				implements ContainerElementNodeBuilderDefinedContext {

			@Override
			public NodeBuilderCustomizableContext addPropertyNode(String name) {
				pathNodes.add( name );
				return new RecordingNodeBuilderCustomizableContext();
			}

			@Override
			public LeafNodeBuilderCustomizableContext addBeanNode() {
				return new RecordingLeafNodeBuilderCustomizableContext();
			}

			@Override
			public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String name,
					Class<?> containerType, Integer typeArgumentIndex) {
				pathNodes.add( name );
				return new RecordingContainerElementNodeBuilderCustomizableContext();
			}

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return HibernateTestConstraintValidatorContext.this.addViolation(
						messageTemplate, pathNodes, builderMessageParameters, builderExpressionVariables );
			}
		}
	}
}
