/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.ap.testmodel.annotationparameters;

import javax.validation.constraints.Pattern;

public class InvalidPatternParameters {

	@Pattern( regexp = "\\" )
	private String strings1;

	@Pattern( regexp = "[a" )
	private String strings2;

	@Pattern( regexp = "+" )
	private String strings3;

}
