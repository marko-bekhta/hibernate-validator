# JUnit 5 Extension Module for Hibernate Validator Testing

## Problem Statement

Testing constraint validators currently requires significant boilerplate:

- Manually instantiating validators and calling `initialize()` with hand-built annotation descriptors
  (`ConstraintAnnotationDescriptor.Builder` — 65 instances across the test suite, using internal API)
- Passing `null` for `ConstraintValidatorContext` in 620+ `isValid()` calls
- Reimplementing `assertValid`/`assertInvalid` helper methods identically across 24+ test files
- Manually bootstrapping `ValidatorFactory`/`Validator` with repeated configuration
- No framework support for testing validators that use CDI-injected dependencies

This design proposes two things: additions to the existing `test-utils` module for framework-agnostic
utilities, and a new `test-utils-jupiter` module providing a JUnit 5 extension for convenient,
annotation-driven validator testing.

---

## Module Structure

```
test-utils                 (existing, public)  ->  jakarta.validation-api, assertj-core
test-utils-jupiter         (new, public)       ->  test-utils, engine (provided), junit-jupiter-api (provided)
engine                     (existing)          ->  test-utils (test)
```

### Dependency Constraints

- **No cyclic dependencies.** Maven does not allow cycles in any scope combination (verified — even
  `test` vs `provided` creates a cycle in the reactor).
- The extension source code lives in the **engine module** under `src/testExtension/java/`, separate
  from regular test sources. This directory is:
  - Added as a **test source** in the engine (via `build-helper-maven-plugin`) — the engine compiles
    and uses the extension in its own JUnit tests, with full access to engine types.
  - Added as a **main source** in `test-utils-jupiter` (via `build-helper-maven-plugin`) — the same
    code is published for external users.
- Build order: `test-utils` → `engine` → `test-utils-jupiter`. No cycle — engine never depends on
  `test-utils-jupiter`.
- `test-utils-jupiter` depends on the engine at `provided` scope — the extension code compiles
  against HV types, but users already have the engine on their classpath.
- `junit-jupiter-api` is `provided` scope in both `test-utils-jupiter` and the engine's test
  dependencies — users already have JUnit on their test classpath, and we avoid version conflicts.
- Cross-version testing: verify against JUnit Jupiter 5.x and 6.x.

### Source Layout

```
engine/
  src/main/java/                         → engine production code
  src/test/java/                         → engine test classes (TestNG, existing)
  src/testExtension/java/                → extension source code (shared)
    org/hibernate/validator/testextension/
      ValidatorTest.java                 → @ValidatorTest annotation
      ValidatorExtension.java            → JUnit 5 extension implementation
      ConstraintSource.java              → @ConstraintSource annotation
      HibernateTestConstraintValidatorContext.java
      ...                                → parameter resolvers, CDI support, etc.

test-utils/
  src/main/java/                         → Annotations, TestConstraintValidatorContext, AssertJ assertions

test-utils-jupiter/
  pom.xml                               → references engine/src/testExtension/java as main source
  src/test/java/                         → tests for the extension itself
```

**Phase 1 (now):** Only `test-utils-jupiter` compiles the extension sources. The engine hosts them
but does not compile or use them yet. The engine's `pom.xml` includes the `build-helper-maven-plugin`
config and `junit-jupiter-api` dependency **commented out** — ready to be uncommented during the
JUnit migration. Tests for the extension live in `test-utils-jupiter/src/test/java/`.

The `test-utils-jupiter`'s `pom.xml` adds the shared directory as a main source:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>add-shared-extension-source</id>
            <phase>generate-sources</phase>
            <goals><goal>add-source</goal></goals>
            <configuration>
                <sources>
                    <source>${project.basedir}/../engine/src/testExtension/java</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Phase 2 (JUnit migration):** Uncomment the `junit-jupiter-api` dependency and
`build-helper-maven-plugin` config in the engine's `pom.xml`. Engine tests are migrated from
TestNG to JUnit and start using the extension directly.

**Decision:** Shared source directory over separate modules. The extension code is authored and
tested in the engine (where it has access to HV types for dogfooding), then published via
`test-utils-jupiter` for external users. One source of truth, no cycle.

