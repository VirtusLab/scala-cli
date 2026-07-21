package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ReplTestsDefault extends ReplTestDefinitions
    with ReplJShellTestDefinitions
    with LazyValTests
    with TestDefault {

  // Sloth tests - only in default suite since they use hardcoded Scala versions
  private val latestJava = Constants.allJavaVersions.max.toString

  private def replNoDepUnsafeTest(slothFlag: String): Unit =
    test(
      s"$runInReplPrefix dont warn about sun.misc.Unsafe on JDK $latestJava (no dependency, $slothFlag)"
    ) {
      val expectedMessage = "Hello"
      val code            = s"""println("$expectedMessage")"""
      TestInputs.empty.fromRoot { root =>
        val res = os.proc(
          TestUtil.cli,
          "repl",
          "--repl-quit-after-init",
          "--repl-init-script",
          code,
          "--jvm",
          latestJava,
          "--power",
          slothFlag,
          extraOptions
        ).call(cwd = root, stderr = os.Pipe)
        expect(res.out.trim().contains(expectedMessage))
        expect(!res.err.trim().contains("sun.misc.Unsafe"))
      }
    }

  private def replLazyValsUnsafeTest(slothFlag: String): Unit =
    test(
      s"$runInReplPrefix 3.3 lazy vals dont warn about sun.misc.Unsafe on JDK $latestJava ($slothFlag)"
    ) {
      val expectedMessage = "Hello"
      val code            = "println(lazyvalslib.LazyValsLib.greeting)"
      TestInputs.empty.fromRoot { root =>
        val (dep, repoDir) = publishLazyValsLib(Constants.scala3Lts, root)
        val res            = os.proc(
          TestUtil.cli,
          "repl",
          ".",
          "--repl-quit-after-init",
          "--repl-init-script",
          code,
          extraOptions,
          "--power",
          slothFlag,
          "--dep",
          dep,
          "--repository",
          repoDir.toNIO.toUri.toASCIIString,
          "--jvm",
          latestJava
        ).call(cwd = root, stderr = os.Pipe)
        expect(res.out.trim().contains(expectedMessage))
        expect(!res.err.trim().contains("sun.misc.Unsafe"))
      }
    }

  for slothFlag <- Seq("--sloth", "--sloth-agent") do
    replNoDepUnsafeTest(slothFlag)
    replLazyValsUnsafeTest(slothFlag)

  if canRunInRepl then
    for { nightlyTag <- List("3.nightly", "nightly") }
      test(
        s"$runInReplPrefix $nightlyTag returns the same Scala version as <latest-minor>.nightly"
      ) {
        val code = """println(scala.util.Properties.versionNumberString)"""
        runInRepl(code, cliOptions = Seq("-S", nightlyTag)) { r1 =>
          val version1 = r1.out.trim()
          System.err.println(s"$nightlyTag returns the following nightly: $version1")
          val nightlyPrefix = version1.split('.').take(2).mkString(".")
          runInRepl(code, cliOptions = Seq("-S", s"$nightlyPrefix.nightly")) { r2 =>
            val version2 = r2.out.trim()
            System.err.println(s"$nightlyPrefix.nightly returns the following nightly: $version2")
            expect(version1 == version2)
            val major = version1.split('.').take(1).head.toInt
            expect(major == 3)
            val minor = version1.split('.').take(2).last.toInt
            expect(minor >= Constants.scala3NextPrefix.split('.').last.toInt)
          }
        }
      }
}
