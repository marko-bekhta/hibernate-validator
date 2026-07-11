/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.test.internal.engine.typeannotationconstraint;

import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertNoViolations;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertThat;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.pathWith;
import static org.hibernate.validator.testutil.ConstraintViolationAssert.violationOf;
import static org.hibernate.validator.testutils.ValidatorUtil.getConfiguration;
import static org.hibernate.validator.testutils.ValidatorUtil.getValidator;

import java.util.Set;

import jakarta.validation.ConstraintDeclarationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.cfg.ArrayConstraintBehavior;
import org.hibernate.validator.engine.ArrayValidationTarget;
import org.hibernate.validator.internal.engine.path.MutableNode;
import org.hibernate.validator.testutil.DummyTraversableResolver;
import org.hibernate.validator.testutil.TestForIssue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for the {@link ArrayConstraintBehavior} configuration and {@link ArrayValidationTarget}
 * payload overrides for array container element validation.
 * <p>
 * Covers JLS vs LEGACY global behavior, per-constraint payload overrides, between-brackets
 * annotation syntax, mixed annotation positions, primitive arrays, error conditions,
 * property-based configuration, and cascading with {@code @Valid}.
 */
@TestForIssue(jiraKey = "HV-1428")
public class ArrayConstraintBehaviorTest {

	private Validator validator;
	private Validator legacyValidator;

	@BeforeClass
	public void setup() {
		validator = getValidator();
		legacyValidator = getConfiguration()
				.traversableResolver( new DummyTraversableResolver() )
				.arrayConstraintBehavior( ArrayConstraintBehavior.LEGACY )
				.buildValidatorFactory()
				.getValidator();
	}

	// --- JLS default behavior ---

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void jls_default_validates_each_array_element() {
		NotBlankArray a = new NotBlankArray();
		a.names = new String[] { "ok", "", null };

		Set<ConstraintViolation<NotBlankArray>> constraintViolations = validator.validate( a );

		assertThat( constraintViolations ).containsOnlyViolations(
				violationOf( NotBlank.class )
						.withPropertyPath( pathWith()
								.property( "names" )
								.containerElement( MutableNode.ITERABLE_ELEMENT_NODE_NAME, true, null, 1, Object[].class, null )
						),
				violationOf( NotBlank.class )
						.withPropertyPath( pathWith()
								.property( "names" )
								.containerElement( MutableNode.ITERABLE_ELEMENT_NODE_NAME, true, null, 2, Object[].class, null )
						)
		);
	}

	// --- LEGACY mode behavior ---

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void legacy_mode_validates_array_itself() {
		SizeOnArray a = new SizeOnArray();
		a.names = new String[] { "ab", "cd", "ef" };

		Set<ConstraintViolation<SizeOnArray>> constraintViolations = legacyValidator.validate( a );

		// In LEGACY mode, @Size(min=5) validates the array length (3 < 5)
		assertThat( constraintViolations ).containsOnlyViolations(
				violationOf( Size.class ).withProperty( "names" )
		);
	}

	// --- Payload overrides ---

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void element_payload_overrides_legacy_mode() {
		SizeElementPayload a = new SizeElementPayload();
		a.names = new String[] { "hello", "hi" };

		Set<ConstraintViolation<SizeElementPayload>> constraintViolations = legacyValidator.validate( a );

		// Element payload forces per-element validation despite LEGACY config:
		// "hello" (length 5) passes min=5, "hi" (length 2) fails min=5
		assertThat( constraintViolations ).containsOnlyViolations(
				violationOf( Size.class )
						.withPropertyPath( pathWith()
								.property( "names" )
								.containerElement( MutableNode.ITERABLE_ELEMENT_NODE_NAME, true, null, 1, Object[].class, null )
						)
		);
	}

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void array_payload_overrides_jls_mode() {
		SizeArrayPayload a = new SizeArrayPayload();
		a.names = new String[] { "a", "b", "c" };

		Set<ConstraintViolation<SizeArrayPayload>> constraintViolations = validator.validate( a );

		// Array payload forces array-level validation despite JLS default:
		// array length 3 < min 5
		assertThat( constraintViolations ).containsOnlyViolations(
				violationOf( Size.class ).withProperty( "names" )
		);
	}

