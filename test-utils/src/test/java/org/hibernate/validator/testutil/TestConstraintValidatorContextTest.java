/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testutil;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

import org.testng.annotations.Test;

/**
 * Tests for {@link TestConstraintValidatorContext} and its standalone implementation.
 */
public class TestConstraintValidatorContextTest {

	@Test
	public void defaultViolationDisabledTracking() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		assertFalse( context.isDefaultViolationDisabled() );

		context.disableDefaultConstraintViolation();

		assertTrue( context.isDefaultViolationDisabled() );
	}

	@Test
	public void defaultMessageTemplate() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone( "my.template" );

		assertEquals( context.getDefaultConstraintMessageTemplate(), "my.template" );
	}

	@Test
	public void buildViolationWithTemplate() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		context.buildConstraintViolationWithTemplate( "must not be null" )
				.addConstraintViolation();

		List<TestConstraintViolation> violations = context.getTestViolations();
		assertEquals( violations.size(), 1 );
		assertEquals( violations.get( 0 ).getMessageTemplate(), "must not be null" );
		assertEquals( violations.get( 0 ).getPropertyPath(), "" );
	}

	@Test
	public void buildViolationWithSinglePropertyNode() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		context.buildConstraintViolationWithTemplate( "{error}" )
				.addPropertyNode( "name" )
				.addConstraintViolation();

		List<TestConstraintViolation> violations = context.getTestViolations();
		assertEquals( violations.size(), 1 );
		assertEquals( violations.get( 0 ).getPropertyPath(), "name" );
	}

	@Test
	public void buildViolationWithNestedPropertyNodes() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		context.buildConstraintViolationWithTemplate( "{error}" )
				.addPropertyNode( "address" )
				.addPropertyNode( "city" )
				.addConstraintViolation();

		List<TestConstraintViolation> violations = context.getTestViolations();
		assertEquals( violations.size(), 1 );
		assertEquals( violations.get( 0 ).getPropertyPath(), "address.city" );
	}

	@Test
	public void multipleViolations() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		context.buildConstraintViolationWithTemplate( "error1" )
				.addPropertyNode( "field1" )
				.addConstraintViolation();

		context.buildConstraintViolationWithTemplate( "error2" )
				.addPropertyNode( "field2" )
				.addConstraintViolation();

		context.buildConstraintViolationWithTemplate( "error3" )
				.addPropertyNode( "nested" )
				.addPropertyNode( "field3" )
				.addConstraintViolation();

		List<TestConstraintViolation> violations = context.getTestViolations();
		assertEquals( violations.size(), 3 );
		assertEquals( violations.get( 0 ).getMessageTemplate(), "error1" );
		assertEquals( violations.get( 0 ).getPropertyPath(), "field1" );
		assertEquals( violations.get( 1 ).getMessageTemplate(), "error2" );
		assertEquals( violations.get( 1 ).getPropertyPath(), "field2" );
		assertEquals( violations.get( 2 ).getMessageTemplate(), "error3" );
		assertEquals( violations.get( 2 ).getPropertyPath(), "nested.field3" );
	}

	@Test
	public void clockProviderDefault() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		assertNotNull( context.getClockProvider() );
	}

	@Test
	public void clockProviderCustom() {
		java.time.Clock fixed = java.time.Clock.systemUTC();
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone(
				"{msg}", () -> fixed );

		assertEquals( context.getClockProvider().getClock(), fixed );
	}

	@Test
	public void unwrapToSelf() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		TestConstraintValidatorContext unwrapped = context.unwrap( TestConstraintValidatorContext.class );
		assertEquals( unwrapped, context );

		ConstraintValidatorContext asBase = context.unwrap( ConstraintValidatorContext.class );
		assertEquals( asBase, context );
	}

	@Test(expectedExceptions = ValidationException.class)
	public void unwrapToUnsupportedType() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();
		context.unwrap( String.class );
	}

	@Test
	public void addBeanNode() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		context.buildConstraintViolationWithTemplate( "{error}" )
				.addBeanNode()
				.addConstraintViolation();

		List<TestConstraintViolation> violations = context.getTestViolations();
		assertEquals( violations.size(), 1 );
		assertEquals( violations.get( 0 ).getPropertyPath(), "" );
	}

	@Test
	public void addContainerElementNode() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		context.buildConstraintViolationWithTemplate( "{error}" )
				.addContainerElementNode( "element", List.class, 0 )
				.addConstraintViolation();

		List<TestConstraintViolation> violations = context.getTestViolations();
		assertEquals( violations.size(), 1 );
		assertEquals( violations.get( 0 ).getPropertyPath(), "element" );
	}

	@Test
	public void addParameterNode() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		context.buildConstraintViolationWithTemplate( "{error}" )
				.addParameterNode( 0 )
				.addConstraintViolation();

		List<TestConstraintViolation> violations = context.getTestViolations();
		assertEquals( violations.size(), 1 );
		assertEquals( violations.get( 0 ).getPropertyPath(), "<parameter 0>" );
	}

	// AssertJ assertion tests

	@Test
	public void assertJHasDefaultViolationDisabled() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();
		context.disableDefaultConstraintViolation();

		TestConstraintValidatorContextAssert.assertThat( context )
				.hasDefaultViolationDisabled();
	}

	@Test
	public void assertJHasDefaultViolationEnabled() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		TestConstraintValidatorContextAssert.assertThat( context )
				.hasDefaultViolationEnabled();
	}

	@Test
	public void assertJHasViolations() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();
		context.buildConstraintViolationWithTemplate( "e1" ).addConstraintViolation();
		context.buildConstraintViolationWithTemplate( "e2" ).addConstraintViolation();

		TestConstraintValidatorContextAssert.assertThat( context )
				.hasViolations( 2 );
	}

	@Test
	public void assertJHasNoViolations() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();

		TestConstraintValidatorContextAssert.assertThat( context )
				.hasNoViolations();
	}

	@Test
	public void assertJViolationDetails() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();
		context.buildConstraintViolationWithTemplate( "{custom.error}" )
				.addPropertyNode( "address" )
				.addPropertyNode( "zip" )
				.addConstraintViolation();

		TestConstraintValidatorContextAssert.assertThat( context )
				.hasViolations( 1 )
				.violation( 0, v -> v
						.hasMessageTemplate( "{custom.error}" )
						.hasPropertyPath( "address.zip" ) );
	}

	@Test
	public void assertJChainedAssertions() {
		TestConstraintValidatorContext context = TestConstraintValidatorContext.standalone();
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate( "a" )
				.addPropertyNode( "x" )
				.addConstraintViolation();
		context.buildConstraintViolationWithTemplate( "b" )
				.addPropertyNode( "y" )
				.addPropertyNode( "z" )
				.addConstraintViolation();

		TestConstraintValidatorContextAssert.assertThat( context )
				.hasDefaultViolationDisabled()
				.hasViolations( 2 )
				.violation( 0, v -> v
						.hasMessageTemplate( "a" )
						.hasPropertyPath( "x" ) )
				.violation( 1, v -> v
						.hasMessageTemplate( "b" )
						.hasPropertyPath( "y.z" ) );
	}
}
