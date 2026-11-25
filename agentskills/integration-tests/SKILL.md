---
name: scala-cli-integration-tests
description: Add or run Scala CLI integration tests. Use when adding integration tests, debugging RunTests/CompileTests/etc., or working in modules/integration.
---

# Integration tests (Scala CLI)

**Location**: `modules/integration/`. Tests invoke the CLI as an external process.

**Run**: `./mill -i integration.test.jvm` (all). Filter: `./mill -i integration.test.jvm 'scala.cli.integration.RunTestsDefault.*'` or by test name. Native: `./mill -i integration.test.native`.

**Always set `CI=true` when running integration tests**, unless the user explicitly asks otherwise:

```bash
CI=true ./mill -i integration.test.jvm 'scala.cli.integration.RunTestsDefault.*'
```

This disables test parallelism, so the run mirrors how the tests behave on CI. It is slower, but the extra time is an acceptable trade-off for reproducible results.

**Structure**: `*TestDefinitions.scala` (abstract, holds test logic) → `*TestsDefault`, `*Tests213`, etc. (concrete, Scala version trait). Traits: `TestDefault`, `Test212`, `Test213`, `Test39Lts`, `Test33Lts`, `Test3NextRc`.

**Adding a test**:
1. Open the right `*TestDefinitions` (e.g. `RunTestDefinitions` for `run`).
2. Add `test("description") { … }` using `TestInputs(os.rel / "Main.scala" -> "…").fromRoot { root => … }` and `os.proc(TestUtil.cli, "run", …).call(cwd = root)`.
3. Assert on stdout/stderr.

**Helpers**: `TestInputs(...).fromRoot`, `TestUtil.cli`. Test groups (CI): `SCALA_CLI_IT_GROUP=1..6`; see `modules/integration/` for group mapping.

**Sanity batches**: with `SCALA_CLI_IT_SANITY=true` (macOS, static, bootstrapped and Scala 2.12/2.13 CI jobs; the Scala 2 jobs run everything with `[test_all]` and on non-PR builds), only tagged tests run. Tag via `test("foo".sanity)` (all such jobs), `.scala2Sanity` (Scala 2.12 & 2.13 jobs), `.scala212Sanity` or `.scala213Sanity`. Keep these lists very short; on Scala 2 jobs, tag only tests likely to catch Scala 2 bump issues (e.g. artifacts published per full Scala version, Scala 2-only code paths).
