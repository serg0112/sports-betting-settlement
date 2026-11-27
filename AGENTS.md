# Working agreement

- Follow the explicit exercise contract and the project's existing conventions.
- Keep confirmed requirements, assumptions, and open questions separate.
- Ask about an ambiguity only when it changes observable behavior or a guarantee.
- Implement the smallest solution that satisfies the agreed scope.
- Use the project's Java version and verified build/test commands.
- Use single-threaded, in-memory state unless concurrency or persistence is agreed.
- Add frameworks, dependencies, layers, or infrastructure only when required.
- Keep tenant state isolated and time-dependent behavior deterministic in tests.
- Derive tests from agreed examples and boundary behavior, including repeated calls.
- Do not weaken a test just to pass it; flag conflicts with the contract.
- Review actual changes and run the relevant available checks.
- Report commands run, observed results, and limitations accurately.
- Discuss production extensions separately from the requested implementation.


# Spring Boot and Java coding standards

Applies to this directory and its subdirectories.

## Clean Code and DRY

- Write simple, readable code with descriptive names, focused methods, and cohesive classes.
- Give each component a clear responsibility. Separate HTTP concerns, application behavior,
  business rules, and persistence.
- Keep each business rule and default authoritative in one place. Reuse shared behavior
  instead of copying it; distinguish HTTP input validation from domain invariants.
- Introduce abstractions when they express a useful boundary or remove meaningful duplication.
  Avoid speculative frameworks, unnecessary interfaces, and unrelated refactoring.
- Prefer explicit dependencies, immutable values, and straightforward control flow.

## Packages and architecture

- Organize packages by layer: `controller`, `service`, `model`, `repository`, `entity`,
  `exception`, and `config`. Create only the packages a feature needs.
- Put request/response records in `model`; HTTP mappings and transport conversion in
  `controller`; application coordination in `service`; persistence access in `repository`;
  and persistence entities in `entity`.
- Keep reusable business logic in a clearly named, framework-independent package.
- Controllers delegate application behavior to services. Keep services independent of
  controllers, servlet APIs, and HTTP request/response DTOs; pass business inputs and results.
- Use focused conversion helpers or mappers where needed. Keep persistence entities out of
  public responses and prevent business logic from depending on transport models.
- Place the application entry point above component packages for component scanning.

## Java and Spring conventions

- Use four-space indentation, standard Java naming, and explicit imports without wildcards.
- Use constructor injection and final fields for required dependencies. Avoid field injection.
- Prefer records for structured request/response DTOs and immutable value objects.
  Defensively copy mutable collections when ownership crosses component boundaries.
- Keep mutable shared state deliberate and thread-safe. Validate replacement state before
  publishing it, and use consistent snapshots for operations that read related settings.
- Use `BigDecimal` for monetary calculations, explicit scale and rounding rules, and exact
  decimal inputs rather than binary floating-point constants.
- Use APIs and namespaces appropriate to the configured Java and Spring Boot versions.
  Treat the build and wrapper as version sources of truth and prefer Boot-managed dependencies.
- Externalize environment-specific configuration. Keep credentials and secrets out of source,
  client responses, and logs.

## HTTP APIs and errors

- Use clear resource paths, appropriate HTTP methods and statuses, and explicit payloads.
  Preserve existing contracts unless the task intentionally changes them.
- Validate incoming DTOs with Jakarta Validation, including nested values when applicable.
  Enforce business invariants in the domain so non-HTTP callers receive the same protection.
- Use specific exceptions for expected failures and centralized HTTP exception handling.
  Do not turn arbitrary programming exceptions into client errors.
- Return safe, consistent `ProblemDetail` errors, with field errors for invalid DTOs.
  Keep stack traces, SQL, credentials, and implementation details out of responses.

## Persistence, when applicable

- Define transaction boundaries in services; use read-only transactions for suitable reads.
- Enforce database invariants with constraints and handle known conflicts specifically.
- Use versioned schema migrations. Preserve applied migrations and avoid destructive database
  operations or volume deletion without explicit authorization.
- Fetch required data and perform entity-to-business mapping within the transaction;
  avoid relying on an open persistence session during HTTP serialization.

## Verification and delivery

- Use the checked-in build wrapper and the JDK required by the build configuration.
- Verify changed behavior with focused tests and run the full build for application changes.
  Keep tests independent and deterministic, and use runtime-assigned ports for HTTP tests.
- Choose the simplest useful verification: unit tests for business rules and integration or
  end-to-end tests for actual component and HTTP behavior. Cover relevant failure cases.
- For API changes, run the application and inspect real HTTP statuses, content types, and
  bodies. A running-server HTTP test can provide this verification.
- For instruction-only or documentation-only changes, check accuracy and consistency;
  application builds are unnecessary unless executable behavior also changes.
- Honor explicit task constraints, preserve unrelated work, and update affected documentation.
- Stop verification services started for the task, preserve pre-existing services and data,
  and report changes, checks, observed results, and any blockers or skipped verification.