**Discarded alternative:** `test-utils-jupiter` as a purely jakarta-only module (no engine dependency).
Rejected because it would prevent HV-specific context support (`HibernateConstraintValidatorContext`,
`HibernateConstraintValidatorInitializationContext`) which is increasingly important as the HV API
grows (PR #2032 BeanResolver, password validators, etc.).

**Discarded alternative:** Extracting HV public API types into a separate `hibernate-validator-api`
module to break the cycle. Rejected as too much restructuring for this use case alone.

### Module Naming

`hibernate-validator-test-utils-jupiter` — "jupiter" is the JUnit 5 API module name and won't age
like "junit5" would.

---

## `test-utils` Additions (Framework-Agnostic)

### 1. `Annotations` — Easy Constraint Annotation Instances

Replaces the internal `ConstraintAnnotationDescriptor.Builder` boilerplate with a public API.

**API:**

```java
// No-attribute shorthand — returns annotation with defaults
NotNull notNull = Annotations.of(NotNull.class);

// Builder for attributes
ISBN isbn = Annotations.builder(ISBN.class)
    .message("Invalid ISBN")
    .groups(MyGroup.class)
    .payload(Severity.Error.class)
    .attribute("type", ISBN.Type.ISBN_10)
    .build();
```

- `of(Class<A>)` — returns an annotation instance with all default values
- `builder(Class<A>)` — returns a builder with:
  - `.message(String)` — typed (every constraint has `message()`)
  - `.groups(Class<?>...)` — typed (every constraint has `groups()`)
  - `.payload(Class<? extends Payload>...)` — typed (every constraint has `payload()`)
  - `.attribute(String, Object)` — generic, for constraint-specific attributes
  - `.build()` — returns the annotation instance

**Decision:** Two methods (`of` / `builder`) rather than one method that returns a builder
requiring `.build()` even for the no-attribute case. The zero-attribute case is very common.

**Discarded alternative:** A single method returning a builder with an always-required `.build()` call.
Rejected because `Annotations.of(NotNull.class)` is cleaner than `Annotations.of(NotNull.class).build()`.

### 2. `TestConstraintValidatorContext` — Recording Context

A test implementation of `ConstraintValidatorContext` that records all interactions for later
assertion. Solves the problem of testing validators that create custom violations through the context.

Two levels of implementation:

**Jakarta-level (in `test-utils`):**

An interface and standalone implementation using only `jakarta.validation.ConstraintValidatorContext`.
No engine dependency. Usable by anyone regardless of test framework:

```java
public interface TestConstraintValidatorContext extends ConstraintValidatorContext {

    boolean isDefaultViolationDisabled();
    List<TestConstraintViolation> getTestViolations();

    static TestConstraintValidatorContext standalone();
    static TestConstraintValidatorContext standalone(String defaultTemplate, ClockProvider clockProvider);
}
```

- Implements the full `ConstraintViolationBuilder` chain, recording each violation with its
  message template, property path nodes, and terminal `addConstraintViolation()` calls
- `standalone()` factory methods construct a proper context from available information
  (message template from the constraint annotation, clock provider from the factory configuration)
- Lives in `test-utils` with no engine dependency

**HV-level (in `engine/src/testExtension/`, published via `test-utils-jupiter`):**

An extended implementation that also implements `HibernateConstraintValidatorContext`, recording
HV-specific interactions:

```java
public interface HibernateTestConstraintValidatorContext
        extends TestConstraintValidatorContext, HibernateConstraintValidatorContext {

    Map<String, Object> getMessageParameters();
    Map<String, Object> getExpressionVariables();
    Object getDynamicPayload();
}
```

Records `addMessageParameter()`, `addExpressionVariable()`, `withDynamicPayload()` calls.
Constructed by the JUnit extension from information available in the `ValidatorFactory`
configuration. Supports `unwrap(HibernateConstraintValidatorContext.class)`.

**TestConstraintViolation** — recorded violation data:

```java
public interface TestConstraintViolation {
    String getMessageTemplate();
    String getPropertyPath();
    Map<String, Object> getMessageParameters();  // HV-specific, empty at jakarta level
    // ... other recorded data
}
```

**Decision:** Two-tier design. The jakarta-level interface lives in `test-utils` (framework-agnostic,
no engine dependency). The HV-level extension lives in the shared `testExtension` source (has access
to engine types, published via `test-utils-jupiter`). The JUnit extension injects the HV-level
implementation when available.

### 3. `TestConstraintValidatorContextAssert` — AssertJ Assertions

AssertJ assertion class for inspecting the recording context after `isValid()` calls:

```java
TestConstraintValidatorContextAssert.assertThat(context)
    .hasDefaultViolationDisabled()
    .hasViolations(1)
    .violation(0, v -> v
        .hasMessageTemplate("Invalid zip: ${zip}")
        .hasPropertyPath("zipCode")
    );
```

Lives alongside the existing `ConstraintViolationAssert` in `test-utils`.

---

## `test-utils-jupiter` — JUnit 5 Extension

### 1. `@ValidatorTest` — Composed Meta-Annotation

The single entry-point annotation. Pulls in the full extension chain via `@ExtendWith`:

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ValidatorExtension.class)
@ExtendWith(ValidatorParameterResolver.class)
@ExtendWith(ConstraintValidatorParameterResolver.class)
@ExtendWith(ContextParameterResolver.class)
public @interface ValidatorTest {
}
```

**CDI auto-detection:** CDI bootstrap is triggered automatically when the extension detects
`@Produces` methods (from `jakarta.enterprise.inject`) on the test class or its supertypes.
No explicit flag needed — `@Produces` can't be compiled without CDI on the classpath, so its
presence is an unambiguous signal. If CDI is not on the classpath, the check never fires.

**CDI lifecycle:** Follows JUnit's `TestInstance.Lifecycle`. With `PER_CLASS`, the CDI container
boots once and is shared across all tests. With `PER_METHOD` (default), a fresh container per test.
Same applies to `ValidatorFactory` lifecycle in non-CDI mode.

**Decision:** No `cdi = true` flag. Auto-detection from `@Produces` methods is simpler — one less
concept to learn, zero configuration for the common case.

**Discarded alternative:** `@ValidatorTest(cdi = true)` boolean attribute. Rejected because the
presence of `@Produces` already implies CDI intent unambiguously.

**Discarded alternative:** Separate `@WithCDI` annotation. Rejected for simplicity — validation
testing doesn't need the layered composition that ORM requires (`@ServiceRegistry` -> `@DomainModel` ->
`@SessionFactory`). Our domain is simpler: one `ValidatorFactory`, optionally with CDI.

**Discarded alternative:** Separate jakarta-level vs HV-specific annotations (like ORM's `@Jpa` vs
`@SessionFactory`). Rejected because validation has a single factory concept, and HV-specific
configuration is handled programmatically via `ValidatorProducer` (see below).

### 2. Factory Configuration — Three Levels

Three mechanisms for configuring the `ValidatorFactory`, from lightest to heaviest. They can be
combined — producers provide components, the configurer tweaks settings, or the full producer
replaces everything.

#### Level 1: `@ProducesValidatorComponent` — Individual Components

See section 3 below. Provides individual factory components that are also available as test
method parameters.

#### Level 2: `@ConfigureValidator` — Configuration Callback

A method that receives the `Configuration<?>` object and can modify it before the factory is built.
The extension still handles factory construction — the user just tweaks settings:

```java
@ValidatorTest
class MyTest {

