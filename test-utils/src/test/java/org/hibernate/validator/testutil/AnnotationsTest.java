/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testutil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;

import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.testng.annotations.Test;

/**
 * Tests for {@link Annotations}.
 */
public class AnnotationsTest {

	@Test
	public void ofCreatesAnnotationWithDefaults() {
		NotNull notNull = Annotations.of( NotNull.class );

		assertThat( notNull ).isNotNull();
		assertThat( notNull.annotationType() ).isEqualTo( NotNull.class );
		assertThat( notNull.message() ).isEqualTo( "{jakarta.validation.constraints.NotNull.message}" );
		assertThat( notNull.groups() ).isEmpty();
		assertThat( notNull.payload() ).isEmpty();
	}

	@Test
	public void builderWithMessage() {
		NotNull notNull = Annotations.builder( NotNull.class )
				.message( "must not be null" )
				.build();

		assertThat( notNull.message() ).isEqualTo( "must not be null" );
		assertThat( notNull.groups() ).isEmpty();
		assertThat( notNull.payload() ).isEmpty();
	}

	@Test
	public void builderWithGroups() {
		NotNull notNull = Annotations.builder( NotNull.class )
				.groups( TestGroup.class )
				.build();

		assertThat( notNull.groups() ).containsExactly( TestGroup.class );
	}

	@Test
	public void builderWithPayload() {
		NotNull notNull = Annotations.builder( NotNull.class )
				.payload( TestPayload.class )
				.build();

		assertThat( notNull.payload() ).containsExactly( TestPayload.class );
	}

	@Test
	public void builderWithCustomAttribute() {
		Size size = Annotations.builder( Size.class )
				.attribute( "min", 1 )
				.attribute( "max", 10 )
				.build();

		assertThat( size.min() ).isEqualTo( 1 );
		assertThat( size.max() ).isEqualTo( 10 );
		assertThat( size.message() ).isEqualTo( "{jakarta.validation.constraints.Size.message}" );
	}

	@Test
	public void sizeAnnotationDefaults() {
		Size size = Annotations.of( Size.class );

		assertThat( size.min() ).isEqualTo( 0 );
		assertThat( size.max() ).isEqualTo( Integer.MAX_VALUE );
	}

	@Test
	public void annotationTypeReturnsCorrectType() {
		Size size = Annotations.of( Size.class );

		assertThat( size.annotationType() ).isEqualTo( Size.class );
	}

	@Test
	public void equalsFollowsAnnotationContract() {
		Size size1 = Annotations.builder( Size.class )
				.attribute( "min", 5 )
				.attribute( "max", 10 )
				.build();

		Size size2 = Annotations.builder( Size.class )
				.attribute( "min", 5 )
				.attribute( "max", 10 )
				.build();

		assertThat( size1 ).isEqualTo( size2 );
		assertThat( size2 ).isEqualTo( size1 );
	}

	@Test
	public void equalsReturnsFalseForDifferentValues() {
		Size size1 = Annotations.builder( Size.class )
				.attribute( "min", 5 )
				.build();

		Size size2 = Annotations.builder( Size.class )
				.attribute( "min", 10 )
				.build();

		assertThat( size1 ).isNotEqualTo( size2 );
	}

	@Test
	public void hashCodeFollowsAnnotationContract() {
		Size size1 = Annotations.builder( Size.class )
				.attribute( "min", 5 )
				.attribute( "max", 10 )
				.build();

		Size size2 = Annotations.builder( Size.class )
				.attribute( "min", 5 )
				.attribute( "max", 10 )
				.build();

		assertThat( size1.hashCode() ).isEqualTo( size2.hashCode() );
	}

	@Test
	public void toStringProducesReadableOutput() {
		Size size = Annotations.builder( Size.class )
				.attribute( "min", 1 )
				.attribute( "max", 10 )
				.build();

		String result = size.toString();
		assertThat( result ).startsWith( "@Size(" );
		assertThat( result ).contains( "min=" );
		assertThat( result ).contains( "max=" );
	}

	@Test
	public void instanceOfAnnotationType() {
		NotNull notNull = Annotations.of( NotNull.class );

		assertThat( notNull ).isInstanceOf( NotNull.class );
		assertThat( notNull ).isInstanceOf( Annotation.class );
	}

	@Test
	public void unknownAttributeThrowsException() {
		assertThatThrownBy( () -> Annotations.builder( NotNull.class )
				.attribute( "nonExistent", "value" )
				.build()
		).isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "nonExistent" );
	}

	private interface TestGroup {
	}

	private static class TestPayload implements Payload {
	}
}
