/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.validator.testutil;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.validation.Payload;

/**
 * Utility class for easily creating constraint annotation instances in tests,
 * replacing the internal {@code ConstraintAnnotationDescriptor.Builder} boilerplate.
 * <p>
 * Two entry points are provided:
 * <ul>
 *   <li>{@link #of(Class)} - returns an annotation instance with all default values</li>
 *   <li>{@link #builder(Class)} - returns a builder for setting attributes before creating the instance</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * // Simple: annotation with all defaults
 * NotNull notNull = Annotations.of( NotNull.class );
 *
 * // Builder: annotation with custom attributes
 * Size size = Annotations.builder( Size.class )
 *         .attribute( "min", 1 )
 *         .attribute( "max", 10 )
 *         .build();
 * </pre>
 *
 * @author Hibernate Authors
 */
public final class Annotations {

	private Annotations() {
	}

	/**
	 * Creates an annotation instance of the given type with all default values.
	 *
	 * @param annotationType the annotation class
	 * @param <A> the annotation type
	 * @return an annotation instance with all default values
	 * @throws IllegalArgumentException if any annotation attribute has no default value
	 */
	public static <A extends Annotation> A of(Class<A> annotationType) {
		return builder( annotationType ).build();
	}

	/**
	 * Creates a builder for constructing an annotation instance of the given type.
	 *
	 * @param annotationType the annotation class
	 * @param <A> the annotation type
	 * @return a new builder
	 */
	public static <A extends Annotation> Builder<A> builder(Class<A> annotationType) {
		return new Builder<>( annotationType );
	}

	/**
	 * Builder for constructing annotation instances with custom attribute values.
	 *
	 * @param <A> the annotation type
	 */
	public static final class Builder<A extends Annotation> {

		private final Class<A> annotationType;
		private final Map<String, Object> attributes;

		private Builder(Class<A> annotationType) {
			this.annotationType = annotationType;
			this.attributes = new HashMap<>();
		}

		/**
		 * Sets the {@code message} attribute. Every constraint annotation has this attribute.
		 *
		 * @param message the constraint violation message
		 * @return this builder
		 */
		public Builder<A> message(String message) {
			attributes.put( "message", message );
			return this;
		}

		/**
		 * Sets the {@code groups} attribute. Every constraint annotation has this attribute.
		 *
		 * @param groups the validation groups
		 * @return this builder
		 */
		public Builder<A> groups(Class<?>... groups) {
			attributes.put( "groups", groups );
			return this;
		}

		/**
		 * Sets the {@code payload} attribute. Every constraint annotation has this attribute.
		 *
		 * @param payload the payload classes
		 * @return this builder
		 */
		@SuppressWarnings("unchecked")
		public Builder<A> payload(Class<? extends Payload>... payload) {
			attributes.put( "payload", payload );
			return this;
		}

		/**
		 * Sets a constraint-specific attribute by name.
		 *
		 * @param name the attribute name
		 * @param value the attribute value
		 * @return this builder
		 */
		public Builder<A> attribute(String name, Object value) {
			attributes.put( name, value );
			return this;
		}

		/**
		 * Builds the annotation instance, merging explicitly set values with defaults
		 * from the annotation interface.
		 *
		 * @return the annotation instance
		 * @throws IllegalArgumentException if an attribute has no value set and no default value,
		 *         or if an unknown attribute name was provided
		 */
		public A build() {
			Map<String, Object> resolvedAttributes = resolveAttributes();
			return createProxy( annotationType, resolvedAttributes );
		}

