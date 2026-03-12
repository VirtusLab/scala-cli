# AGENTS.md — Guidance for AI agents contributing to Scala CLI

This document is the primary reference for AI agents working on the Scala CLI codebase.
It describes the project structure, build system, testing, CI, and contribution patterns.

> **LLM Policy**: All AI-assisted contributions to this project must comply with
> the [LLM usage policy](https://github.com/scala/scala3/blob/HEAD/LLM_POLICY.md).
> The contributor (human) is responsible for every line of code submitted. State LLM usage
> clearly in the PR description. See also [LLM_POLICY.md](LLM_POLICY.md) in this repo.

## Human-facing docs

Read these for broader context; do not duplicate their content in your PRs:

- **[DEV.md](DEV.md)** — Developer setup, how to run the CLI from sources, run tests, generate launchers, IDE import,
  GraalVM reflection config, and more.
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — Fork-pull workflow, branch strategy (`main` for code, `stable` for
  docs-only), PR rules (single-purpose squash commits, formatting, import ordering, reference doc generation).
- **[INTERNALS.md](INTERNALS.md)** — High-level overview of modules, the `Inputs → Sources → Build` pipeline, source
  pre-processing (script wrapping), and build output post-processing.

## Build system

The project uses [Mill](https://mill-build.org/). Mill launchers ship with the repo (`./mill`). JVM 17 is required.

### Key build files

| File                            | Purpose                                                                                  |
|---------------------------------|------------------------------------------------------------------------------------------|
| `build.mill`                    | Root build definition: all module declarations, CI helper tasks, integration test wiring |
| `project/deps/package.mill`     | Dependency versions and definitions (`Deps`, `Scala`, `Java` objects)                    |
| `project/settings/package.mill` | Shared traits, utils (`HasTests`, `CliLaunchers`, `FormatNativeImageConf`, etc.)         |
| `project/publish/package.mill`  | Publishing settings                                                                      |
| `project/website/package.mill`  | Website-related build tasks                                                              |

### Essential commands

```bash
./mill -i scala …args…                            # Run Scala CLI from source
./mill -i __.compile                              # Compile everything
./mill -i unitTests                               # All unit tests
./mill -i integration.test.jvm                    # Integration tests (JVM launcher)
./mill -i integration.test.native                 # Integration tests (native launcher)
./mill -i 'module-name[].test'                    # Unit tests for a specific module
./mill -i 'generate-reference-doc[]'.run          # Regenerate reference docs
./mill -i __.fix                                  # Fix import ordering (scalafix)
scala-cli fmt .                                   # Format all code (scalafmt)
./mill -i __.formatNativeImageConf                # Format native-image reflection config
```

### Cross-compilation

Most modules are cross-compiled across Scala 3 versions. The cross axis is typically `Scala.scala3MainVersions`. The
default cross version for most modules is `Scala.defaultInternal`. When invoking a module, `[]` uses the default cross
version; `[_]` selects all versions.

## Project modules

Modules live under `modules/`. The dependency graph flows roughly as:

```
specification-level → config → core → options → directives → build-module → cli
```

### Module overview

The list below may not be exhaustive — check `modules/` and `build.mill` for the current set.

| Module                                        | Purpose                                                                                                          |
|-----------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `specification-level`                         | Defines `SpecificationLevel` (MUST / SHOULD / IMPLEMENTATION / RESTRICTED / EXPERIMENTAL) for SIP-46 compliance. |
| `config`                                      | Scala CLI configuration keys and persistence.                                                                    |
| `build-macros`                                | Compile-time macros (e.g. `EitherCps`).                                                                          |
| `core`                                        | Core types: `Inputs`, `Sources`, build constants, Bloop integration, JVM/JS/Native tooling.                      |
| `options`                                     | `BuildOptions`, `SharedOptions`, and all option types.                                                           |
| `directives`                                  | Using directive handlers — the bridge between `//> using` directives and `BuildOptions`.                         |
| `build-module` (aliased from `build` in mill) | The main build pipeline: preprocessing, compilation, post-processing. Most business logic lives here.            |
| `cli`                                         | Command definitions, argument parsing (CaseApp), the `ScalaCli` entry point. Packaged as the native image.       |
| `runner`                                      | Lightweight app that runs a main class and pretty-prints exceptions. Fetched at runtime.                         |
| `test-runner`                                 | Discovers and runs test frameworks/suites. Fetched at runtime.                                                   |
| `tasty-lib`                                   | Edits file names in `.tasty` files for source mapping.                                                           |
| `scala-cli-bsp`                               | BSP protocol types.                                                                                              |
| `integration`                                 | Integration tests (see dedicated section below).                                                                 |
| `docs-tests`                                  | Tests that validate documentation (`Sclicheck`).                                                                 |
| `generate-reference-doc`                      | Generates reference documentation from CLI option/directive metadata.                                            |

## Specification levels

Every command, CLI option, and using directive has a `SpecificationLevel`. This is central to how features are exposed.

| Level            | In the Scala Runner spec? | Available without `--power`? | Stability                       |
|------------------|---------------------------|------------------------------|---------------------------------|
| `MUST`           | Yes                       | Yes                          | Stable                          |
| `SHOULD`         | Yes                       | Yes                          | Stable                          |
| `IMPLEMENTATION` | No                        | Yes                          | Stable                          |
| `RESTRICTED`     | No                        | No (requires `--power`)      | Stable                          |
| `EXPERIMENTAL`   | No                        | No (requires `--power`)      | Unstable — may change/disappear |

**New features contributed by agents should generally be marked `EXPERIMENTAL`** unless the maintainers explicitly
request otherwise. This applies to new sub-commands, options, and directives alike.

The specification level is set via:

- **Directives**: `@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)` annotation on the directive case class.
- **CLI options**: `@Tag(tags.experimental)` annotation on option fields.
- **Commands**: Override `scalaSpecificationLevel` in the command class.

## Using directives

Using directives are in-source configuration comments:

```scala
//> using scala 3.6.4
//> using dep com.lihaoyi::os-lib:0.11.4
//> using test.dep org.scalameta::munit::1.1.1
```

### How they work

1. The `using_directives` library (`org.virtuslab:using_directives`) parses `//> using …` from source files.
2. `ExtractedDirectives` (in `build-module`) converts the AST into `StrictDirective` objects.
3. `DirectivesPreprocessor` matches directives to handlers and produces `BuildOptions` / `BuildRequirements`.
4. Directive-produced options are merged with CLI options. **CLI options always take priority** — they override
   directive values when both specify the same setting.
   4a. Some option values are merged with directives values, usually when a list is passed to them, 
       rather than a single value.

### Adding a new directive

1. Create a case class in `modules/directives/src/main/scala/scala/build/preprocessing/directives/` that extends one of:
    - `HasBuildOptions` — produces `BuildOptions` directly.
    - `HasBuildOptionsWithRequirements` — produces `BuildOptions` with scoped requirements (e.g. `test.dep`).
    - `HasBuildRequirements` — produces `BuildRequirements` (for `//> require` directives).

2. Annotate the class:
    - `@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)` — set the level.
    - `@DirectiveDescription("…")`, `@DirectiveUsage("…")`, `@DirectiveExamples("…")`.
    - `@DirectiveName("key")` on fields to define the directive key names.

3. Add a companion with `val handler: DirectiveHandler[YourDirective] = DirectiveHandler.derive`.

4. Register the handler in
   `modules/build/src/main/scala/scala/build/preprocessing/directives/DirectivesPreprocessingUtils.scala` in the
   appropriate list:
    - `usingDirectiveHandlers` — simple directives.
    - `usingDirectiveWithReqsHandlers` — directives with build requirements.
    - `requireDirectiveHandlers` — `//> require` directives.

5. Regenerate reference docs: `./mill -i 'generate-reference-doc[]'.run`.

## Testing

> **Every contribution that changes logic must include automated tests.** A PR without tests for
> new or changed behavior will not be accepted. If testing is truly infeasible, explain why in the
> PR description — but this should be exceptional.

> **Unit tests are always preferred over integration tests.** Unit tests are faster, more reliable,
> easier to debug, and cheaper to run on CI. Only add integration tests when the behavior cannot be
> adequately verified at the unit level (e.g. end-to-end CLI invocation, launcher-specific behavior,
> cross-process interactions).

> **Always re-run and verify tests locally before submitting.** After any logic change, run the
> relevant test suites on your machine and confirm they pass. Do not rely on CI to catch failures —
> CI resources are shared, and broken PRs waste maintainer time.

### Unit tests

Unit tests live inside individual modules as `test` submodules. The test framework is **munit**.

```bash
./mill -i 'build-module[].test'                                        # All build-module tests
./mill -i 'build-module[].test' 'scala.build.tests.BuildTestsScalac.*' # Filter by suite
./mill -i 'build-module[].test' 'scala.build.tests.BuildTests.simple'  # Filter by test name
./mill -i 'directives[].test'                                          # Directive tests
./mill -i 'options[].test'                                             # Options tests
./mill -i 'cli[].test'                                                 # CLI tests
./mill -i unitTests                                                    # All unit tests at once
```

Modules with unit tests include: `build-module`, `build-macros`, `cli`, `directives`, `options`, and others. Check for a
`test` object inside the module trait in `build.mill`.

**When adding a feature**, always start with unit tests. Unit tests for directives and build options live in
`modules/build/src/test/scala/scala/build/tests/` (e.g. `BuildTests.scala`, `DirectiveTests.scala`). CLI-level unit
tests live in `modules/cli/src/test/scala/`. Only reach for integration tests when a unit test cannot cover the
scenario.

### Integration tests

Integration tests live in `modules/integration/`. They invoke the Scala CLI launcher as an external process and verify
end-to-end behavior.

#### Structure

```
modules/integration/src/
├── main/scala/scala/cli/integration/
│   └── TestInputs.scala              # Helper: creates temp dirs with test source files
└── test/scala/scala/cli/integration/
    ├── *TestDefinitions.scala         # Abstract base: shared test logic
    ├── *TestsDefault.scala            # Concrete suite: default Scala 3
    ├── *Tests212.scala                # Concrete suite: Scala 2.12
    ├── *Tests213.scala                # Concrete suite: Scala 2.13
    ├── *Tests3Lts.scala               # Concrete suite: Scala 3 LTS
    ├── *Tests3NextRc.scala            # Concrete suite: Scala 3 next RC
    ├── TestUtil.scala                 # CLI invocation helpers, retry logic, platform checks
    ├── ScalaCliSuite.scala            # Base munit suite with group filtering
    └── util/                          # Bloop, Docker, compiler plugin helpers
```

#### Key patterns

- **`TestInputs`**: Creates temp directories with source files. Usage:
  `TestInputs(os.rel / "Main.scala" -> "…").fromRoot { root => … }`.
- **`TestUtil.cli`**: The CLI command to invoke. Built from system properties `test.scala-cli.path` and
  `test.scala-cli.kind`.
- **Test hierarchy**: `*TestDefinitions` (abstract, has the actual tests) → `*TestsDefault` / `*Tests213` / etc. (
  concrete, mixes in a Scala version trait).
- **Scala version traits**: `TestDefault`, `Test212`, `Test213`, `Test3Lts`, `Test3NextRc` — each sets `scalaVersionOpt`
  and `group`.

#### Running integration tests

```bash
./mill integration.test.jvm                                             # All, JVM launcher
./mill integration.test.jvm 'scala.cli.integration.RunTestsDefault.*'   # Filter by suite
./mill integration.test.jvm 'scala.cli.integration.RunTestsDefault.Multiple scripts'  # Single test
./mill integration.test.native                                          # Native launcher
```

Debugging the launcher during integration tests:

```bash
./mill integration.test.jvm 'scala.cli.integration.RunTestsDefault.*' --debug
./mill integration.test.jvm 'scala.cli.integration.RunTestsDefault.*' --debug:5006
```

#### Test groups and CI parallelism

Tests are split into groups by Scala version, controlled by `SCALA_CLI_IT_GROUP`:

| Group | Scala version          | Env var                |
|-------|------------------------|------------------------|
| 1     | Default (Scala 3 next) | `SCALA_CLI_IT_GROUP=1` |
| 2     | Scala 2.13             | `SCALA_CLI_IT_GROUP=2` |
| 3     | Scala 2.12             | `SCALA_CLI_IT_GROUP=3` |
| 4     | Scala 3 LTS            | `SCALA_CLI_IT_GROUP=4` |
| 5     | Scala 3 next RC        | `SCALA_CLI_IT_GROUP=5` |

On CI, each group runs as a separate job. Locally, all groups run by default (no env var filtering).

#### Adding a new integration test

1. Find the appropriate `*TestDefinitions` file (e.g. `RunTestDefinitions` for `run` command tests).
2. Add a test method using the munit `test("description") { … }` syntax.
3. Use `TestInputs(…).fromRoot { root => … }` to set up test files.
4. Invoke the CLI with `os.proc(TestUtil.cli, "run", extraOptions, …).call(cwd = root)`.
5. Assert on stdout/stderr using munit assertions.
6. The test will automatically run for each Scala version group that the `*TestsDefault` / `*Tests213` / etc. concrete
   suites cover.

### Documentation tests

Documentation is validated with the `Sclicheck` tool:

```bash
./mill -i 'docs-tests[]'.test                                    # All doc tests
./mill -i 'docs-tests[]'.test 'sclicheck.DocTests.command compile' # Specific doc
```

## Website and documentation

The website lives in `website/` and is built with [Docusaurus](https://v1.docusaurus.io/en/).

```
website/docs/
├── commands/          # Per-command documentation
├── cookbooks/         # How-to guides
├── guides/            # In-depth guides (advanced/, migration/, etc.)
├── reference/         # Auto-generated reference docs (options, directives, etc.)
├── getting_started.md
├── overview.md
└── release_notes.md
```

The `reference/` directory is **auto-generated** by `./mill -i 'generate-reference-doc[]'.run`. Do not edit files there
manually — edit the annotations/metadata in source code instead, then regenerate.

To build the website locally:

```bash
cd website && yarn && yarn build && npm run serve   # One-time build
cd website && yarn && yarn run start                # Dev server with hot reload
```

## CI overview

CI is defined in `.github/workflows/ci.yml`. It runs on push to `main`, on tags `v*`, and on PRs.

### Job categories

1. **Unit tests** (`unit-tests`): Compiles everything, runs all unit tests via `./mill -i unitTests`.
2. **JVM integration tests** (`jvm-tests-*`): Runs integration tests with the JVM launcher, one job per Scala version
   group (1–5).
3. **Native launcher generation** (`generate-linux-launcher`, `generate-macos-*-launcher`, `generate-windows-launcher`):
   Builds GraalVM native images.
4. **Native integration tests** (`native-linux-tests-*`, `native-macos-tests-*`, `native-windows-tests-*`): Runs
   integration tests against native launchers, per-platform and per-group.
5. **Static/mostly-static launchers** (`generate-static-launcher`, `generate-mostly-static-launcher`): Linux-specific
   statically linked binaries + Docker tests.
6. **Checks** (`checks`): Scala/JS version checks, native-image config format, Ammonite availability, cross-version
   dependency conflicts, scalafix import ordering.
7. **Format** (`format`): `scala-cli fmt . --check`.
8. **Reference doc** (`reference-doc`): Verifies reference docs are up-to-date.
9. **Docs tests** (`docs-tests`): Builds the website and runs `Sclicheck` documentation validation.
10. **Publish** (`publish`): Only on push to `main` with all tests green. Publishes artifacts to Sonatype/GitHub.

### What you must ensure before submitting a PR

- Code compiles: `./mill -i __.compile`
- Code is formatted: `scala-cli fmt .` (or `scalafmt`)
- Imports are ordered: `./mill -i __.fix`
- Reference docs are up-to-date: `./mill -i 'generate-reference-doc[]'.run`
- Relevant unit tests pass
- Relevant integration tests pass (at minimum the `*Default` group locally)

## Typical contribution workflow

A typical contribution addresses an issue at https://github.com/VirtusLab/scala-cli/issues.

1. **Understand the issue**: Read the issue description, labels, and any linked discussion.
2. **Branch from `main`** (or `stable` for docs-only changes).
3. **Implement the change**: Follow the module structure and patterns described above.
4. **Add tests**: Tests are mandatory for any logic change. Start with unit tests — they should be the default. Add integration tests only when unit tests cannot cover the scenario. Both where applicable.
5. **Run tests locally**: Re-run all relevant test suites after every logic change and verify they pass. This is not optional — do not submit code with untested or unverified changes.
6. **Update docs**: If the feature is user-facing, add/update docs in `website/docs/`.
7. **Regenerate reference docs**: `./mill -i 'generate-reference-doc[]'.run` if options or directives changed.
8. **Format and fix**: `scala-cli fmt .` and `./mill -i __.fix`.
9. **Final local verification**: Run the full relevant test suites one more time to confirm nothing is broken.
10. **Submit PR**: Fill in the PR template. State LLM usage. Single-purpose squash commits.

## Code style

**Code style is taken seriously in this project. PRs that ignore these guidelines will require revision.**

Write idiomatic **Scala 3** code. The codebase targets the latest Scala 3 LTS; prefer syntax and features available
there over legacy Scala 2 patterns.

### Scala 3 idioms to prefer

- **New control syntax**: `if … then … else`, `for … do`, `for … yield`, `while … do` (no parentheses around conditions).
- **Braceless syntax**: Indentation-based blocks where the existing code already uses them. Follow the local style of the file you're editing.
- **`enum`** over sealed trait + case object hierarchies where appropriate.
- **`extension` methods** over implicit classes.
- **`given` / `using`** over `implicit val` / `implicit def`.
- **`export`** clauses over forwarder methods.
- **Top-level definitions** when a wrapper object serves no purpose.
- **Union / intersection types** when they simplify type signatures.

### Functional style

- **Prefer immutability**: Use `val`, immutable collections, and copy-on-write (`case class` `.copy()`) by default.
  Reach for `var` or mutable collections only when there is a clear performance or clarity reason.
- **Prefer expressions over statements**: `if`/`match` as expressions returning values rather than imperative
  if-then-assign patterns.
- **Prefer `map` / `flatMap` / `fold` / `collect`** and other combinators over manual loops.
- **Use `@tailrec`** on any recursive method that can be made tail-recursive. The annotation both documents intent and
  lets the compiler verify the optimisation. Import it from `scala.annotation.tailrec`.
- **Avoid `null`**: Use `Option`, or `Either` / custom error ADTs for failure paths. The codebase uses `EitherCps`
  (from `build-macros`) for monadic error handling in places — prefer that over exception-based control flow.
- **Favour `for`-comprehensions** (`for … yield`) over deeply nested `flatMap` chains when readability improves.
- **Keep functions small and focused**: Extract meaningful helper methods rather than writing long method bodies.

### Avoid code duplication

When you see the same logic repeated across modules or command implementations, extract it into a shared trait or utility
method. Common patterns in this codebase include shared `*Options` traits, helper methods in companion objects, and
utility modules (e.g. `CommandHelpers`, `TestUtil`). Before duplicating code, check whether a suitable abstraction
already exists — and if not, create one.

### Logging and output

**Always use the project `Logger` abstraction for user-facing output.** Never write directly to `System.err` or
`System.out` — the logger respects the user-configured verbosity level and ensures consistent formatting.

The `Logger` trait (`scala.build.Logger`) provides verbosity-aware methods:

| Method                  | Shown when                |
|-------------------------|---------------------------|
| `logger.error(msg)`     | Always                    |
| `logger.message(msg)`   | Verbosity >= 0 (default)  |
| `logger.log(msg)`       | Verbosity >= 1 (`-v`)     |
| `logger.debug(msg)`     | Verbosity >= 2 (`-v -v`)  |
| `logger.diagnostic(…)`  | Structured diagnostics    |

In CLI commands the logger is available from `LoggingOptions` (e.g. `options.shared.logging.logger`). In build-level
code it is threaded as a parameter. In tests use `TestLogger`. Direct `System.err.println` / `System.out.println` calls
bypass verbosity settings, break `--quiet` mode, and make output inconsistent — avoid them.

### When mutability is acceptable

Mutable state is fine in hot paths (e.g. tight loops in `tasty-lib`, byte-level manipulation in `asm` post-processing),
or when interfacing with Java APIs that require it. In those cases, keep the mutable scope as narrow as possible and
document why mutability is used if it's not obvious from context.

## GraalVM native image

Scala CLI is distributed as a GraalVM native image. Reflection configuration lives in
`modules/cli/src/main/resources/META-INF/native-image/`. If you add code that uses reflection (or update dependencies
that do), you may need to update `reflect-config.json`. See [DEV.md](DEV.md) for the `runWithAssistedConfig` workflow
and format verification (`./mill -i __.formatNativeImageConf`).

## Additional notes

- **Build-time constants**: Many modules generate a `Constants.scala` at build time (via `constantsFile` in
  `build.mill`). These contain version strings, artifact coordinates, etc. Do not hardcode such values — use or extend
  the generated constants.
- **`local-repo`**: The `runner` and `test-runner` modules are packaged into a local repository JAR and embedded in the
  CLI. If you modify these modules, set `developingOnStubModules = true` in `build.mill` `local-repo` for watch mode to
  pick up changes.
- **Power mode**: Features behind `--power` include RESTRICTED and EXPERIMENTAL levels. When adding a new feature that
  is not part of the Scala Runner Specification, gate it behind EXPERIMENTAL and require `--power`.
- **`ShadowingSeq`**: When CLI options and directives both specify the same value (e.g. a dependency), the CLI option
  wins. This is implemented via `ShadowingSeq` which deduplicates by key, keeping the first occurrence.
