---
name: Create new built-in constraint
description: |
  MUST use when the user asks to create, design, implement, add, propose,
  or modify a NEW built-in Hibernate Validator constraint annotation.
  This includes requests such as "add a @Base64 constraint", "create a new
  constraint", "design a constraint", or "propose a built-in constraint".

  This is a PROCEDURAL workflow skill. When selected, the agent MUST follow
  Phases 1–5 in order and MUST NOT skip the research, user decision,
  sign-off, generation, or verification gates.
---

# New Hibernate Validator Constraint

## Applicability

MUST use this skill when the user asks to create, design, add, implement,
or propose a NEW built-in Hibernate Validator constraint.

This skill governs both the interaction workflow and the implementation.

## CRITICAL: Workflow Contract

This is a procedural workflow, not a reference guide.

Once selected, the agent MUST NOT skip workflow gates even if:
- the user asks for immediate code;
- the implementation appears obvious;
- a similar existing constraint can be copied;
- the agent believes no design decisions are necessary.

### State machine

The workflow has exactly these states:

1. RESEARCH
2. DECISION
3. WAIT_FOR_USER
4. DECISION
5. WAIT_FOR_USER
6. ...
7. FINAL_PROPOSAL
8. WAIT_FOR_GO
9. GENERATE
10. VERIFY
11. DONE

### Hard rules

- Complete required research before the first design question.
- Present exactly ONE unresolved decision per response.
- A decision may have multiple options, but only one independently answerable choice.
- After asking the decision, STOP and wait.
- Never present the next decision until the current one is answered.
- Never generate implementation code before explicit final approval.
- `yes` to an individual decision is NOT final approval.
- Final approval must occur after the complete proposal and decision log.
- If a decision changes, re-evaluate affected downstream decisions.
- If verification discovers a design contradiction, return to DECISION.
- The user's request to skip a mandatory step does not bypass the workflow.

### Current workflow state

The agent must internally track:

- Phase
- Current unresolved decision
- Resolved decisions
- Remaining decisions
- Whether waiting for user input
- Whether final approval has been received

If waiting for user input, the agent MUST NOT advance the workflow in that
response.

Prefer mirroring an existing, closest constraint over writing from scratch. 
Identify the architectural complexity first, then choose the closest existing Hibernate Validator constraint as the implementation template.

Useful reference patterns:
- **Simple:** `@NotNull` or another basic boolean check.
- **Medium:** `Contains` / `CodePointLength` / `@Size` / `@Pattern` — attributes and configuration.
- **Specialized/complex:** `Normalized` / `Port` / `IBAN` / `Email` — enum attributes, multiple supported types, parsing, or specialized validation logic.

The reference constraint is an architectural template, not a behavior specification: adapt it to the new constraint's actual requirements.

---

## Phase 1 — Research & Pattern Matching

### Research is mandatory

Always research before proposing anything. Cover at least:

- The **governing standard / specification** if one exists (e.g. RFC 4648 for Base64), otherwise the strongest de-facto rules such as a language/library specification, vendor documentation, or Wikipedia.
- **Existing Java classes** that perform the core operation (e.g. `java.util.Base64`), including their exact strictness, variants, edge cases, and exception behavior.
- **Prior art** in Bean Validation, Jakarta Validation, Hibernate Validator, and relevant language/library ecosystems.
- **Existing Hibernate Validator constraints** that overlap or provide a useful architectural pattern.
- **Security**, using the mandatory Security Checklist below. This is mandatory, never skippable!
- **API compatibility and overlap**, including whether Jakarta Validation already provides a related constraint or whether the proposed API would conflict with an existing HV concept.

Do not silently substitute general model knowledge for research findings. If research is inconclusive, record that uncertainty.

### Explicitly establish feasibility

Use the research to judge two things explicitly:

