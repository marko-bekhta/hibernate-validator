/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.integrationtest.jupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.testutil.Annotations;

import org.junit.jupiter.api.Test;

class AnnotationsIT {

	@Test
	void createAnnotationWithDefaults() {
		NotNull notNull = Annotations.of( NotNull.class );
		assertNotNull( notNull );
	}

	@Test
	void createAnnotationWithBuilder() {
		Size size = Annotations.builder( Size.class )
				.attribute( "min", 1 )
				.attribute( "max", 10 )
				.build();
		assertEquals( 1, size.min() );
		assertEquals( 10, size.max() );
	}
}
