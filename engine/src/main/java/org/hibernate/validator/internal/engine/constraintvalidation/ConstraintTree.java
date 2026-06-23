/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.internal.engine.constraintvalidation;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ValidationException;

import org.hibernate.validator.constraints.CompositionType;
import org.hibernate.validator.internal.engine.validationcontext.ValidationContext;
import org.hibernate.validator.internal.engine.valuecontext.ValueContext;
import org.hibernate.validator.internal.metadata.descriptor.ConstraintDescriptorImpl;
import org.hibernate.validator.internal.util.CollectionHelper;
import org.hibernate.validator.internal.util.logging.Log;
import org.hibernate.validator.internal.util.logging.LoggerFactory;
import org.hibernate.validator.internal.util.stereotypes.Immutable;

import org.jboss.logging.Logger;

/**
 * Due to constraint composition a single constraint annotation can lead to a whole constraint tree being validated.
 * This class encapsulates such a tree.
 *
 * @author Hardy Ferentschik
 * @author Federico Mancini
 * @author Dag Hovland
 * @author Kevin Pollet &lt;kevin.pollet@serli.com&gt; (C) 2012 SERLI
 * @author Guillaume Smet
 * @author Marko Bekhta
 */
public final class ConstraintTree<A extends Annotation> {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	public static boolean isTraceEnabled() {
		return LOG.isEnabled( Logger.Level.TRACE );
	}

	private enum Strategy {
		SIMPLE,
		REPORT_AS_SINGLE,
		AND,
		OR
	}

	private volatile ConstraintValidator<A, ?> defaultInitializedConstraintValidator;
	private final ConstraintDescriptorImpl<A> descriptor;
	private final Type validatedValueType;

	@Immutable
	private final List<ConstraintTree<?>> children;
	private final Strategy strategy;
	private final CompositionType compositionType;
	private final boolean reportAsSingleViolation;
	private final boolean hasMainConstraintValidator;

	private ConstraintTree(ConstraintValidatorManager constraintValidatorManager, ConstraintDescriptorImpl<A> descriptor, Type validatedValueType) {
		this.descriptor = descriptor;
		this.validatedValueType = validatedValueType;

		if ( constraintValidatorManager.isPredefinedScope() ) {
			this.defaultInitializedConstraintValidator = constraintValidatorManager.getInitializedValidator( validatedValueType,
					descriptor,
					constraintValidatorManager.getDefaultConstraintValidatorFactory(),
					constraintValidatorManager.getDefaultConstraintValidatorInitializationContext() );
		}

		Set<ConstraintDescriptorImpl<?>> composingConstraints = descriptor.getComposingConstraintImpls();
		if ( composingConstraints.isEmpty() ) {
			this.children = Collections.emptyList();
			this.compositionType = null;
			this.reportAsSingleViolation = false;
			this.hasMainConstraintValidator = true;
			this.strategy = Strategy.SIMPLE;
		}
		else {
			List<ConstraintTree<?>> childList = new ArrayList<>( composingConstraints.size() );
			for ( ConstraintDescriptorImpl<?> desc : composingConstraints ) {
				childList.add( new ConstraintTree<>( constraintValidatorManager, desc, validatedValueType ) );
			}
			this.children = CollectionHelper.toImmutableList( childList );
			this.compositionType = descriptor.getCompositionType();
			this.reportAsSingleViolation = descriptor.isReportAsSingleViolation() || this.compositionType == CompositionType.ALL_FALSE;
			this.hasMainConstraintValidator = !descriptor.getMatchingConstraintValidatorDescriptors().isEmpty();
			if ( reportAsSingleViolation ) {
				this.strategy = Strategy.REPORT_AS_SINGLE;
			}
			else if ( compositionType == CompositionType.OR ) {
				this.strategy = Strategy.OR;
			}
			else {
				this.strategy = Strategy.AND;
			}
		}
	}