1. **Feasibility** — is this well-defined, general-purpose, and safe enough to ship as a built-in Hibernate Validator constraint? Flag blockers up front.
2. **Path forward** — should the first version implement only the core behavior, or does the evidence justify configuration knobs now?

Also explicitly identify:

3. **Overlap** — is there already an equivalent or substantially overlapping Jakarta Validation/Hibernate Validator constraint?
4. **API stability** — could a proposed attribute or supported type become difficult to change later because it is public API?
5. **Implementation complexity** — is the constraint simple validation logic, configuration-heavy, or effectively a parser?

### Type support is a first-class design question

Determine whether the constraint should support:

- `String`
- `CharSequence`
- another single type
- multiple types requiring multiple validators

Do not assume `String` merely because the example input is textual.

Consider:
- semantic correctness of each supported type;
- consistency with existing HV constraints;
- annotation-processor support;
- validator complexity;
- future extensibility;
- whether supporting a type creates surprising behavior.

Record the decision and rationale.

### Attribute design

For every proposed annotation attribute, explicitly ask:

> Why should this be configurable public API rather than fixed behavior?

Do not expose a knob merely because the underlying standard has multiple modes. Prefer the smallest API that correctly serves the general-purpose use case.

---

## Security Checklist — mandatory

For any constraint whose validator processes user-supplied input, explicitly assess:

- **Complexity attacks:** Can crafted input cause O(n²), exponential, or otherwise unexpectedly expensive execution?
- **Regex / ReDoS:** If regular expressions are involved, is the pattern linear-time and safe from catastrophic backtracking?
- **Memory exhaustion:** Can crafted input cause unbounded allocation, such as splitting, decoding, or allocating based on an input-derived size?
- **Parsing/decoding behavior:** Can the underlying library throw exceptions, consume excessive resources, or behave unexpectedly on malformed input?
- **Library vulnerabilities:** Does the implementation rely on a parser/library with relevant known CVEs, and is the project using a patched version?
- **Unicode normalization:** Does correctness depend on normalization? If so, should validation happen before or after normalization? Do not introduce normalization merely because it seems convenient.
- **Numeric overflow:** Could length, range, checksum, or allocation calculations overflow integer types?
- **Covert channels / equality bypass:** Prefer strict rejection of invalid/non-alphabet characters over silently ignoring them where applicable. For example, RFC 4648 discusses security implications of ignored characters.
- **Decoded content:** If the validator decodes input, validate well-formedness only; never execute or interpret decoded content.
- **Thread safety:** Validators may be reused across validations. Keep them stateless after initialization or otherwise safely initialized.
- **Input-size behavior:** If work or allocation scales with input size, determine whether an explicit size bound is required.

State the outcome of every applicable security check in the Decision Log.

---

## Phase 2 — Decision & User Alignment

Do **not** dump all research at once.

After research, present only a concise list of the decisions that require user input.

Example:

- Supported data types
- Strict vs. lenient parsing
- Required vs. optional padding
- Attribute set / defaults
- Constraint package
- Repeatability
- JIRA key
- `@author`

### One decision at a time — mandatory interaction rule

For each decision:

1. State the **question**.
2. Give the relevant **research finding** in one or two concise sentences.
3. Give the available **options**.
4. Give your **recommendation**.
5. Give a one-line **rationale**.
6. Ask the user to confirm or adjust it.
7. **STOP and wait for the user's answer.**

Do not ask the next decision in the same response.

After the user answers, record the decision and move to the next decision.

If the user's answer introduces a new design consequence, update the remaining decision list accordingly.

### Mandatory decisions

The following decisions must always be addressed when applicable:

#### `@author`

The user must explicitly opt in and provide the author value, or explicitly opt out.

Never assume an author and never copy one from an existing/template constraint.

#### JIRA key

Ask for the JIRA key (for example, `HV-1234`).

If the user does not have one, record:

> JIRA key: TBD

Do not invent a JIRA key.

#### Supported types

Explicitly decide the supported Java types.

#### API attributes

