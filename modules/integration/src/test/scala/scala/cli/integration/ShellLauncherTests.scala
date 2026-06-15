package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Properties

class ShellLauncherTests extends ScalaCliSuite {

  // downloading the launcher + a JVM + the compiler on a cold cache can take a while
  override def munitTimeout: Duration =
    10.minutes

  private lazy val launcherScript: os.Path = {
    val path = Option(System.getenv("SCALA_CLI_SHELL_LAUNCHER")).getOrElse {
      sys.error("SCALA_CLI_SHELL_LAUNCHER not set")
    }
    os.Path(path)
  }

  private def hasCachedLaunchers(cache: os.Path): Boolean =
    os.walk(cache).exists { p =>
      val subPath = p.subRelativeTo(cache)
      p.last.startsWith("scala-cli-") && subPath.segments.contains("github.com") && os.isFile(p)
    }

  if (!Properties.isWin)
    test("only the app output goes to stdout") {
      stdoutTest()
    }

  def stdoutTest(): Unit = {
    val appMessage = "Hello from the dummy app"
    val appRelPath = os.rel / "app.sc"

    TestInputs(
      appRelPath -> s"""println("$appMessage")"""
    ).fromRoot { root =>
      val cache = root / "cs-cache"
      os.makeDir.all(cache)

      // sanity check: the launcher really isn't in the cache to begin with
      expect(!hasCachedLaunchers(cache))

      def runLauncher(): os.CommandResult =
        os.proc(launcherScript, "run", "--server=false", appRelPath).call(
          cwd = root,
          env = Map("COURSIER_CACHE" -> cache.toString)
        )

      val res = runLauncher()

      // the launcher's own messages (e.g. "Downloading ...") must not leak to stdout
      expect(res.out.trim() == appMessage)
      // the launcher got downloaded under COURSIER_CACHE
      expect(hasCachedLaunchers(cache))

      // second run, with the launcher already present in the cache
      val res1 = runLauncher()
      expect(res1.out.trim() == appMessage)
    }
  }
}