    @ConfigureValidator
    void configure(Configuration<?> config) {
        config.addProperty("some.property", "value");

        // HV-specific? Just cast — HV is on the classpath at runtime
        if (config instanceof HibernateValidatorConfiguration hvConfig) {
            hvConfig.temporalValidationTolerance(Duration.ofMillis(100));
        }
    }
}
```

This is the middle ground for HV-specific settings that don't need to be accessed as test
parameters. The extension applies `@ProducesValidatorComponent` instances first, then calls
`@ConfigureValidator`, then builds the factory.

Discovered on the test class and supertypes. Method-level `@ValidatorTest` can override
class-level configuration.

#### Level 3: `ValidatorProducer` — Full Factory Control

For users who need complete control over `ValidatorFactory` construction:

```java
@ValidatorTest
class MyTest implements ValidatorProducer {

    @Override
    public ValidatorFactory produceValidatorFactory() {
        return Validation.byProvider(HibernateValidator.class)
            .configure()
            .clockProvider(myClock)
            .buildValidatorFactory();
    }
}
```

When `ValidatorProducer` is implemented, the extension delegates entirely — it does NOT apply
`@ProducesValidatorComponent` or `@ConfigureValidator` to the Configuration (since the user
builds the factory themselves). However, `@ProducesValidatorComponent` instances are still
available as test method parameters.

Follows the hibernate-orm pattern (`SessionFactoryProducer`, `DomainModelProducer`).

### 3. Factory Component Producers

For configuring individual factory components without implementing the full `ValidatorProducer`,
methods in the test class (or supertypes) can be annotated to provide specific components:

```java
@ValidatorTest
class MyTest {