	public static <U extends Annotation> ConstraintTree<U> of(ConstraintValidatorManager constraintValidatorManager,
			ConstraintDescriptorImpl<U> composingDescriptor, Type validatedValueType) {
		return new ConstraintTree<>( constraintValidatorManager, composingDescriptor, validatedValueType );
	}

	public ConstraintDescriptorImpl<A> getDescriptor() {
		return descriptor;
	}

	public boolean validateConstraints(ValidationContext<?> validationContext, ValueContext<?, ?> valueContext) {
		return switch ( strategy ) {
			case SIMPLE -> validateConstraintsSimple( validationContext, valueContext );
			case REPORT_AS_SINGLE -> validateConstraintsReportAsSingle( validationContext, valueContext );
			case AND, OR -> validateConstraintsComposing( validationContext, valueContext );
		};
	}

	/**
	 * Violation-producing validation with lazy list. Used by AND/OR parents.
	 * Returns null if no violations were produced, otherwise the (possibly newly created) list.
	 */
	private List<ConstraintViolationCreationContext> doValidateWithViolations(
			ValidationContext<?> validationContext, ValueContext<?, ?> valueContext,
			List<ConstraintViolationCreationContext> violations) {
		return switch ( strategy ) {
			case SIMPLE -> doValidateSimpleWithViolations( validationContext, valueContext, violations );
			case REPORT_AS_SINGLE -> doValidateReportAsSingleWithViolations( validationContext, valueContext, violations );
			case AND, OR -> doValidateComposingWithViolations( validationContext, valueContext, violations );
		};
	}

	// Main entry points for different strategies:
	// --------------------------------------------------------------------------------------------
	private boolean validateConstraintsSimple(ValidationContext<?> validationContext, ValueContext<?, ?> valueContext) {
		ConstraintValidator<A, ?> validator = getInitializedConstraintValidator( validationContext, valueContext );
		ConstraintValidatorContextImpl ctx = validationContext.createConstraintValidatorContextFor( descriptor, valueContext.getPropertyPath() );

		if ( validateSingleConstraint( validationContext, valueContext, ctx, validator ) ) {
			return true;
		}

		ctx.contributeConstraintViolations( validationContext, valueContext );

		return false;
	}

	private boolean validateConstraintsReportAsSingle(ValidationContext<?> validationContext, ValueContext<?, ?> valueContext) {
		boolean allTrue = true;
		boolean atLeastOneTrue = false;

		for ( ConstraintTree<?> child : children ) {
			if ( child.doValidate( validationContext, valueContext ) ) {
				atLeastOneTrue = true;
				if ( compositionType == CompositionType.OR ) {
					break;
				}
			}
			else {
				allTrue = false;
				if ( compositionType == CompositionType.AND ) {
					break;
				}
			}
		}

		ConstraintValidatorContextImpl mainContext = null;
		if ( mainConstraintNeedsEvaluation( validationContext, allTrue ) ) {
			ConstraintValidator<A, ?> validator = getInitializedConstraintValidator( validationContext, valueContext );
			mainContext = validationContext.createConstraintValidatorContextFor( descriptor, valueContext.getPropertyPath() );
			if ( validateSingleConstraint( validationContext, valueContext, mainContext, validator ) ) {
				atLeastOneTrue = true;
				mainContext = null;
			}
			else {
				allTrue = false;
			}
		}

		if ( compositionPasses( allTrue, atLeastOneTrue ) ) {
			return true;
		}

		// report single violation:
		if ( mainContext != null ) {
			mainContext.contributeConstraintViolations( validationContext, valueContext );
		}
		else {
			validationContext.createConstraintValidatorContextFor( descriptor, valueContext.getPropertyPath() )
					.contributeConstraintViolations( validationContext, valueContext );
		}
		return false;
	}