		private Map<String, Object> resolveAttributes() {
			Map<String, Object> resolved = new HashMap<>();
			int processedFromExplicit = 0;
			Method[] methods = annotationType.getDeclaredMethods();

			for ( Method method : methods ) {
				String name = method.getName();
				Object explicitValue = attributes.get( name );

				if ( explicitValue != null ) {
					resolved.put( name, explicitValue );
					processedFromExplicit++;
				}
				else if ( method.getDefaultValue() != null ) {
					resolved.put( name, method.getDefaultValue() );
				}
				else {
					throw new IllegalArgumentException(
							"No value provided for attribute '" + name
									+ "' of annotation @" + annotationType.getSimpleName()
									+ " and no default value exists"
					);
				}
			}

			if ( processedFromExplicit != attributes.size() ) {
				Map<String, Object> unknown = new HashMap<>( attributes );
				unknown.keySet().removeAll( resolved.keySet() );
				throw new IllegalArgumentException(
						"Unknown attribute(s) " + unknown.keySet()
								+ " for annotation @" + annotationType.getSimpleName()
				);
			}

			return resolved;
		}
	}

	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A createProxy(Class<A> annotationType, Map<String, Object> attributes) {
		InvocationHandler handler = new AnnotationInvocationHandler<>( annotationType, attributes );
		return (A) Proxy.newProxyInstance(
				annotationType.getClassLoader(),
				new Class<?>[] { annotationType },
				handler
		);
	}

	/**
	 * {@link InvocationHandler} that backs the annotation proxy instances.
	 * Implements the {@link Annotation} contract for {@code equals()}, {@code hashCode()},
	 * {@code toString()}, and {@code annotationType()}.
	 */
	private static final class AnnotationInvocationHandler<A extends Annotation> implements InvocationHandler {

		private final Class<A> annotationType;
		private final Map<String, Object> attributes;

		AnnotationInvocationHandler(Class<A> annotationType, Map<String, Object> attributes) {
			this.annotationType = annotationType;
			this.attributes = attributes;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();

			// Handle Annotation interface methods
			if ( "annotationType".equals( methodName ) && method.getParameterCount() == 0 ) {
				return annotationType;
			}

			if ( "equals".equals( methodName ) && method.getParameterCount() == 1 ) {
				return annotationEquals( args[0] );
			}

			if ( "hashCode".equals( methodName ) && method.getParameterCount() == 0 ) {
				return annotationHashCode();
			}

			if ( "toString".equals( methodName ) && method.getParameterCount() == 0 ) {
				return annotationToString();
			}

			// Handle annotation attribute methods
			Object value = attributes.get( methodName );
			if ( value != null ) {
				return value;
			}

			throw new IllegalStateException(
					"No value for annotation attribute '" + methodName
							+ "' on @" + annotationType.getSimpleName()
			);
		}

		/**
		 * Implements the {@link Annotation#equals(Object)} contract.
		 * <p>
		 * From the JDK javadoc: Returns true if the specified object represents an annotation
		 * that is logically equivalent to this one. More specifically, returns true if the
		 * specified object is an instance of the same annotation interface and if, for each
		 * member of this annotation, the corresponding member of the specified annotation is
		 * equal.
		 */
		private boolean annotationEquals(Object other) {
			if ( !annotationType.isInstance( other ) ) {
				return false;
			}

			Annotation otherAnnotation = (Annotation) other;

			Method[] methods = annotationType.getDeclaredMethods();
			for ( Method method : methods ) {
				String name = method.getName();
				Object thisValue = attributes.get( name );
				Object otherValue;
				try {
					otherValue = method.invoke( otherAnnotation );
				}
				catch (Exception e) {
					return false;
				}
				if ( !memberEquals( thisValue, otherValue ) ) {
					return false;
				}
			}

			return true;
		}

		/**
		 * Implements the {@link Annotation#hashCode()} contract.
		 * <p>
		 * From the JDK javadoc: The hash code of an annotation is the sum of the hash codes
		 * of its members (including those with default values). The hash code of an
		 * annotation member is (127 times the hash code of the member-name XORed with the
		 * hash code of the member-value).
		 */
		private int annotationHashCode() {
			int result = 0;
			for ( Map.Entry<String, Object> entry : attributes.entrySet() ) {
				int nameHashCode = entry.getKey().hashCode();
				int valueHashCode = memberValueHashCode( entry.getValue() );
				result += 127 * nameHashCode ^ valueHashCode;
			}
			return result;
		}

		/**
		 * Produces a readable string representation like {@code @NotNull(message=must not be null)}.
		 */
		private String annotationToString() {
			StringBuilder sb = new StringBuilder();
			sb.append( '@' ).append( annotationType.getSimpleName() ).append( '(' );

			SortedSet<String> sortedKeys = new TreeSet<>( attributes.keySet() );
			boolean first = true;
			for ( String key : sortedKeys ) {
				if ( !first ) {
					sb.append( ", " );
				}
				sb.append( key ).append( '=' );
				Object value = attributes.get( key );
				appendValue( sb, value );
				first = false;
			}

			sb.append( ')' );
			return sb.toString();
		}