    @ProducesValidatorComponent
    ClockProvider clockProvider() {
        return mock(ClockProvider.class);
    }

    @ProducesValidatorComponent
    MessageInterpolator messageInterpolator() {
        return new CustomMessageInterpolator();
    }
}
```

The extension:
1. Scans the test class hierarchy for `@ProducesValidatorComponent` methods
2. Calls each once, caches the returned instance
3. Applies to the `Configuration` by `instanceof` matching on the return value
   (known jakarta types: `ClockProvider`, `MessageInterpolator`, `TraversableResolver`,
   `ConstraintValidatorFactory`, `ParameterNameProvider`, `ValueExtractor`)
4. Registers **all** produced instances for parameter injection by type — regardless of whether
   the extension knows how to route them to the `Configuration`

This means the **same instance** is both used to configure the factory AND available as a test
method parameter for verification:

```java
@ValidatorTest
class MyTest {

    @ProducesValidatorComponent
    ClockProvider clockProvider() {
        return mock(ClockProvider.class);
    }

    @Test
    void testTemporalValidation(Validator validator, ClockProvider clock) {
        // 'clock' is the SAME mock that was used to build the ValidatorFactory
        when(clock.getClock()).thenReturn(fixedClock);

        validator.validate(myBean);

        verify(clock).getClock();
    }
}
```

For HV-specific components that the extension can't route automatically (because it only knows
jakarta types), the user combines `@ProducesValidatorComponent` (for parameter access) with
`ValidatorProducer` (for applying it to the configuration):

```java
@ValidatorTest
class MyTest implements ValidatorProducer {

    @ProducesValidatorComponent
    Duration tolerance() {
        return Duration.ofMillis(100);
    }

    @Override
    public ValidatorFactory produceValidatorFactory() {
        return Validation.byProvider(HibernateValidator.class).configure()
            .temporalValidationTolerance(tolerance())
            .buildValidatorFactory();
    }

    @Test
    void test(Validator validator, Duration tolerance) {
        // both available — same Duration instance
    }
}
```

**Decision:** Single `@ProducesValidatorComponent` annotation, resolved by return type for
configuration routing. The return type IS the routing key — each factory component type is unique,
so no property name or string key is needed. Any produced instance is available as a test method
parameter regardless of whether the extension knows how to route it to the `Configuration`.

This avoids a proliferation of annotations (`@ProducesMessageInterpolator`,
`@ProducesClockProvider`, etc.) while remaining clear.

Methods are discovered on the test class and all supertypes, allowing base test classes to
provide shared configuration.

**Discarded alternative:** `@ValidatorProperty("name-of-prop")` with string property names mapped
to `Configuration` setters. Rejected because the jakarta `Configuration.addProperty(String, String)`
only accepts strings (not object instances), and mapping string names to typed setters would require
the extension to know about HV-specific setters or use brittle reflection.

**Discarded alternative:** Using ONLY a `@ConfigureValidator` callback (without `@ProducesValidatorComponent`).
Rejected as the sole mechanism because it doesn't provide parameter access to the configured
components — the user can't get the same mock instance back as a test parameter for verification.
Instead, both coexist: `@ProducesValidatorComponent` for components that need parameter access,
`@ConfigureValidator` for settings that don't.

### 4. Parameter Resolvers

The extension resolves these types as test method parameters:

| Parameter Type | What's Injected |
|---|---|
| `Validator` | Validator from the configured factory |
| `ValidatorFactory` | The configured factory itself |
| `ConstraintValidator<A, T>` | Initialized validator instance (see `@ConstraintSource`) |
| `ConstraintValidatorContext` | HV-aware recording context |
| `TestConstraintValidatorContext` | Jakarta-level recording context (from `test-utils`) |
| `HibernateTestConstraintValidatorContext` | HV-level recording context (from `testExtension`) |
| `HibernateConstraintValidatorInitializationContext` | Init context built from factory config |
| `@ProducesValidatorComponent` types | Same instance used to configure the factory |
| CDI beans (when `cdi = true`) | Beans produced by `@Produces` methods in the test class hierarchy |

Resolution priority:
1. Extension-managed types (Validator, ValidatorFactory, contexts, init contexts)
2. `@ConstraintSource`-provided validators
3. `@ProducesValidatorComponent` instances (by type)
4. CDI beans (only types explicitly produced in the test class hierarchy)

**Decision for CDI bean resolution:** Only resolve beans whose types match `@Produces` methods found
on the test class or its supertypes. This prevents accidental resolution of internal CDI beans and
keeps the behavior explicit and predictable. The extension walks the full class hierarchy to discover
producer methods.

### 5. `@ConstraintSource` — Parameterized Validator Creation

A custom `ArgumentsProvider` that resolves constraint annotation instances into initialized
`ConstraintValidator` instances.

```java
@interface ConstraintSource {
    String value();           // source method name
    int constraintAt() default 0;  // position of annotation in Arguments tuple
}
```

**Mode 1 — Single config (class-level or method-level):**

```java
@ValidatorTest
@ConstraintSource("isbn10")  // class-level default
class ISBN10ValidatorTest {