Explicitly decide each public annotation attribute and justify why it belongs in the API.

#### Package

Decide where the constraint belongs, including whether a new subpackage is justified.

### Running Decision Log

Maintain the log as decisions are made:

#### Decision Log

- **[Decision Name]**: Options → **Selected: X**
    - *Research*: Relevant finding.
    - *Rationale*: Why this was selected.
    - *Future*: Deferred improvements or alternatives.

- **Security**
    - Complexity: ...
    - Memory: ...
    - Parsing/decoding: ...
    - Regex/ReDoS: ...
    - Library vulnerabilities: ...
    - Unicode normalization: ...
    - Numeric overflow: ...
    - Covert channels: ...
    - Decoded content: ...
    - Thread safety: ...
    - Input-size behavior: ...

The log should remain terse and skimmable.

---

## Phase 3 — Convergence & Final Sign-off

When all required decisions are resolved, stop the interactive questioning and emit:

### Capabilities Summary

A 2–4 sentence summary describing:
- what the constraint validates;
- attributes;
- supported types;
- important semantics such as null handling.

### Final Decision Log

Present the complete decision and investigation log, including security findings and deferred/future items.

### Error-message proposal

Present every proposed default English message and its message key for approval.

Example:

```text
[ ] org.hibernate.validator.constraints.Base64.message =
    "must be a valid Base64 encoded string"
```

Do not silently add translations to every locale.

English is required; localized translations may be proposed separately and must be flagged for human review.
Ask the user whether they want to have translations. If user agrees -- show the suggested translated versions
and make it easier for the user to approve them (ideally a  checkbox list where the user approves the ones they are ok with).

### Final research consistency check

Before asking for approval, re-check the complete design against the research.

Specifically verify:

- selected behavior still matches the governing specification;
- supported types are justified;
- every attribute is necessary and compatible with the research;
- no existing HV/Jakarta Validation constraint makes the proposed design redundant;
- the chosen reference constraint is still an appropriate architectural template;
- security assumptions still hold;
- no decision contradicts another decision;
- the proposed API does not unnecessarily lock in behavior;
- test and annotation-processor requirements match the final supported types.

If this re-check reveals a contradiction or new design decision, **do not generate code**. Return to Phase 2 and resolve the affected decision one at a time.

### User approval

Present the final proposal and ask for an explicit **Go**.

Do not enter Phase 4 until the user explicitly approves.

---

## Phase 4 — Generate

### Pre-flight

Determine the current project version from `pom.xml`, preferably with Maven (for example, `mvn help:evaluate`).

Derive `@since` as **major.minor**, dropping the patch and `-SNAPSHOT`.

Example:

```text
9.2.0-SNAPSHOT → @since 9.2
```

### Pre-generation checklist

Before writing files, verify that the final design plans all applicable pieces:

- [ ] Annotation with correct meta-annotations.
- [ ] `*Def` configuration class if attributes exist.
- [ ] `ConstraintValidator` implementation(s).
- [ ] `BuiltinConstraint` registration.
- [ ] `ConstraintHelper` registration.
- [ ] Default English validation message.
- [ ] Validator unit test.
- [ ] `PredefinedScopeAllConstraintsTest`.
- [ ] `MessagePropertiesTest`.
- [ ] Registration/constrained test with the requested JIRA key or `TBD`.
- [ ] Annotation Processor `TypeNames.java`.
- [ ] Annotation Processor `ConstraintHelper.java`.
- [ ] AP test model and `ConstraintValidationProcessorIT`, unless explicitly skipped under the rule below.
- [ ] Documentation entry.
- [ ] `package-info.java` if a new package is introduced.
- [ ] Localized translations only if explicitly approved.

Only generate components that apply to the final design.

---

### Engine — main

#### 1. Annotation

Create:

```text
engine/src/main/java/org/hibernate/validator/constraints/[<subpkg>/]<Name>.java
```

