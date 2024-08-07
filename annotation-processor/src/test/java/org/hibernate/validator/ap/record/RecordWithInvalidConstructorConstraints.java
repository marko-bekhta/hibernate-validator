/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.ap.record;

import java.util.Date;

import jakarta.validation.constraints.FutureOrPresent;

/**
 * @author Jan Schatteman
 */
public record RecordWithInvalidConstructorConstraints(String string, Date date) {
	public RecordWithInvalidConstructorConstraints(@FutureOrPresent String string, @FutureOrPresent Date date) {
		this.string = string;
		this.date = date;
	}
}