    static ISBN isbn10() {
        return Annotations.builder(ISBN.class)
            .attribute("type", ISBN.Type.ISBN_10)
            .build();
    }

    @Test
    void acceptsNull(ConstraintValidator<ISBN, String> validator,
                     ConstraintValidatorContext context) {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    @ConstraintSource("isbn13")  // method-level override
    void isbn13(ConstraintValidator<ISBN, String> validator,
                ConstraintValidatorContext context) {
        assertTrue(validator.isValid("978-123-456-789-7", context));
    }

    static ISBN isbn13() {
        return Annotations.builder(ISBN.class)
            .attribute("type", ISBN.Type.ISBN_13)
            .build();
    }
}
```

When the source method returns a single annotation, it's used for a regular `@Test` method.

**Mode 2 — Parameterized (stream of configs):**

```java
@ParameterizedTest
@ConstraintSource("isbnConfigs")
void allTypesAcceptNull(ConstraintValidator<ISBN, String> validator,
                        ConstraintValidatorContext context) {
    assertTrue(validator.isValid(null, context));
}

static Stream<ISBN> isbnConfigs() {
    return Stream.of(
        Annotations.of(ISBN.class),
        Annotations.builder(ISBN.class).attribute("type", ISBN.Type.ISBN_10).build(),
        Annotations.builder(ISBN.class).attribute("type", ISBN.Type.ISBN_13).build()
    );
}
```

When the source method returns a `Stream`/`Collection`, each element becomes a test invocation.

**Mode 3 — Mixed arguments (`constraintAt`):**

```java
@ParameterizedTest
@ConstraintSource("isbnTestCases")  // constraintAt = 0 by default
void testISBN(ConstraintValidator<ISBN, String> validator,
              String input, boolean expected,
              ConstraintValidatorContext context) {
    assertThat(validator.isValid(input, context)).isEqualTo(expected);
}

static Stream<Arguments> isbnTestCases() {
    return Stream.of(
        Arguments.of(Annotations.builder(ISBN.class)
                .attribute("type", ISBN.Type.ISBN_10).build(),
            "99921-58-10-7", true),
        Arguments.of(Annotations.builder(ISBN.class)
                .attribute("type", ISBN.Type.ISBN_10).build(),
            "99921-58-10-8", false)
    );
}
```

The `constraintAt` attribute tells the extension which position in the `Arguments` tuple contains
the constraint annotation. It resolves that into a validator and passes everything else through.

**Under the hood**, `@ConstraintSource` resolves validators by:
1. Getting the constraint annotation instance from the source method
2. Reading `@Constraint(validatedBy = {...})` from the annotation type via reflection
3. Matching the validator class based on the validated type (from the `ConstraintValidator<A, T>`
   generic parameter)
4. Getting an instance from the `ConstraintValidatorFactory`
5. Calling `initialize()` with the annotation
6. Injecting the ready-to-use validator

All of this uses jakarta API + reflection — no engine dependency needed.

**Decision:** Custom `@ConstraintSource` rather than relying on `@MethodSource` + a helper object.
The source method returns clean domain objects (annotation instances) while the extension handles
the mechanical validator creation. This eliminates the need for a `ConstraintValidators` helper class.

**Discarded alternative:** Putting the constraint annotation directly on the parameter
(`@ISBN(type=ISBN.Type.ISBN_10) ConstraintValidator<ISBN, String> validator`). Rejected for two
reasons: (1) constraint annotations are not required to target `ElementType.PARAMETER` — custom
constraints may only target `FIELD`/`METHOD`; (2) annotation values are compile-time constants,
so you can't vary them across parameterized test invocations.

**Discarded alternative:** `@ConfigureConstraint` annotation with string-based `@Attribute` values.
Rejected because it loses type safety — enum values like `ISBN.Type.ISBN_10` would need string
representation and runtime coercion.

**Discarded alternative:** Custom test template mechanism (`@ConstraintValidatorCheck` with
`valid`/`invalid` arrays). Rejected because JUnit's `@ParameterizedTest` + `@ValueSource`/`@MethodSource`
already provides per-value test reporting, and the template added marginal value over standard JUnit
patterns with our parameter resolvers.

### 6. CDI Integration

When the extension detects `@Produces` methods on the test class (or supertypes), it automatically
bootstraps a lightweight CDI container using Weld SE (following patterns from `weld-junit-jupiter`):

```java
@ValidatorTest
class ZipCodeValidatorTest {