Use the repository's established annotation conventions.

For a normal element-level built-in constraint, use:

```java
@Documented
@Constraint(validatedBy = { ... })
@Target({
    METHOD,
    FIELD,
    ANNOTATION_TYPE,
    CONSTRUCTOR,
    PARAMETER,
    TYPE_USE
})
@Retention(RUNTIME)
@Repeatable(List.class)
@Incubating
```

with the nested `List` container for a repeatable version of a constraint.

Do **not** add `@SupportedValidationTarget` to the annotation merely because the constraint is new. It is relevant only where the validation target requires it.

Include:

- `message()`
- `groups()`
- `payload()`
- custom attributes

in the repository's established ordering.

Default message:

```text
{org.hibernate.validator.constraints[.subpkg].Name.message}
```

The annotation's Javadoc must explain:
- what is validated;
- attribute semantics;
- supported types;
- null semantics;
- relevant specification behavior.

Include `@since <version>`.

Include `@author` only according to the explicit Phase 2 decision.

#### New package

If the constraint introduces a new subpackage, create:

```text
.../constraints/<subpkg>/package-info.java
```

Keep its Javadoc generic so later constraints in the same package do not require modifying it.

Mirror existing packages such as `br` / `pl`.

Write the SPDX license header manually because Spotless does not automatically add it to `package-info.java` / `module-info.java`.

---

#### 2. Validator(s)

Create:

```text
engine/src/main/java/org/hibernate/validator/internal/constraintvalidators/hv/<Name>Validator[ForX].java
```

Implement:

```java
ConstraintValidator<Name, X>
```

Use multiple validators only when supported types genuinely require different implementations.

For ordinary element-level constraints, `isValid()` must return `true` for `null`.

Use `@SupportedValidationTarget(ValidationTarget.PARAMETERS)` only when the constraint is actually cross-parameter.

### Implementation requirements

- Precompute immutable state in `initialize()`.
- Normalize attributes once.
- Resolve sentinels once.
- Precompile patterns once.
- Pre-lowercase/precompute lookup values once.
- Keep `isValid()` cheap.
- Prefer early returns.
- Avoid per-validation allocations where reasonably possible.
- Handle null, empty, malformed, and boundary values.
- Comment non-trivial logic with a short explanation of **why**.
- Keep validators thread-safe.
- Do not perform unnecessary parsing or normalization on every validation.

---

#### 3. `ConstraintHelper`

Register the constraint in:

```text
engine/.../internal/metadata/core/ConstraintHelper.java
```

inside `resolve(...)`.

Add the registration alphabetically using the appropriate:

```java
putBuiltinConstraint(...)
```

or:

```java
putBuiltinConstraints(...)
```

form.

---

#### 4. `BuiltinConstraint`

Add the enum constant alphabetically in:

```text
engine/.../internal/metadata/core/BuiltinConstraint.java
```

using:

```java
ORG_HIBERNATE_VALIDATOR_CONSTRAINTS_NAME(
    "org.hibernate.validator.constraints[.subpkg].Name"
)
```

Include composing-constraint dependencies when applicable.

---

#### 5. Default message

Add the English message alphabetically within the appropriate group to:

```text
engine/src/main/resources/org/hibernate/validator/ValidationMessages.properties
```

The English entry is required.

Do not silently add all localized messages.

If translations are desired, propose them separately for user approval and flag them for human review.

---

#### 6. Programmatic constraint definition

Always create:

```text
engine/src/main/java/org/hibernate/validator/cfg/defs/[<subpkg>/]<Name>Def.java
```

It extends:

```java
ConstraintDef<NameDef, Name>
```

and provides one chained method returning `this` for each custom attribute.

Because this is public API:
- add Javadoc;
- add `@Incubating`;
- include `@since`;
- include `@author` only according to the Phase 2 decision.

Skip adding any attribute methods if the constraint has no configurable attributes.

---

## Engine — tests