		private void appendValue(StringBuilder sb, Object value) {
			if ( value instanceof Object[] ) {
				sb.append( Arrays.toString( (Object[]) value ) );
			}
			else if ( value instanceof boolean[] ) {
				sb.append( Arrays.toString( (boolean[]) value ) );
			}
			else if ( value instanceof byte[] ) {
				sb.append( Arrays.toString( (byte[]) value ) );
			}
			else if ( value instanceof char[] ) {
				sb.append( Arrays.toString( (char[]) value ) );
			}
			else if ( value instanceof double[] ) {
				sb.append( Arrays.toString( (double[]) value ) );
			}
			else if ( value instanceof float[] ) {
				sb.append( Arrays.toString( (float[]) value ) );
			}
			else if ( value instanceof int[] ) {
				sb.append( Arrays.toString( (int[]) value ) );
			}
			else if ( value instanceof long[] ) {
				sb.append( Arrays.toString( (long[]) value ) );
			}
			else if ( value instanceof short[] ) {
				sb.append( Arrays.toString( (short[]) value ) );
			}
			else {
				sb.append( value );
			}
		}

		/**
		 * Compares two annotation member values, handling array types per the
		 * {@link Annotation#equals(Object)} contract.
		 */
		private static boolean memberEquals(Object o1, Object o2) {
			if ( o1 == o2 ) {
				return true;
			}
			if ( o1 == null || o2 == null ) {
				return false;
			}

			if ( o1.getClass().isArray() ) {
				if ( o1 instanceof Object[] && o2 instanceof Object[] ) {
					return Arrays.equals( (Object[]) o1, (Object[]) o2 );
				}
				if ( o1 instanceof boolean[] && o2 instanceof boolean[] ) {
					return Arrays.equals( (boolean[]) o1, (boolean[]) o2 );
				}
				if ( o1 instanceof byte[] && o2 instanceof byte[] ) {
					return Arrays.equals( (byte[]) o1, (byte[]) o2 );
				}
				if ( o1 instanceof char[] && o2 instanceof char[] ) {
					return Arrays.equals( (char[]) o1, (char[]) o2 );
				}
				if ( o1 instanceof double[] && o2 instanceof double[] ) {
					return Arrays.equals( (double[]) o1, (double[]) o2 );
				}
				if ( o1 instanceof float[] && o2 instanceof float[] ) {
					return Arrays.equals( (float[]) o1, (float[]) o2 );
				}
				if ( o1 instanceof int[] && o2 instanceof int[] ) {
					return Arrays.equals( (int[]) o1, (int[]) o2 );
				}
				if ( o1 instanceof long[] && o2 instanceof long[] ) {
					return Arrays.equals( (long[]) o1, (long[]) o2 );
				}
				if ( o1 instanceof short[] && o2 instanceof short[] ) {
					return Arrays.equals( (short[]) o1, (short[]) o2 );
				}
				return false;
			}

			return o1.equals( o2 );
		}

		/**
		 * Computes the hash code of a member value per the {@link Annotation#hashCode()} contract.
		 */
		private static int memberValueHashCode(Object value) {
			if ( value instanceof Object[] ) {
				return Arrays.hashCode( (Object[]) value );
			}
			if ( value instanceof boolean[] ) {
				return Arrays.hashCode( (boolean[]) value );
			}
			if ( value instanceof byte[] ) {
				return Arrays.hashCode( (byte[]) value );
			}
			if ( value instanceof char[] ) {
				return Arrays.hashCode( (char[]) value );
			}
			if ( value instanceof double[] ) {
				return Arrays.hashCode( (double[]) value );
			}
			if ( value instanceof float[] ) {
				return Arrays.hashCode( (float[]) value );
			}
			if ( value instanceof int[] ) {
				return Arrays.hashCode( (int[]) value );
			}
			if ( value instanceof long[] ) {
				return Arrays.hashCode( (long[]) value );
			}
			if ( value instanceof short[] ) {
				return Arrays.hashCode( (short[]) value );
			}
			return value.hashCode();
		}
	}
}