    @Produces
    ZipCodeRepository repo() {
        return mock(ZipCodeRepository.class);
    }

    @Test
    void testValid(ConstraintValidator<ZipCode, String> validator,
                   ZipCodeRepository repo,    // same mock from CDI
                   ConstraintValidatorContext context) {
        when(repo.exists("12345")).thenReturn(true);
        assertTrue(validator.isValid("12345", context));
        verify(repo).exists("12345");
    }
}
```

**Bootstrap approach** (inspired by weld-testing):
- Discovery disabled (`Weld.disableDiscovery()`) — no classpath scanning
- Test class registered as a bean source — its `@Produces` methods are discovered
- Supertype hierarchy walked for additional `@Produces` methods
- CDI-aware `ConstraintValidatorFactory` uses `BeanManager.createInjectionTarget()` to create
  validators with `@Inject` support
- Container lifecycle follows JUnit's `TestInstance.Lifecycle` — PER_CLASS shares the container,
  PER_METHOD (default) boots a fresh one per test

**CDI bean parameter resolution:**
The extension tracks which types are produced by `@Produces` methods in the test class hierarchy.
Only those types are resolvable as test method parameters — prevents accidental resolution of
internal CDI infrastructure beans.

**`@ProducesValidatorComponent` vs `@Produces` — clear separation:**
These annotations serve different purposes and must not be confused:
- `@ProducesValidatorComponent` → configures the `ValidatorFactory` (clock provider, message
  interpolator, etc.) and makes the instance available as a test parameter
- `@Produces` (CDI) → registers a bean in the CDI container, injected into validators via `@Inject`

```java
@ValidatorTest
class MyTest {

    // Factory configuration — used to BUILD the ValidatorFactory
    @ProducesValidatorComponent
    ClockProvider clock() { return mock( ClockProvider.class ); }

    // CDI bean — INJECTED into validators via @Inject
    @Produces
    ZipCodeRepository repo() { return mock( ZipCodeRepository.class ); }

    @Test
    void test(Validator validator, ClockProvider clock, ZipCodeRepository repo) {
        // both available as parameters, different wiring
    }
}
```

**Dependencies:**
- `org.jboss.weld.se:weld-se-core` — `compile` scope (users get it automatically)
- `jakarta.enterprise:jakarta.enterprise.cdi-api` — transitively provided by Weld

Users don't need to know about Weld or pick a compatible version. CDI bootstrap is auto-detected
and only activated when needed — zero overhead for non-CDI tests.

**Discarded alternative:** Weld SE as `provided` scope requiring users to add it manually. Rejected
because users would need to know about Weld and pick a compatible version — poor UX.

**Discarded alternative:** Lightweight "fake" CDI injection (scanning `@Inject` fields, setting
via reflection without a real container). Rejected because it reimplements CDI APIs that we'd have
to maintain, and behavior would diverge from real CDI.

---

## Integration with `@ParameterizedTest`

Rather than inventing a custom test template, the extension is designed to compose with JUnit's
built-in `@ParameterizedTest`:

```java
@ValidatorTest
@ConstraintSource("isbn10")
class ISBN10Test {

    static ISBN isbn10() {
        return Annotations.builder(ISBN.class)
            .attribute("type", ISBN.Type.ISBN_10)
            .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"99921-58-10-7", "9971-5-0210-0", "0-9752298-0-X"})
    void validISBN(ConstraintValidator<ISBN, String> validator,
                   String input,
                   ConstraintValidatorContext context) {
        assertTrue(validator.isValid(input, context));
    }

