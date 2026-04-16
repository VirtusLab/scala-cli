# AGENTS.md — Guidance for AI agents contributing to Scala CLI

Short reference for AI agents. For task-specific guidance (directives, integration tests), load skills from *
*[agentskills/](agentskills/)** when relevant.

> **LLM Policy**: All AI-assisted contributions must comply with the
> [LLM usage policy](https://github.com/scala/scala3/blob/HEAD/LLM_POLICY.md). The contributor (human) is responsible
> for every line. State LLM usage in the PR description. See [LLM_POLICY.md](LLM_POLICY.md).

## Human-facing docs

- **[DEV.md](DEV.md)**                   — Setup, run from source, tests, launchers, GraalVM.
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — PR workflow, formatting, reference doc generation.
- **[INTERNALS.md](INTERNALS.md)**       — Modules, `Inputs → Sources → Build`, preprocessing.

## Build system

The project uses [Mill](https://mill-build.org/). Mill launchers ship with the repo (`./mill`). JVM 17 required.
Cross-compilation: default `Scala.defaultInternal`; `[]` = default version, `[_]` = all.

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
./mill -i clean                                                           # Clean Mill context
./mill -i scala …args…                                                    # Run Scala CLI from source
./mill -i __.compile                                                      # Compile everything
./mill -i unitTests                                                       # All unit tests
./mill -i 'build-module[].test'                                           # Unit tests for a specific module
./mill -i 'build-module[].test' 'scala.build.tests.BuildTestsScalac.*'    # Filter by suite
./mill -i 'build-module[].test' 'scala.build.tests.BuildTests.simple'     # Single test by name
./mill -i integration.test.jvm                                            # Integration tests (JVM launcher)
./mill -i integration.test.jvm 'scala.cli.integration.RunTestsDefault.*'  # Integration: filter by suite
./mill -i 'generate-reference-doc[]'.run                                  # Regenerate reference docs
./mill -i __.fix                                                          # Fix import ordering (scalafix)
scala-cli fmt .                                                           # Format all code (scalafmt)
```

## Project modules

Modules live under `modules/`. The dependency graph flows roughly as:

```
specification-level → config → core → options → directives → build-module → cli
                                                    ↑
                                          directives-parser
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
| `directives-parser`                           | Pure Scala 3 parser for `//> using` directive syntax: comment extraction, lexing, and parsing into AST nodes.    |
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
//> using scala 3
//> using dep com.lihaoyi::os-lib:0.11.4
//> using test.dep org.scalameta::munit::1.1.1
```

Directives are parsed by the `directives-parser` module (`CommentExtractor` → `Lexer` → `Parser`), then
`ExtractedDirectives` → `DirectivesPreprocessor` → `BuildOptions`/`BuildRequirements`. **CLI options override directive
values.** To add a new directive, see [agentskills/adding-directives/](agentskills/adding-directives/SKILL.md).

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

**Unit tests**: munit, in each module’s `test` submodule. Run commands above; add tests in `modules/build/.../tests/` or
`modules/cli/src/test/scala/`. Prefer unit over integration.

**Integration tests**: `modules/integration/`; they run the CLI as a subprocess.
See [agentskills/integration-tests/](agentskills/integration-tests/SKILL.md) for structure and how to add tests.

## Pre-PR checklist

1. Code compiles: `./mill -i __.compile`
2. Tests added and passing locally (unit tests first, integration if needed)
3. Code formatted: `scala-cli fmt .`
4. Imports ordered: `./mill -i __.fix`
5. Reference docs regenerated (if options/directives changed): `./mill -i 'generate-reference-doc[]'.run`
6. PR template filled, LLM usage stated

## Code style

Code style is enforced.

**Scala 3**: Prefer `if … then … else`, `for … do`/`yield`, `enum`, `extension`, `given`/`using`, braceless blocks,
top-level defs. Use union/intersection types when they simplify signatures. Always favor Scala 3 idiomatic syntax.

**Functional**: Prefer `val`, immutable collections, `case class`.copy(). Prefer expressions over statements; prefer
`map`/`flatMap`/`fold`/`for`-comprehensions over loops. Use `@tailrec` for tail recursion. Avoid `null`; use `Option`/
`Either`/`EitherCps` (build-macros). Keep functions small; extract helpers.

**No duplication**: Extract repeated logic into shared traits or utils (`*Options` traits, companion helpers,
`CommandHelpers`, `TestUtil`). Check for existing abstractions before copying.

**Logging**: Use the project `Logger` only — never `System.err` or `System.out`. Logger respects verbosity (`-v`, `-q`).
Use `logger.message(msg)` (default), `logger.log(msg)` (verbose), `logger.debug(msg)` (debug), `logger.error(msg)` (
always). In commands: `options.shared.logging.logger`; in build code it is passed in; in tests use `TestLogger`.

**Mutability**: OK in hot paths or when a Java API requires it; keep scope minimal.

## Further reference

[DEV.md](DEV.md), [CONTRIBUTING.md](CONTRIBUTING.md), [INTERNALS.md](INTERNALS.md).
