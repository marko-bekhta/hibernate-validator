/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testutil;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

class StandaloneTestConstraintValidatorContext implements TestConstraintValidatorContext {

	private static final ClockProvider DEFAULT_CLOCK_PROVIDER = Clock::systemDefaultZone;

	private final String defaultTemplate;
	private final ClockProvider clockProvider;
	private final List<TestConstraintViolation> violations = new ArrayList<>();
	private boolean defaultViolationDisabled;

	StandaloneTestConstraintValidatorContext(String defaultTemplate, ClockProvider clockProvider) {
		this.defaultTemplate = defaultTemplate;
		this.clockProvider = clockProvider != null ? clockProvider : DEFAULT_CLOCK_PROVIDER;
	}

	@Override
	public boolean isDefaultViolationDisabled() {
		return defaultViolationDisabled;
	}

	@Override
	public List<TestConstraintViolation> getTestViolations() {
		return Collections.unmodifiableList( violations );
	}

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
	public ConstraintViolationBuilder buildConstraintViolationWithTemplate(String messageTemplate) {
		return new RecordingConstraintViolationBuilder( messageTemplate );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if ( type.isInstance( this ) ) {
			return (T) this;
		}
		throw new ValidationException( "Cannot unwrap to " + type.getName() );
	}

	private ConstraintValidatorContext addViolation(String messageTemplate, List<String> pathNodes) {
		StringJoiner joiner = new StringJoiner( "." );
		for ( String node : pathNodes ) {
			if ( node != null ) {
				joiner.add( node );
			}
		}
		String propertyPath = joiner.toString();
		violations.add( new RecordedViolation( messageTemplate, propertyPath ) );
		return this;
	}

	private static final class RecordedViolation implements TestConstraintViolation {

		private final String messageTemplate;
		private final String propertyPath;

		RecordedViolation(String messageTemplate, String propertyPath) {
			this.messageTemplate = messageTemplate;
			this.propertyPath = propertyPath;
		}

		@Override
		public String getMessageTemplate() {
			return messageTemplate;
		}

		@Override
		public String getPropertyPath() {
			return propertyPath;
		}
	}

	private class RecordingConstraintViolationBuilder implements ConstraintViolationBuilder {

		private final String messageTemplate;
		private final List<String> pathNodes = new ArrayList<>();

		RecordingConstraintViolationBuilder(String messageTemplate) {
			this.messageTemplate = messageTemplate;
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
			return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
		}

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
				return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
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
				return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
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
				return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
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
				return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
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
				return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
			}
		}

		private class RecordingLeafNodeBuilderDefinedContext implements LeafNodeBuilderDefinedContext {

			@Override
			public ConstraintValidatorContext addConstraintViolation() {
				return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
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
				return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
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
				return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
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
				return StandaloneTestConstraintValidatorContext.this.addViolation( messageTemplate, pathNodes );
			}
		}
	}
}