    @ParameterizedTest
    @ValueSource(strings = {"99921-58-10-8", "", "978-0-5"})
    void invalidISBN(ConstraintValidator<ISBN, String> validator,
                     String input,
                     ConstraintValidatorContext context) {
        assertFalse(validator.isValid(input, context));
    }
}
```

This gives per-value reporting in the test runner, uses patterns developers already know, and
requires no custom test template infrastructure.

**Decision:** Compose with `@ParameterizedTest` rather than building a custom template. JUnit's
parameterized infrastructure already handles per-value reporting, multiple source types
(`@ValueSource`, `@MethodSource`, `@CsvSource`, `@EnumSource`), and display name customization.

---

## Integration SPI

The extension delegates all operations through a `ValidatorTestIntegration` SPI, discovered via
`ServiceLoader`. Our own default behavior is just the default implementation of this SPI — we eat
our own dog food.

### `ValidatorTestIntegration` interface

```java
public interface ValidatorTestIntegration {

    boolean isActive(ExtensionContext context);

    default ValidatorFactory createValidatorFactory(ExtensionContext context,
            Map<Class<?>, Object> components, List<Method> configurers) {
        throw new UnsupportedOperationException(
            getClass().getSimpleName()
            + " does not support ValidatorFactory bootstrapping."
        );
    }

    default void applyComponents(Configuration<?> config,
            Map<Class<?>, Object> components) {
        throw new UnsupportedOperationException(
            getClass().getSimpleName()
            + " does not support @ProducesValidatorComponent. "
            + "Configure your ValidatorFactory through your framework instead."
        );
    }

    default <T> T resolveBean(Class<T> type) {
        throw new UnsupportedOperationException(
            getClass().getSimpleName()
            + " does not support bean resolution."
        );
    }

    // Future capabilities added as default methods that throw —
    // existing implementations don't break, unsupported usage fails clearly.
}
```

All methods have default implementations that throw `UnsupportedOperationException` with an
actionable message. New capabilities can be added in future versions without breaking existing
integrations — they just throw until implemented.

### Architecture

```
ValidatorExtension (orchestrator)
    → ServiceLoader<ValidatorTestIntegration>
    → finds active integration (or falls back to DefaultValidatorTestIntegration)
    → delegates all operations

DefaultValidatorTestIntegration (ships with the module)
    → factory bootstrap (Validation.buildDefaultValidatorFactory())
    → @ProducesValidatorComponent (instanceof routing to Configuration)
    → @ConfigureValidator (callback with Configuration)
    → ValidatorProducer (full factory control)
    → CDI/Weld auto-detection (@Produces → boot Weld SE)
    → bean resolution (CDI beans + produced components)

SpringValidatorTestIntegration (external, shipped by Spring/community)
    → isActive() → checks for @SpringBootTest
    → createValidatorFactory() → from ApplicationContext
    → resolveBean() → from ApplicationContext
    → applyComponents() → throws (Spring owns the factory)

QuarkusValidatorTestIntegration (external, shipped by Quarkus/community)
    → similar, adapted to Arc
