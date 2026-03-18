---
name: scala-cli-integration-tests
description: Add or run Scala CLI integration tests. Use when adding integration tests, debugging RunTests/CompileTests/etc., or working in modules/integration.
---

# Integration tests (Scala CLI)

**Location**: `modules/integration/`. Tests invoke the CLI as an external process.

**Run**: `./mill -i integration.test.jvm` (all). Filter: `./mill -i integration.test.jvm 'scala.cli.integration.RunTestsDefault.*'` or by test name. Native: `./mill -i integration.test.native`.

**Structure**: `*TestDefinitions.scala` (abstract, holds test logic) → `*TestsDefault`, `*Tests213`, etc. (concrete, Scala version trait). Traits: `TestDefault`, `Test212`, `Test213`, `Test3Lts`, `Test3NextRc`.

**Adding a test**:
1. Open the right `*TestDefinitions` (e.g. `RunTestDefinitions` for `run`).
2. Add `test("description") { … }` using `TestInputs(os.rel / "Main.scala" -> "…").fromRoot { root => … }` and `os.proc(TestUtil.cli, "run", …).call(cwd = root)`.
3. Assert on stdout/stderr.

**Helpers**: `TestInputs(...).fromRoot`, `TestUtil.cli`. Test groups (CI): `SCALA_CLI_IT_GROUP=1..5`; see `modules/integration/` for group mapping.