	private boolean validateConstraintsComposing(ValidationContext<?> validationContext, ValueContext<?, ?> valueContext) {
		List<ConstraintViolationCreationContext> violations = doValidateComposingWithViolations( validationContext, valueContext, null );

		if ( violations == null || violations.isEmpty() ) {
			return true;
		}

		for ( ConstraintViolationCreationContext c : violations ) {
			validationContext.addConstraintFailure( valueContext, c );
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------

	private List<ConstraintViolationCreationContext> doValidateSimpleWithViolations(
			ValidationContext<?> validationContext, ValueContext<?, ?> valueContext,
			List<ConstraintViolationCreationContext> violations) {
		ConstraintValidator<A, ?> validator = getInitializedConstraintValidator( validationContext, valueContext );
		ConstraintValidatorContextImpl ctx = validationContext.createConstraintValidatorContextFor(
				descriptor, valueContext.getPropertyPath()
		);

		if ( validateSingleConstraint( validationContext, valueContext, ctx, validator ) ) {
			return violations;
		}

		if ( violations == null ) {
			violations = new ArrayList<>();
		}
		ctx.contributeConstraintViolationCreationContexts( violations );
		return violations;
	}

	private List<ConstraintViolationCreationContext> doValidateReportAsSingleWithViolations(
			ValidationContext<?> validationContext, ValueContext<?, ?> valueContext,
			List<ConstraintViolationCreationContext> violations) {
		boolean allTrue = true;
		boolean atLeastOneTrue = false;

		for ( ConstraintTree<?> child : children ) {
			if ( child.doValidate( validationContext, valueContext ) ) {
				atLeastOneTrue = true;
				if ( compositionType == CompositionType.OR ) {
					break;
				}
			}
			else {
				allTrue = false;
				if ( compositionType == CompositionType.AND ) {
					break;
				}
			}
		}

		ConstraintValidatorContextImpl mainContext = null;
		if ( mainConstraintNeedsEvaluation( validationContext, allTrue ) ) {
			ConstraintValidator<A, ?> validator = getInitializedConstraintValidator( validationContext, valueContext );
			mainContext = validationContext.createConstraintValidatorContextFor(
					descriptor, valueContext.getPropertyPath()
			);
			if ( validateSingleConstraint( validationContext, valueContext, mainContext, validator ) ) {
				atLeastOneTrue = true;
				mainContext = null;
			}
			else {
				allTrue = false;
			}
		}

		if ( compositionPasses( allTrue, atLeastOneTrue ) ) {
			return violations;
		}

		if ( violations == null ) {
			violations = new ArrayList<>();
		}

		if ( mainContext != null ) {
			mainContext.contributeConstraintViolationCreationContexts( violations );
		}
		else {
			validationContext.createConstraintValidatorContextFor( descriptor, valueContext.getPropertyPath() )
					.contributeConstraintViolationCreationContexts( violations );
		}
		return violations;
	}

	private List<ConstraintViolationCreationContext> doValidateComposingWithViolations(
			ValidationContext<?> validationContext, ValueContext<?, ?> valueContext,
			List<ConstraintViolationCreationContext> violations) {
		int startIndex = violations == null ? 0 : violations.size();
		boolean allTrue = true;
		boolean atLeastOneTrue = false;

		for ( ConstraintTree<?> child : children ) {
			int childStart = violations == null ? 0 : violations.size();
			violations = child.doValidateWithViolations( validationContext, valueContext, violations );
			boolean childPassed = ( violations == null || violations.size() == childStart );

			if ( childPassed ) {
				atLeastOneTrue = true;
				if ( compositionType == CompositionType.OR ) {
					break;
				}
			}
			else {
				allTrue = false;
				if ( compositionType == CompositionType.AND && validationContext.isFailFastModeEnabled() ) {
					break;
				}
			}
		}

		ConstraintValidatorContextImpl mainContext = null;
		if ( mainConstraintNeedsEvaluation( validationContext, allTrue ) ) {
			ConstraintValidator<A, ?> validator = getInitializedConstraintValidator( validationContext, valueContext );
			mainContext = validationContext.createConstraintValidatorContextFor(
					descriptor, valueContext.getPropertyPath()
			);
			if ( validateSingleConstraint( validationContext, valueContext, mainContext, validator ) ) {
				atLeastOneTrue = true;
				mainContext = null;
			}
			else {
				allTrue = false;
			}
		}

		if ( compositionPasses( allTrue, atLeastOneTrue ) ) {
			if ( violations != null && violations.size() > startIndex ) {
				violations.subList( startIndex, violations.size() ).clear();
			}
			return violations;
		}

		if ( mainContext != null ) {
			if ( violations == null ) {
				violations = new ArrayList<>();
			}
			mainContext.contributeConstraintViolationCreationContexts( violations );
		}

		return violations;
	}

	// Boolean-only validation. Used by REPORT_AS_SINGLE parents that don't need child violations.
	// --------------------------------------------------------------------------------------------
	private boolean doValidate(ValidationContext<?> validationContext, ValueContext<?, ?> valueContext) {
		if ( strategy == Strategy.SIMPLE ) {
			return doValidateSimple( validationContext, valueContext );
		}
		return doValidateComposing( validationContext, valueContext );
	}

	private boolean doValidateSimple(ValidationContext<?> validationContext, ValueContext<?, ?> valueContext) {
		ConstraintValidator<A, ?> validator = getInitializedConstraintValidator( validationContext, valueContext );
		ConstraintValidatorContextImpl ctx = validationContext.createConstraintValidatorContextFor( descriptor, valueContext.getPropertyPath() );
		return validateSingleConstraint( validationContext, valueContext, ctx, validator );
	}

	private boolean doValidateComposing(ValidationContext<?> validationContext, ValueContext<?, ?> valueContext) {
		boolean allTrue = true;
		boolean atLeastOneTrue = false;

		for ( ConstraintTree<?> child : children ) {
			if ( child.doValidate( validationContext, valueContext ) ) {
				atLeastOneTrue = true;
				if ( compositionType == CompositionType.OR ) {
					break;
				}
			}
			else {
				allTrue = false;
				if ( compositionType == CompositionType.AND
						&& ( validationContext.isFailFastModeEnabled() || reportAsSingleViolation ) ) {
					break;
				}
			}
		}

		if ( mainConstraintNeedsEvaluation( validationContext, allTrue ) ) {
			ConstraintValidator<A, ?> validator = getInitializedConstraintValidator( validationContext, valueContext );
			ConstraintValidatorContextImpl ctx = validationContext.createConstraintValidatorContextFor(
					descriptor, valueContext.getPropertyPath()
			);
			if ( validateSingleConstraint( validationContext, valueContext, ctx, validator ) ) {
				atLeastOneTrue = true;
			}
			else {
				allTrue = false;
			}
		}

		return compositionPasses( allTrue, atLeastOneTrue );
	}

	// Some general helpers
	// --------------------------------------------------------------------------------------------
	private boolean mainConstraintNeedsEvaluation(ValidationContext<?> validationContext, boolean allPassingSoFar) {
		// we are dealing with a composing constraint with no validator for the main constraint
		if ( !hasMainConstraintValidator ) {
			return false;
		}

		if ( allPassingSoFar ) {
			return true;
		}

		// report as single violation and there is already a violation
		if ( reportAsSingleViolation && compositionType == CompositionType.AND ) {
			return false;
		}

		// explicit fail fast mode
		return !validationContext.isFailFastModeEnabled();
	}

	private boolean compositionPasses(boolean allTrue, boolean atLeastOneTrue) {
		return switch ( compositionType ) {
			case OR -> atLeastOneTrue;
			case AND -> allTrue;
			case ALL_FALSE -> !atLeastOneTrue;
		};
	}

	private ConstraintValidator<A, ?> getInitializedConstraintValidator(ValidationContext<?> validationContext, ValueContext<?, ?> valueContext) {
		ConstraintValidator<A, ?> validator;

		if ( validationContext.getConstraintValidatorManager().isPredefinedScope() ) {
			validator = defaultInitializedConstraintValidator;
		}
		else {
			if ( validationContext.getConstraintValidatorFactory() == validationContext.getConstraintValidatorManager().getDefaultConstraintValidatorFactory()
					&& validationContext.getConstraintValidatorInitializationContext() == validationContext.getConstraintValidatorManager()
							.getDefaultConstraintValidatorInitializationContext() ) {
				validator = defaultInitializedConstraintValidator;

				if ( validator == null ) {
					synchronized (this) {
						validator = defaultInitializedConstraintValidator;
						if ( validator == null ) {
							validator = validationContext.getConstraintValidatorManager().getInitializedValidator(
									validatedValueType,
									descriptor,
									validationContext.getConstraintValidatorManager().getDefaultConstraintValidatorFactory(),
									validationContext.getConstraintValidatorManager().getDefaultConstraintValidatorInitializationContext() );

							defaultInitializedConstraintValidator = validator;
						}
					}
				}
			}
			else {
				// For now, we don't cache the result in the ConstraintTree if we don't use the default constraint validator
				// factory. Creating a lot of CHM for that cache might not be a good idea and we prefer being conservative
				// for now. Note that we have the ConstraintValidatorManager cache that mitigates the situation.
				// If you come up with a use case where it makes sense, please reach out to us.
				validator = validationContext.getConstraintValidatorManager().getInitializedValidator(
						validatedValueType,
						descriptor,
						validationContext.getConstraintValidatorFactory(),
						validationContext.getConstraintValidatorInitializationContext()
				);
			}
		}

		if ( validator == null ) {
			throw getExceptionForNullValidator( validatedValueType, valueContext.getPropertyPath().asString() );
		}

		return validator;
	}

	private ValidationException getExceptionForNullValidator(Type validatedValueType, String path) {
		if ( descriptor.getConstraintType() == ConstraintDescriptorImpl.ConstraintType.CROSS_PARAMETER ) {
			return LOG.getValidatorForCrossParameterConstraintMustEitherValidateObjectOrObjectArrayException(
					descriptor.getAnnotationType()
			);
		}
		else {
			String className = validatedValueType.toString();
			if ( validatedValueType instanceof Class<?> clazz ) {
				if ( clazz.isArray() ) {
					className = clazz.getComponentType().toString() + "[]";
				}
				else {
					className = clazz.getName();
				}
			}
			return LOG.getNoValidatorFoundForTypeException( descriptor.getAnnotationType(), className, path );
		}
	}

	private <V> boolean validateSingleConstraint(
			ValidationContext<?> validationContext,
			ValueContext<?, ?> valueContext,
			ConstraintValidatorContextImpl constraintValidatorContext,
			ConstraintValidator<A, V> validator) {
		if ( validationContext.isConstraintTreeTraceEnabled() ) {
			if ( validationContext.isShowValidatedValuesInTraceLogs() ) {
				LOG.tracef(
						"Validating value %s against constraint defined by %s.",
						valueContext.getCurrentValidatedValue(),
						descriptor
				);
			}
			else {
				LOG.tracef( "Validating against constraint defined by %s.", descriptor );
			}
		}
		try {
			@SuppressWarnings("unchecked")
			V validatedValue = (V) valueContext.getCurrentValidatedValue();
			return validator.isValid( validatedValue, constraintValidatorContext );
		}
		catch (RuntimeException e) {
			if ( e instanceof ConstraintDeclarationException ) {
				throw e;
			}
			throw LOG.getExceptionDuringIsValidCallException( e );
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConstraintTree" );
		sb.append( "{ descriptor=" ).append( descriptor );
		sb.append( '}' );
		return sb.toString();
	}

}