```

The extension never hardcodes behavior — it always goes through the SPI. This proves the SPI
works (we are our own first consumer) and makes the integration points well-defined for
third-party frameworks.

**Decision:** SPI with default-throwing methods rather than a capability enum +
`supportedCapabilities()` set. Forward-compatible — adding a new method doesn't break existing
implementations. Unsupported usage fails with clear, actionable error messages.

**Decision:** Weld SE as a compile dependency of `test-utils-jupiter`. Users don't need to know
about Weld or which version to use. CDI bootstrap is auto-detected from `@Produces` methods —
only activated when needed, zero overhead otherwise.

**Discarded alternative:** Weld SE as `provided` scope. Rejected because users would need to know
about Weld, pick a compatible version, and add it explicitly — poor user experience.

**Discarded alternative:** Lightweight "fake" CDI injection (scanning `@Inject` fields, setting
them via reflection). Rejected because it reimplements CDI APIs that we'd have to maintain, and
behavior would inevitably diverge from real CDI.

---

## Scope Management

Following the hibernate-orm pattern:
- Extension state is stored in JUnit's `ExtensionContext.Store` with `AutoCloseable` for cleanup
- `@ValidatorTest` at class level: factory created once, shared across tests
- `@ValidatorTest` at method level: fresh factory for that test, overriding class-level config
- `@ConstraintSource` at class level: shared constraint config for all tests
- `@ConstraintSource` at method level: overrides class-level config

---

## Prior Art

### PR #1281 (shark300, 2022)
Proposed `PreconfiguredConstraintValidatorFactory` and `PreconfiguredValidatorsValidatorFactory` —
utility classes that substitute entire validator instances (typically mocks). No JUnit integration.

**Why we went further:** That PR mocks the whole validator rather than injecting dependencies into
a real one. Our CDI integration solves the actual problem — test the real validator logic with mock
dependencies injected.

### hibernate-orm testing framework
Three-layer extension stack: `@ServiceRegistry` -> `@DomainModel` -> `@SessionFactory`. Rich
composed annotations, scope objects, producer interfaces, method-level overrides.

**What we borrowed:** Composed meta-annotations, producer interfaces, `ExtensionContext.Store`-based
scope management, dual annotation/programmatic configuration.

**What we simplified:** Validation has a simpler domain (one factory, not three layers), so we use
a single `@ValidatorTest` annotation rather than a stack.

### weld-testing (`weld-junit-jupiter`)
Mature Weld SE integration for JUnit 5. Discovery disabled by default, test class as bean source,
parameter injection from CDI container.

**What we borrowed:** Weld SE bootstrap pattern (disabled discovery, test class as bean source),
parameter resolution for CDI beans, lifecycle management.

**What we changed:** We track explicitly produced types rather than using greedy CDI bean resolution,
to avoid conflicts with our own parameter resolvers.

---

## Validator Initialization Strategy

Since the extension code lives in the engine's `testExtension` sources, it has access to HV types.
This enables full HV-aware initialization in `@ConstraintSource`:

### Path 1: Unit testing via `@ConstraintSource` (full initialization)

The `@ConstraintSource` mechanism resolves validators by:
1. Reading `@Constraint(validatedBy = {...})` from the annotation type
2. Getting a validator instance from the `ConstraintValidatorFactory`
3. Calling `initialize(annotation)` — the standard jakarta path
4. If the validator implements `HibernateConstraintValidator`, also calling
   `initialize(ConstraintDescriptor, HibernateConstraintValidatorInitializationContext)` with a
   properly constructed descriptor and init context built from the factory configuration

The init context is constructed from information available in the `ValidatorFactory`:
- `ClockProvider` — from factory configuration
- `ScriptEvaluatorFactory` — from factory configuration
- `TemporalValidationTolerance` — from factory configuration
- `SharedData` — from constraint validator payload
- `BeanResolver` — from factory configuration (when available, PR #2032)

This means validators using HV-specific initialization (including `BeanResolver`) are properly
initialized even in unit tests.

### Path 2: Integration testing via `Validator` (full engine pipeline)

For testing the complete validation flow including message interpolation, group sequences,
cascading, etc., users inject `Validator` and validate annotated beans:

```java
@ValidatorTest
class PasswordValidatorTest {

    @Test
    void testPasswordStrength(Validator validator) {
        var result = validator.validate(new UserForm("weak"));
        assertThat(result).containsOnlyViolations(violationOf(PasswordStrength.class));
    }

    static class UserForm {
        @PasswordStrength(min = 3)
        final String password;
        UserForm(String password) { this.password = password; }
    }
}
```

The engine handles everything end-to-end. The extension just provides the `Validator`.

---

## Cross-Version Testing

A dedicated integration test module (`integrationtest/java/modules/test-utils-jupiter`) verifies
the extension works across JUnit Jupiter versions.

- **Default version**: 5.12.2 (from root pom)
- **Tested via profiles**: `jupiter-5.10` (5.10.5), `jupiter-5.11` (5.11.4), `jupiter-6.0` (when released)
- `junit-jupiter-api` is `provided` scope in `test-utils-jupiter` — users bring their own version
- CI matrix runs all profiles automatically
- Compile against the version we use; profiles test backward compatibility to find the floor

**Decision:** Profile + CI matrix approach over separate modules per version. Lighter — one module,
profiles override the version property. CI matrix automates coverage.

**Decision:** `junit-jupiter-api` as `provided` scope. Users already have JUnit on their test
classpath — we avoid forcing a specific version and version conflicts.

---

## Open Questions

1. **`BeanResolver` integration (PR #2032)**: Once `BeanResolver` lands, validators that resolve
   dependencies via `initContext.getBeanResolver()` will need test support. The CDI integration
   partially addresses this (beans are injectable), but the `BeanResolver` API itself may need
   test-friendly configuration. For v1, these validators are tested via Path 2 (full `Validator`
   integration testing).

2. **IDE support for shared sources**: The `build-helper-maven-plugin` shared source directory
   (`engine/src/testExtension/java`) should work in IntelliJ and Eclipse, but needs verification.
   Both IDEs support additional source roots from build-helper.