	// --- Between-brackets annotation syntax ---

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void between_brackets_annotation_validates_array() {
		BetweenBracketsSize a = new BetweenBracketsSize();
		a.names = new String[] { "only-one" };

		Set<ConstraintViolation<BetweenBracketsSize>> constraintViolations = validator.validate( a );

		// @Size between brackets targets the array type: length 1 < min 2
		assertThat( constraintViolations ).containsOnlyViolations(
				violationOf( Size.class ).withProperty( "names" )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void mixed_positions_validate_elements_and_array_separately() {
		MixedPositions a = new MixedPositions();
		a.names = new String[] { "", "ok" };

		Set<ConstraintViolation<MixedPositions>> constraintViolations = validator.validate( a );

		// @NotBlank (leftmost) validates elements: index 0 is blank
		// @Size(min=1) (between brackets) validates array length: 2 >= 1, passes
		assertThat( constraintViolations ).containsOnlyViolations(
				violationOf( NotBlank.class )
						.withPropertyPath( pathWith()
								.property( "names" )
								.containerElement( MutableNode.ITERABLE_ELEMENT_NODE_NAME, true, null, 0, Object[].class, null )
						)
		);
	}

	// --- Primitive arrays ---

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void primitive_array_validates_each_element() {
		MinIntArray a = new MinIntArray();
		a.values = new int[] { 15, 3 };

		Set<ConstraintViolation<MinIntArray>> constraintViolations = validator.validate( a );

		// @Min(10) validates each int element: 15 passes, 3 fails
		assertThat( constraintViolations ).containsOnlyViolations(
				violationOf( Min.class )
						.withPropertyPath( pathWith()
								.property( "values" )
								.containerElement( MutableNode.ITERABLE_ELEMENT_NODE_NAME, true, null, 1, int[].class, null )
						)
		);
	}

	// --- Error conditions ---

	@Test(expectedExceptions = ConstraintDeclarationException.class, expectedExceptionsMessageRegExp = "HV000275.*")
	@TestForIssue(jiraKey = "HV-1428")
	public void both_element_and_array_payloads_throws_exception() {
		validator.validate( new BothPayloads() );
	}

	@Test(expectedExceptions = ConstraintDeclarationException.class, expectedExceptionsMessageRegExp = "HV000276.*")
	@TestForIssue(jiraKey = "HV-1428")
	public void payload_on_non_array_field_throws_exception() {
		validator.validate( new PayloadOnNonArray() );
	}

	// --- Property-based configuration ---

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void property_based_legacy_configuration() {
		Validator propertyValidator = getConfiguration()
				.traversableResolver( new DummyTraversableResolver() )
				.addProperty( "hibernate.validator.array_constraint_behavior", "legacy" )
				.buildValidatorFactory()
				.getValidator();

		SizeOnArray a = new SizeOnArray();
		a.names = new String[] { "ab", "cd", "ef" };

		Set<ConstraintViolation<SizeOnArray>> constraintViolations = propertyValidator.validate( a );

		// Property-based LEGACY mode: @Size(min=5) validates array length (3 < 5)
		assertThat( constraintViolations ).containsOnlyViolations(
				violationOf( Size.class ).withProperty( "names" )
		);
	}

	// --- Cascading with @Valid ---

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void cascading_with_valid_validates_nested_bean_constraints() {
		ValidBarArray a = new ValidBarArray();
		a.bars = new CascadeBar[] { new CascadeBar( 15 ), new CascadeBar( 3 ) };

		Set<ConstraintViolation<ValidBarArray>> constraintViolations = validator.validate( a );

		assertThat( constraintViolations ).containsOnlyViolations(
				violationOf( Min.class )
						.withPropertyPath( pathWith()
								.property( "bars" )
								.property( "value", true, null, 1, Object[].class, null )
						)
		);
	}

	// --- Model classes ---

	static class NotBlankArray {
		@NotBlank
		String[] names;
	}

	static class SizeOnArray {
		@Size(min = 5)
		String[] names;
	}

	static class SizeElementPayload {
		@Size(min = 5, payload = ArrayValidationTarget.Element.class)
		String[] names;
	}

	static class SizeArrayPayload {
		@Size(min = 5, payload = ArrayValidationTarget.Array.class)
		String[] names;
	}

	static class BetweenBracketsSize {
		String @Size(min = 2) [] names;
	}

	static class MixedPositions {
		@NotBlank
		String @Size(min = 1) [] names;
	}

	static class MinIntArray {
		@Min(10)
		int[] values;
	}

	static class BothPayloads {
		@Size(min = 2, payload = { ArrayValidationTarget.Element.class, ArrayValidationTarget.Array.class })
		String[] names;
	}

	static class PayloadOnNonArray {
		@Size(min = 2, payload = ArrayValidationTarget.Element.class)
		String name;
	}

	static class ValidBarArray {
		@Valid
		CascadeBar[] bars;
	}

	static class CascadeBar {
		@Min(10)
		int value;

		CascadeBar(int value) {
			this.value = value;
		}
	}

	// Multi-dimensional model classes

	static class TwoDimLeafValidation {
		@NotBlank
		String[][] matrix;
	}

	static class TwoDimAllLevels {
		@NotBlank
		String @Size(min = 3) [] @Size(min = 2) [] matrix;
	}

	static class TwoDimInnerBrackets {
		String[] @Size(min = 2) [] matrix;
	}

	// --- Multi-dimensional tests ---

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void two_dimensional_array_validates_leaf_elements() {
		TwoDimLeafValidation bean = new TwoDimLeafValidation();
		bean.matrix = new String[][] { { "ok", "" }, { null, "fine" } };

		Set<ConstraintViolation<TwoDimLeafValidation>> violations = validator.validate( bean );

		assertThat( violations ).containsOnlyViolations(
				violationOf( NotBlank.class ),
				violationOf( NotBlank.class )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void two_dimensional_array_validates_all_levels() {
		TwoDimAllLevels bean = new TwoDimAllLevels();
		// Outer array has 2 elements (violates @Size(min=3) on outer array)
		// Inner arrays have 1 element each (violates @Size(min=2) on inner arrays)
		// Leaf "" violates @NotBlank
		bean.matrix = new String[][] { { "" }, { "ok" } };

		Set<ConstraintViolation<TwoDimAllLevels>> violations = validator.validate( bean );

		// Outer @Size(min=3) → 1 violation on the array itself
		// Inner @Size(min=2) → 2 violations (one per inner array)
		// @NotBlank → 1 violation on leaf element ""
		assertThat( violations ).containsOnlyViolations(
				violationOf( Size.class ).withPropertyPath( pathWith().property( "matrix" ) ),
				violationOf( Size.class ).withPropertyPath( pathWith()
						.property( "matrix" )
						.containerElement( MutableNode.ITERABLE_ELEMENT_NODE_NAME, true, null, 0, Object[].class, null ) ),
				violationOf( Size.class ).withPropertyPath( pathWith()
						.property( "matrix" )
						.containerElement( MutableNode.ITERABLE_ELEMENT_NODE_NAME, true, null, 1, Object[].class, null ) ),
				violationOf( NotBlank.class ).withPropertyPath( pathWith()
						.property( "matrix" )
						.containerElement( MutableNode.ITERABLE_ELEMENT_NODE_NAME, true, null, 0, Object[].class, null )
						.containerElement( MutableNode.ITERABLE_ELEMENT_NODE_NAME, true, null, 0, Object[].class, null ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void two_dimensional_array_validates_all_levels_no_violations_when_valid() {
		TwoDimAllLevels bean = new TwoDimAllLevels();
		bean.matrix = new String[][] { { "a", "b" }, { "c", "d" }, { "e", "f" } };

		Set<ConstraintViolation<TwoDimAllLevels>> violations = validator.validate( bean );

		assertNoViolations( violations );
	}

	@Test
	@TestForIssue(jiraKey = "HV-1428")
	public void two_dimensional_inner_brackets_validates_inner_array_length() {
		TwoDimInnerBrackets bean = new TwoDimInnerBrackets();
		// Inner arrays: {a} has 1 element (violates min=2), {b,c} has 2 (ok)
		bean.matrix = new String[][] { { "a" }, { "b", "c" } };

		Set<ConstraintViolation<TwoDimInnerBrackets>> violations = validator.validate( bean );

		assertThat( violations ).containsOnlyViolations(
				violationOf( Size.class )
		);
	}
}
