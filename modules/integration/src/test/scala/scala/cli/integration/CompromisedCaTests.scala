package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.util.UUID

import scala.concurrent.duration.*

/** Verifies that the native launcher doesn't trust the compromised eMudhra intermediate CAs
  * (CVE-2026-91403) when downloading dependencies.
  *
  * Native images embed the trust store (and the certificate checks) of the GraalVM used to build
  * them, so this can't be verified on the JDK alone. The JVM launcher runs on whatever JDK the user
  * picks, so it's not tested here.
  *
  * See https://www.scala-lang.org/blog/jdk-certificate-vulnerability.html
  */
class CompromisedCaTests extends ScalaCliSuite:
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First
  override def munitTimeout: Duration         = 5.minutes

  private val compromisedRepos =
    for
      validation <- List("dv", "ov", "ev")
      aaa        <- List("", "-aaa")
      generation <- List("g3b", "g3c")
    yield s"https://$validation-$generation$aaa-emudhra.we-have-a-problem.com/"

  // a repository with a legitimate certificate chain, which the launcher must trust
  private val controlRepo = "https://repo1.maven.org/maven2/"

  // how the JDK reports a chain with no trust anchor, or with an explicitly distrusted certificate
  private val rejectionMessages = Seq("PKIX path building failed", "Untrusted certificate")

  if TestUtil.isNativeCli then
    test("native launcher does not trust the compromised CAs"):
      TestInputs(os.rel / "Main.scala" -> "object Main").fromRoot: root =>
        // a dependency that doesn't exist, so that coursier tries to download it from every repository
        val dep         = s"com.example::nonexistent:0.0.0-${UUID.randomUUID()}"
        val repoOptions = (controlRepo :: compromisedRepos).flatMap(repo => Seq("-r", repo))
        val res         = os.proc(
          TestUtil.cli,
          "compile",
          ".",
          "--server=false",
          "--dep",
          dep,
          repoOptions
        ).call(
          cwd = root,
          check = false,
          mergeErrIntoOut = true,
          // a dedicated cache, so that nothing downloaded from the compromised hosts gets shared
          env = Map("COURSIER_CACHE" -> (root / "coursier-cache").toString)
        )
        val output = res.out.text()
        expect(res.exitCode != 0)
        // the TLS handshake with the control repository succeeded, it just doesn't have the dependency
        expect(output.linesIterator.exists(l =>
          l.contains("not found:") && l.contains(controlRepo)
        ))
        val failures = compromisedRepos.filterNot: repo =>
          output.linesIterator.exists: line =>
            line.contains(repo) && line.contains("download error:") &&
            rejectionMessages.exists(line.contains)
        assert(
          failures.isEmpty,
          s"""Compromised CAs are trusted by the launcher, or an unexpected error was encountered for:
             |${failures.mkString(System.lineSeparator())}
             |Output:
             |$output""".stripMargin
        )
