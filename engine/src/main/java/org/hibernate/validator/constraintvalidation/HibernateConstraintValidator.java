/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.constraintvalidation;

import java.lang.annotation.Annotation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.metadata.ConstraintDescriptor;

import org.hibernate.validator.Incubating;

/**
 * Extends the contract of {@link ConstraintValidator} by adding {@link #initialize(ConstraintDescriptor, HibernateConstraintValidatorInitializationContext)}
 * which gives more flexibility during initialization of a constraint validator.
 *
 * @author Marko Bekhta
 * @since 6.1
 */
@Incubating
public interface HibernateConstraintValidator<A extends Annotation, T> extends ConstraintValidator<A, T> {

	/**
	 * Initializes the validator in preparation for {@link #isValid(Object, ConstraintValidatorContext)} calls.
	 * It is an alternative to {@link #initialize(Annotation)} method. Should be used if any additional information
	 * except annotation is needed to initialize a validator.
	 * Note, when using {@link HibernateConstraintValidator} user should only override one of the methods, either
	 * {@link #initialize(Annotation)} or {@link #initialize(ConstraintDescriptor, HibernateConstraintValidatorInitializationContext)}.
	 * It is guaranteed that in case of {@link HibernateConstraintValidator} just the
	 * {@link #initialize(ConstraintDescriptor, HibernateConstraintValidatorInitializationContext)} will be called during initialization.
	 *
	 * @param constraintDescriptor a constraint descriptor for a given constraint declaration
	 * @param initializationContext an initialization context for a current {@link ConstraintValidatorFactory}
	 */
	default void initialize(ConstraintDescriptor<A> constraintDescriptor, HibernateConstraintValidatorInitializationContext initializationContext) {
		initialize( constraintDescriptor.getAnnotation() );
	}
}
