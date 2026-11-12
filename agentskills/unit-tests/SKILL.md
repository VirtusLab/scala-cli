---
name: scala-cli-unit-tests
description: Add or run Scala CLI unit tests (munit). Use when adding unit tests to any module, adding a test module, writing suites that compile code with Bloop, or debugging unit test failures/timeouts.
---

# Unit tests (Scala CLI)

Unit tests are preferred over integration tests (see `AGENTS.md`). They use **munit**.

**Location**: a module's unit tests live in its `test` submodule (`object test extends ScalaCliTests`
in `build.mill`), with sources under `modules/<module>/src/test/scala/`. Check `modules/*/src/test`
for the modules which currently have tests.

**Adding tests to a module without a test module**: add `object test extends ScalaCliTests` to the
module in `build.mill`, and make sure the CI aggregate `unitTests` (in `build.mill`) runs it.

**Base suites**: if the module's tests have a shared base suite (e.g. `TestUtil.ScalaCliBuildSuite`
in `build-module`, `cli.tests.TestUtil.ScalaCliSuite` in `cli`), extend it. Base suites set the
per-test timeout (`munitTimeout`) and log the start/end of every test. Otherwise extend
`munit.FunSuite`. If several suites in a module need the same setup, extract a base suite or a helper
instead of copying it. Check the existing test helpers of the module and of its dependencies first.

**Run**:

```bash
./mill -i unitTests                                         # all (what CI runs)
./mill -i '<module>[].test'                                 # a single module, e.g. 'options[].test'
./mill -i '<module>[].test' 'scala.build.tests.MySuite.*'   # a single suite
./mill -i '<module>[].test' 'scala.build.tests.MySuite.foo' # a single test
```

Selecting a single test by name only works for names without spaces. For other names, run the
whole suite. The Mill module of `modules/build` is `build-module`.

**Logging**: use the project `Logger` (in tests, e.g. `scala.build.tests.TestLogger`, or
`RecordingLogger` to assert on messages), never `System.out`/`System.err`.

## Suites compiling with Bloop: mix in `BloopServer.Suite`

The `build-module` test helpers (`TestInputs(…).withBuild`, `.withLoadedBuild`, `.withBuilds`, …)
compile with **Bloop** when given `Some(BloopServer.bloopConfig)`, and with plain scalac when given
`None`. They can be used from any test module depending on the `build-module` tests (add
`` `build-module`(crossScalaVersion).test `` to the test module's `moduleDeps`, as `cli` does).
**Every suite that passes a Bloop config, in whichever module, must mix in
`scala.build.tests.util.BloopServer.Suite`**:

```scala
import scala.build.tests.util.BloopServer

class MyTests extends munit.FunSuite with BloopServer.Suite {
  def bloopConfigOpt = Some(BloopServer.bloopConfig)
  // …
}
```

Why: the mixin adds a suite fixture which starts the shared Bloop server before the tests run
(with retries). Without it, the first Bloop build in the JVM starts the server inside a test.
Starting it can get stuck on busy CI runners, and bloop-rifle only gives up after
`startCheckTimeout + 30s` (150s on CI). That is longer than the per-test timeout, so the test fails
before the build can retry. munit doesn't apply the per-test timeout to fixtures, so the fixture can
retry safely.

- Only mix it into suites that actually use Bloop. It starts a long-lived Bloop daemon, which suites
  that build with `None` or don't build at all don't need.
- For a suite with a Bloop and a scalac variant (e.g. `BuildTests(server: Boolean)`), mix it into the
  Bloop variant only (`BuildTestsBloop`).
- If you add Bloop builds to an existing suite that has no mixin yet, add the mixin.
