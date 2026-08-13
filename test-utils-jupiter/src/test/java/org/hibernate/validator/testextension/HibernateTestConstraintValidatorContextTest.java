/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testextension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.testutil.TestConstraintValidatorContext;
import org.hibernate.validator.testutil.TestConstraintViolation;

import org.junit.jupiter.api.Test;

@ValidatorTest
class HibernateTestConstraintValidatorContextTest {

	@Test
	void messageParametersAreRecorded() {
		HibernateTestConstraintValidatorContext context = new HibernateTestConstraintValidatorContext();

		context.addMessageParameter( "foo", "bar" );
		context.addMessageParameter( "count", 42 );

		Map<String, Object> params = context.getMessageParameters();
		assertThat( params ).hasSize( 2 );
		assertThat( params ).containsEntry( "foo", "bar" );
		assertThat( params ).containsEntry( "count", 42 );
	}

	@Test
	void expressionVariablesAreRecorded() {
		HibernateTestConstraintValidatorContext context = new HibernateTestConstraintValidatorContext();

		context.addExpressionVariable( "min", 5 );
		context.addExpressionVariable( "max", 100 );

		Map<String, Object> vars = context.getExpressionVariables();
		assertThat( vars ).hasSize( 2 );
		assertThat( vars ).containsEntry( "min", 5 );
		assertThat( vars ).containsEntry( "max", 100 );
	}

	@Test
	void dynamicPayloadIsRecorded() {
		HibernateTestConstraintValidatorContext context = new HibernateTestConstraintValidatorContext();

		Object payload = "my-payload";
		context.withDynamicPayload( payload );

		assertEquals( payload, context.getDynamicPayload() );
	}

	@Test
	void violationsCaptureMessageParameters() {
		HibernateTestConstraintValidatorContext context = new HibernateTestConstraintValidatorContext();

		context.addMessageParameter( "name", "value1" );
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate( "{custom.message}" )
				.addConstraintViolation();

		assertThat( context.getTestViolations() ).hasSize( 1 );
		TestConstraintViolation violation = context.getTestViolations().get( 0 );
		assertTrue( violation instanceof HibernateTestConstraintViolation );

		HibernateTestConstraintViolation hvViolation = (HibernateTestConstraintViolation) violation;
		assertThat( hvViolation.getMessageParameters() ).containsEntry( "name", "value1" );
		assertEquals( "{custom.message}", hvViolation.getMessageTemplate() );
	}

	@Test
	void unwrapWorksForHibernateConstraintValidatorContext() {
		HibernateTestConstraintValidatorContext context = new HibernateTestConstraintValidatorContext();

		HibernateConstraintValidatorContext hvContext = context.unwrap( HibernateConstraintValidatorContext.class );
		assertNotNull( hvContext );

		TestConstraintValidatorContext testContext = context.unwrap( TestConstraintValidatorContext.class );
		assertNotNull( testContext );

		HibernateTestConstraintValidatorContext directContext =
				context.unwrap( HibernateTestConstraintValidatorContext.class );
		assertNotNull( directContext );
	}

	@Test
	void assertJAssertionsWork() {
		HibernateTestConstraintValidatorContext context = new HibernateTestConstraintValidatorContext();

		context.addMessageParameter( "key", "val" );
		context.addExpressionVariable( "expr", 99 );
		context.withDynamicPayload( "payload" );
		context.disableDefaultConstraintViolation();

		context.buildConstraintViolationWithTemplate( "{msg}" )
				.addPropertyNode( "field" )
				.addConstraintViolation();

		HibernateTestConstraintValidatorContextAssert.assertThat( context )
				.hasMessageParameter( "key", "val" )
				.hasExpressionVariable( "expr", 99 )
				.hasDynamicPayload( "payload" )
				.hasDefaultViolationDisabled()
				.hasViolations( 1 )
				.hibernateViolation( 0, v -> v
						.hasMessageTemplate( "{msg}" )
						.hasPropertyPath( "field" )
						.hasMessageParameter( "key", "val" )
						.hasExpressionVariable( "expr", 99 )
				);
	}

	@Test
	void injectedAsParameter(ConstraintValidatorContext context) {
		assertNotNull( context );
		assertTrue( context instanceof HibernateTestConstraintValidatorContext,
				"Injected context should be HibernateTestConstraintValidatorContext" );
	}

	@Test
	void injectedAsHibernateContext(HibernateConstraintValidatorContext context) {
		assertNotNull( context );
		assertTrue( context instanceof HibernateTestConstraintValidatorContext,
				"Injected HibernateConstraintValidatorContext should be HibernateTestConstraintValidatorContext" );
	}

	@Test
	void injectedAsHibernateTestContext(HibernateTestConstraintValidatorContext context) {
		assertNotNull( context );
		assertNotNull( context.getMessageParameters() );
	}

	@Test
	void constraintValidatorPayloadReturnsNullByDefault() {
		HibernateTestConstraintValidatorContext context = new HibernateTestConstraintValidatorContext();

		assertNull( context.getConstraintValidatorPayload( String.class ) );
	}

	@Test
	void constraintValidatorPayloadReturnsConfiguredValue() {
		HibernateTestConstraintValidatorContext context = new HibernateTestConstraintValidatorContext();
		context.setConstraintValidatorPayload( "configured" );

		assertEquals( "configured", context.getConstraintValidatorPayload( String.class ) );
		assertNull( context.getConstraintValidatorPayload( Integer.class ) );
	}

	@Test
	void violationsCaptureExpressionVariables() {
		HibernateTestConstraintValidatorContext context = new HibernateTestConstraintValidatorContext();

		context.addExpressionVariable( "var1", "hello" );
		context.buildConstraintViolationWithTemplate( "${var1}" )
				.addConstraintViolation();

		HibernateTestConstraintViolation violation =
				(HibernateTestConstraintViolation) context.getTestViolations().get( 0 );
		assertThat( violation.getExpressionVariables() ).containsEntry( "var1", "hello" );
	}
}