### 7. Validator unit test

Create:

```text
engine/src/test/java/org/hibernate/validator/test/internal/constraintvalidators/hv/<Name>ValidatorTest.java
```

Cover:

- positive cases;
- negative cases;
- null;
- empty input where applicable;
- boundaries;
- malformed input;
- important attribute combinations;
- security-sensitive edge cases;
- supported types.

Build annotation proxies using `ConstraintAnnotationDescriptor.Builder`.

---

### 8. Built-in integration coverage

Add a bean and:

```text
testConstraint(Name.class, new NameBean())
```

to:

```text
engine/src/test/java/org/hibernate/validator/test/internal/constraintvalidators/PredefinedScopeAllConstraintsTest.java
```

Also add the message-interpolation entry to:

```text
.../constraintvalidators/MessagePropertiesTest.java
```

Both are required for built-in constraint coverage.

---

### 9. Registration test

Create:

```text
engine/src/test/java/org/hibernate/validator/test/constraints/annotations/hv/<Name>ConstrainedTest.java
```

It should extend:

```java
AbstractConstrainedTest
```

Include a bean using the constraint at its allowed targets.

Use:

```java
validator.validate(...)
```

and `ConstraintViolationAssert`.

Add:

```java
@TestForIssue(jiraKey = "HV-<n>")
```

using the user-provided JIRA key, or `TBD` if none exists.

---

## Annotation Processor

### 10. `TypeNames.java`

Add:

```text
HibernateValidatorTypes.NAME =
    ORG_HIBERNATE_VALIDATOR_CONSTRAINTS[.subpkg] + ".Name";
```

---

### 11. Annotation Processor `ConstraintHelper.java`

Register the supported types with:

```text
registerAllowedTypesForBuiltInConstraint(
    HibernateValidatorTypes.NAME,
    <allowed types>
);
```

The allowed types must exactly match the final Phase 2 decision.

---

### 12. Annotation Processor tests

Create:

```text
annotation-processor/src/test/java/org/hibernate/validator/ap/testmodel/ModelWith<Name>Constraints.java
```

with:
- one valid usage;
- one invalid usage.

Add the corresponding test to:

```text
.../ap/ConstraintValidationProcessorIT.java
```

Do this by default.

Skip it only when the supported target types cannot be readily exercised in an AP test model. If that happens, raise the omission as an explicit user decision and record why.

The existence of similar omissions in older constraints is not sufficient justification to skip this step.

---

## Documentation

### 13. `_ch02.adoc`

Add the constraint alphabetically under:

```text
[[validator-defineconstraints-hv-constraints]]
```

in "Additional constraints":

```text
`@Name(attr=)`:: <description>.
    Supported data types::: ...
    Hibernate metadata impact::: None
```

Do not add a changelog entry; the changelog is generated automatically at release time.

---

## Phase 5 — Verify

### Targeted verification first

Run the tests related to the new constraint first:

- Engine:
    - `<Name>ValidatorTest`
    - `PredefinedScopeAllConstraintsTest`
    - `MessagePropertiesTest`
    - `<Name>ConstrainedTest`
- Annotation processor:
    - `ConstraintValidationProcessorIT`, unless explicitly skipped

Fix failures before attempting the complete build.

### Full verification

Then run:

```text
mvn clean install
```

A clean full build with all applicable tests passing is the definition of **done**.

`clean` is the default because build plugins may clobber jars or output directories.

Only skip `clean` for a genuinely surgical single-test run.

---

## Artifacts

The workflow produces:

1. **Capabilities Summary** — 2–4 sentences describing the final constraint.
2. **Decision & Investigation Log** — research findings, decisions, rationale, deferred items, and security assessment.
3. **Draft Implementation** — the complete generated file set, presented as a proposal for inclusion.
4. **Verification Result** — targeted test results followed by full-build status.

Keep the Decision Log terse and skimmable; it is the primary design-review artifact.
---