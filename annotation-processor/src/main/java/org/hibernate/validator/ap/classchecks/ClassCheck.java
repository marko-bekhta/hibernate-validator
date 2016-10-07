/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.ap.classchecks;

import java.util.Collection;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import org.hibernate.validator.ap.checks.ConstraintCheckError;

/**
 * <p>
 * Implementations represent checks that determine whether a given class
 * is implemented correctly.
 * </p>
 * <p>
 * Implementations should be derived from {@link AbstractClassCheck} in
 * order to implement only those check methods applicable for the element kinds
 * supported by the check.
 * </p>
 *
 *
 * @author Marko Bekhta
 */
public interface ClassCheck {

	/**
	 * Checks whether the given method is written correctly.
	 *
	 * @param element A method under investigation
	 *
	 * @return A collection with errors, that describe, why the given method
	 *         is not correctly implemented. In case no errors occur (the
	 *         method is written correctly), an empty set must be returned.
	 */
	Collection<ConstraintCheckError> checkMethod(ExecutableElement element);


	/**
	 * Run all checks on the element.
	 *
	 * @param element an element under investigation
	 * @return A collection with errors, that describe, why the given element
	 *         does not pass the checks. In case no errors occur (all checks completed successfully),
	 *         an empty set must be returned.
	 */
	Collection<ConstraintCheckError> execute(Element element);
}
