/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.test.internal.metadata.bb;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.hibernate.validator.engine.HibernateValidatorEnhancedBean;
import org.hibernate.validator.internal.util.StringHelper;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveBoxingDelegate;
import net.bytebuddy.implementation.bytecode.assign.reference.ReferenceTypeAwareAssigner;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.matcher.ElementMatchers;
import org.testng.annotations.Test;

/**
 * @author Marko Bekhta
 */
public class ByteBuddyWrapperTest {


	@Test
	public void testByteBuddy() throws Throwable {

		ClassLoader classLoader = new ByteArrayClassLoader(
				ClassLoadingStrategy.BOOTSTRAP_LOADER,
				ClassFileLocator.ForClassLoader.readToNames( Foo.class, GetFieldValue.class, HibernateValidatorEnhancedBean.class,
						MyContracts.class, StringHelper.class ) );

		Class<?> aClass = new ByteBuddy().rebase( Foo.class )
				.implement( HibernateValidatorEnhancedBean.class )
				.method(
						named( "getFieldValue" )
								.and( ElementMatchers.takesArguments( String.class ) )
								.and( ElementMatchers.returns( Object.class ) )
				)
				.intercept( new Implementation.Simple( new GetFieldValue( Foo.class ) ) )
				.method(
						named( "getGetterValue" )
								.and( ElementMatchers.takesArguments( String.class ) )
								.and( ElementMatchers.returns( Object.class ) )
				)
				.intercept( new Implementation.Simple( new GetGetterValue( Foo.class ) ) )
				.make()
				.load( classLoader, ClassLoadingStrategy.Default.INJECTION )
				.getLoaded();

		Object object = aClass.newInstance();

		Method getFieldValue = aClass.getMethod( "getFieldValue", String.class );

		assertThat( getFieldValue.invoke( object, "num" ) ).isEqualTo( -1 );
		assertThat( getFieldValue.invoke( object, "string" ) ).isEqualTo( "test" );
		assertThat( getFieldValue.invoke( object, "looooong" ) ).isEqualTo( 100L );

		Method getGetterValue = aClass.getMethod( "getGetterValue", String.class );

		assertThat( getGetterValue.invoke( object, "getMessage" ) ).isEqualTo( "messssssage" );
		assertThat( getGetterValue.invoke( object, "getKey" ) ).isEqualTo( false );

	}

	public static final class MyContracts {
		public static void assertNotEmpty(String s, String message) {
			if ( StringHelper.isNullOrEmptyString( s ) ) {
				throw new IllegalArgumentException( message );
			}
		}
	}

	private static class GetFieldValue implements ByteCodeAppender {

		private final Class clazz;

		private final Field[] fields;

		public GetFieldValue(Class clazz) {
			this.clazz = clazz;
			this.fields = clazz.getDeclaredFields();
		}

		@Override
		public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
			try {
				// Contracts.assertNotEmpty(propertyName, "Property cannot be blank");
				Label contractsPropertyNameCheckLabel = new Label();
				methodVisitor.visitLabel( contractsPropertyNameCheckLabel );
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
				methodVisitor.visitLdcInsn( "Property cannot be blank" );
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKESTATIC,
						Type.getType( MyContracts.class ).getInternalName(),
						"assertNotEmpty",
						Type.getType( MyContracts.class.getDeclaredMethod( "assertNotEmpty", String.class, String.class ) ).getDescriptor(),
						false
				);

				Label l1 = new Label();
				methodVisitor.visitLabel( l1 );

				int index = 0;
				for ( Field field : fields ) {
					String fieldName = field.getName();

					if ( index > 0 ) {
						methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
					}

					//		if (propertyName.equals(field_name_goes_here)) {
					//			return field;
					//		}
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
					methodVisitor.visitLdcInsn( fieldName );
					methodVisitor.visitMethodInsn(
							Opcodes.INVOKEVIRTUAL,
							Type.getType( String.class ).getInternalName(),
							"equals",
							Type.getType( String.class.getDeclaredMethod( "equals", Object.class ) ).getDescriptor(),
							false
					);

					Label ifCheckLabel = new Label();
					methodVisitor.visitJumpInsn( Opcodes.IFEQ, ifCheckLabel );

					Label returnFieldLabel = new Label();
					methodVisitor.visitLabel( returnFieldLabel );
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
					methodVisitor.visitFieldInsn(
							Opcodes.GETFIELD,
							Type.getInternalName( clazz ),
							fieldName,
							Type.getDescriptor( field.getType() )
					);
					if ( field.getType().isPrimitive() ) {
						PrimitiveBoxingDelegate.forPrimitive( new TypeDescription.ForLoadedType( field.getType() ) )
								.assignBoxedTo(
										TypeDescription.Generic.OBJECT,
										ReferenceTypeAwareAssigner.INSTANCE,
										Assigner.Typing.STATIC
								)
								.apply( methodVisitor, implementationContext );
					}
					methodVisitor.visitInsn( Opcodes.ARETURN );
					methodVisitor.visitLabel( ifCheckLabel );

					index++;
				}

				// throw new IllegalArgumentException("No property was found for a given name");

				methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
				methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( IllegalArgumentException.class ) );
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( "No property was found for a given name" );
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKESPECIAL,
						Type.getInternalName( IllegalArgumentException.class ),
						"<init>",
						Type.getType( IllegalArgumentException.class.getDeclaredConstructor( String.class ) ).getDescriptor(),
						false
				);
				methodVisitor.visitInsn( Opcodes.ATHROW );

				Label label = new Label();
				methodVisitor.visitLabel( label );
				methodVisitor.visitLocalVariable(
						"this",
						Type.getDescriptor( clazz ),
						null,
						contractsPropertyNameCheckLabel,
						label,
						0
				);
				methodVisitor.visitLocalVariable(
						"propertyName",
						Type.getDescriptor( String.class ),
						null,
						contractsPropertyNameCheckLabel,
						label,
						1
				);
				methodVisitor.visitMaxs( 3, 2 );

				return new Size( 6, instrumentedMethod.getStackSize() );
			}
			catch (NoSuchMethodException e) {
				throw new IllegalArgumentException( e );
			}
		}
	}

	private static class GetGetterValue implements ByteCodeAppender {

		private final Class clazz;

		private final Method[] getters;

		public GetGetterValue(Class clazz) {
			this.clazz = clazz;
			this.getters = Arrays.stream( clazz.getDeclaredMethods() )
					.filter( m -> m.getParameterCount() == 0 )
					.toArray( Method[]::new );
		}

		@Override
		public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
			try {
				// Contracts.assertNotEmpty(propertyName, "Property cannot be blank");
				Label contractsPropertyNameCheckLabel = new Label();
				methodVisitor.visitLabel( contractsPropertyNameCheckLabel );
				methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
				methodVisitor.visitLdcInsn( "Property cannot be blank" );
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKESTATIC,
						Type.getType( MyContracts.class ).getInternalName(),
						"assertNotEmpty",
						Type.getType( MyContracts.class.getDeclaredMethod( "assertNotEmpty", String.class, String.class ) ).getDescriptor(),
						false
				);

				Label l1 = new Label();
				methodVisitor.visitLabel( l1 );
				int index = 0;

				// look at getters
				for ( Method getter : getters ) {
					String propertyName = getter.getName();

					if ( index > 0 ) {
						methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
					}

					//		if (propertyName.equals(property_name_goes_here)) {
					//			return getProperty();
					//		}
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 1 );
					methodVisitor.visitLdcInsn( propertyName );
					methodVisitor.visitMethodInsn(
							Opcodes.INVOKEVIRTUAL,
							Type.getType( String.class ).getInternalName(),
							"equals",
							Type.getType( String.class.getDeclaredMethod( "equals", Object.class ) ).getDescriptor(),
							false
					);

					Label ifCheckLabel = new Label();
					methodVisitor.visitJumpInsn( Opcodes.IFEQ, ifCheckLabel );

					Label returnFieldLabel = new Label();
					methodVisitor.visitLabel( returnFieldLabel );
					methodVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
					methodVisitor.visitMethodInsn(
							Opcodes.INVOKEVIRTUAL,
							Type.getInternalName( clazz ),
							getter.getName(),
							Type.getMethodDescriptor( getter ),
							false
					);
					if ( getter.getReturnType().isPrimitive() ) {
						PrimitiveBoxingDelegate.forPrimitive( new TypeDescription.ForLoadedType( getter.getReturnType() ) )
								.assignBoxedTo(
										TypeDescription.Generic.OBJECT,
										ReferenceTypeAwareAssigner.INSTANCE,
										Assigner.Typing.STATIC
								)
								.apply( methodVisitor, implementationContext );
					}

					methodVisitor.visitInsn( Opcodes.ARETURN );
					methodVisitor.visitLabel( ifCheckLabel );

					index++;
				}


				// throw new IllegalArgumentException("No property was found for a given name");

				methodVisitor.visitFrame( Opcodes.F_SAME, 0, null, 0, null );
				methodVisitor.visitTypeInsn( Opcodes.NEW, Type.getInternalName( IllegalArgumentException.class ) );
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( "No property was found for a given name" );
				methodVisitor.visitMethodInsn(
						Opcodes.INVOKESPECIAL,
						Type.getInternalName( IllegalArgumentException.class ),
						"<init>",
						Type.getType( IllegalArgumentException.class.getDeclaredConstructor( String.class ) ).getDescriptor(),
						false
				);
				methodVisitor.visitInsn( Opcodes.ATHROW );

				Label label = new Label();
				methodVisitor.visitLabel( label );
				methodVisitor.visitLocalVariable(
						"this",
						Type.getDescriptor( clazz ),
						null,
						contractsPropertyNameCheckLabel,
						label,
						0
				);
				methodVisitor.visitLocalVariable(
						"propertyName",
						Type.getDescriptor( String.class ),
						null,
						contractsPropertyNameCheckLabel,
						label,
						1
				);
				methodVisitor.visitMaxs( 3, 2 );

				return new Size( 6, instrumentedMethod.getStackSize() );
			}
			catch (NoSuchMethodException e) {
				throw new IllegalArgumentException( e );
			}
		}

	}


	public static class Foo {
		private String string;
		private Integer num;
		private long looooong;

		public Foo() {
			this( "test", -1 );
			this.looooong = 100L;
		}

		public Foo(String string, Integer num) {
			this.string = string;
			this.num = num;
		}

		public String getMessage() {
			return "messssssage";
		}

		public boolean getKey() {
			return false;
		}
	}
}
