package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

class ReplTestsDefault extends ReplTestDefinitions
    with ReplAmmoniteTestDefinitions
    with ReplAmmoniteTests3StableDefinitions
    with TestDefault {
  if (TestUtil.isNativeCli)
    test("not download java 17 when run repl without sources") {
      TestUtil.retryOnCi() {
        TestInputs.empty.fromRoot { root =>
          val java8Home =
            os.Path(os.proc(TestUtil.cs, "java-home", "--jvm", "zulu:8").call().out.trim(), os.pwd)

          val res =
            os.proc(TestUtil.cli, "--power", "repl", TestUtil.extraOptions, "--", "-version").call(
              cwd = root,
              mergeErrIntoOut = true,
              env = Map(
                "JAVA_HOME"              -> java8Home.toString,
                "COURSIER_ARCHIVE_CACHE" -> (root / "archive-cache").toString(),
                "COURSIER_CACHE"         -> (root / "cache").toString(),
                "PATH" -> ((java8Home / "bin").toString + File.pathSeparator + System.getenv(
                  "PATH"
                ))
              )
            )

          val output = res.out.trim().toLowerCase()

          expect(!output.contains("jdk17"))
          expect(!output.contains("jvm-index"))
        }
      }
    }
}
