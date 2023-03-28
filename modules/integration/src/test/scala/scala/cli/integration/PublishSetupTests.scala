package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import com.virtuslab.using_directives.config.Settings
import com.virtuslab.using_directives.custom.model.UsingDirectiveKind
import com.virtuslab.using_directives.reporter.ConsoleReporter
import com.virtuslab.using_directives.{Context, UsingDirectivesProcessor}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish

import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

class PublishSetupTests extends ScalaCliSuite {

  private def ghUserName = "foo"
  private def projName   = "project-name"
  private def devName    = "Alex Test"
  private def devMail    = "alex@alex.me"
  private def devUrl     = "https://alex.me"
  private def password   = "password"

  private def configSetup(configFile: os.Path, root: os.Path): Unit = {
    val envs = Map("SCALA_CLI_CONFIG" -> configFile.toString)
    os.proc(TestUtil.cli, "--power", "config", "publish.user.name", devName)
      .call(cwd = root, stdout = os.Inherit, env = envs, stderr = os.Pipe)
    os.proc(TestUtil.cli, "--power", "config", "publish.user.email", devMail)
      .call(cwd = root, stdout = os.Inherit, env = envs, stderr = os.Pipe)
    os.proc(TestUtil.cli, "--power", "config", "publish.user.url", devUrl)
      .call(cwd = root, stdout = os.Inherit, env = envs, stderr = os.Pipe)
    os.proc(
      TestUtil.cli,
      "--power",
      "config",
      "publish.credentials",
      "s01.oss.sonatype.org",
      "value:uSeR",
      "value:1234"
    )
      .call(cwd = root, stdout = os.Inherit, env = envs, stderr = os.Pipe)
    os.proc(TestUtil.cli, "--power", "config", "--create-pgp-key", "--pgp-password", "random")
      .call(cwd = root, stdout = os.Inherit, env = envs, stderr = os.Pipe)
  }

  private val projDir = os.rel / projName
  private val testInputs = TestInputs(
    os.rel / projDir / "Foo.scala" ->
      """object Foo
        |""".stripMargin
  )
  private val configFile = os.rel / "config" / "config.json"
  private val envs       = Map("SCALA_CLI_CONFIG" -> configFile.toString)

  private def gitInit(dir: os.Path): Git = {
    val git = Git.init().setDirectory(dir.toIO).call()
    git
      .remoteAdd()
      .setName("origin")
      .setUri(new URIish(s"https://github.com/$ghUserName/tests.git"))
      .call()
    git
  }

  private def directives(content: String): Map[String, Seq[String]] = {
    val reporter = new ConsoleReporter
    val processor = {
      val settings = new Settings
      settings.setAllowStartWithoutAt(true)
      settings.setAllowRequire(false)
      val context = new Context(reporter, settings)
      new UsingDirectivesProcessor(context)
    }

    val usedDirectives = processor
      .extract(content.toCharArray, true, true)
      .asScala
      .find(_.getKind == UsingDirectiveKind.SpecialComment)
      .get

    usedDirectives
      .getFlattenedMap
      .asScala
      .toSeq
      .map {
        case (k, l) =>
          (k.getPath.asScala.mkString("."), l.asScala.toSeq.map(_.toString))
      }
      .toMap
  }

  test("local") {
    val expectedDirectives = Map(
      "publish.versionControl" -> Seq(s"github:$ghUserName/tests"),
      "publish.organization"   -> Seq(s"io.github.$ghUserName"),
      "publish.developer"      -> Seq(s"$devName|$devMail|$devUrl"),
      "publish.repository"     -> Seq("central-s01"),
      "publish.url"            -> Seq(s"https://github.com/$ghUserName/tests"),
      "publish.name"           -> Seq(projName),
      "publish.computeVersion" -> Seq("git:tag"),
      "publish.license"        -> Seq("Apache-2.0")
    )
    val expectedGhSecrets = Set.empty[String]
    testInputs.fromRoot { root =>
      configSetup(root / configFile, root)
      gitInit(root / projDir)
      val res = os.proc(TestUtil.cli, "--power", "publish", "setup", "--dummy", projDir).call(
        cwd = root,
        mergeErrIntoOut = true,
        env = envs
      )

      val ghSecrets = res.out.text()
        .linesIterator
        .filter(_.startsWith("Would have set GitHub secret "))
        .map(_.stripPrefix("Would have set GitHub secret "))
        .toSet
      val directives0 = directives(os.read(root / projDir / "publish-conf.scala"))
      expect(directives0 == expectedDirectives)
      expect(ghSecrets == expectedGhSecrets)
      expect(res.out.text().contains("found keys in config"))
    }
  }

  test("CI") {
    val expectedDirectives = Map(
      "publish.versionControl"    -> List(s"github:$ghUserName/tests"),
      "publish.organization"      -> List(s"io.github.$ghUserName"),
      "publish.developer"         -> List(s"$devName|$devMail|$devUrl"),
      "publish.name"              -> List(projName),
      "publish.license"           -> List("Apache-2.0"),
      "publish.url"               -> List(s"https://github.com/$ghUserName/tests"),
      "publish.ci.secretKey"      -> List("env:PUBLISH_SECRET_KEY"),
      "publish.ci.user"           -> List("env:PUBLISH_USER"),
      "publish.ci.password"       -> List("env:PUBLISH_PASSWORD"),
      "publish.ci.publicKey"      -> List("env:PUBLISH_PUBLIC_KEY"),
      "publish.ci.repository"     -> List("central-s01"),
      "publish.ci.computeVersion" -> List("git:tag")
    )
    val expectedGhSecrets =
      Set(
        "PUBLISH_USER",
        "PUBLISH_PASSWORD",
        "PUBLISH_SECRET_KEY",
        "PUBLISH_PUBLIC_KEY"
      )
    testInputs.fromRoot { root =>
      configSetup(root / configFile, root)
      gitInit(root / projDir)
      val res =
        os.proc(TestUtil.cli, "--power", "publish", "setup", "--ci", "--dummy", projDir).call(
          cwd = root,
          mergeErrIntoOut = true,
          env = envs
        )

      val ghSecrets = res.out.text()
        .linesIterator
        .filter(_.startsWith("Would have set GitHub secret "))
        .map(_.stripPrefix("Would have set GitHub secret "))
        .toSet
      val directives0 = directives(os.read(root / projDir / "publish-conf.scala"))
      expect(directives0 == expectedDirectives)
      expect(ghSecrets == expectedGhSecrets)
    }
  }

  test("local GitHub") {
    val expectedDirectives = Map(
      "publish.versionControl" -> Seq(s"github:$ghUserName/tests"),
      "publish.organization"   -> Seq(s"io.github.$ghUserName"),
      "publish.developer"      -> Seq(s"$devName|$devMail|$devUrl"),
      "publish.repository"     -> Seq("github"),
      "publish.url"            -> Seq(s"https://github.com/$ghUserName/tests"),
      "publish.name"           -> Seq(projName),
      "publish.computeVersion" -> Seq("git:tag"),
      "publish.license"        -> List("Apache-2.0")
    )
    val expectedGhSecrets = Set.empty[String]
    testInputs.fromRoot { root =>
      configSetup(root / configFile, root)
      gitInit(root / projDir)
      val res = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "setup",
        "--dummy",
        "--publish-repository",
        "github",
        projDir
      ).call(
        cwd = root,
        mergeErrIntoOut = true,
        env = envs
      )

      val ghSecrets = res.out.text()
        .linesIterator
        .filter(_.startsWith("Would have set GitHub secret "))
        .map(_.stripPrefix("Would have set GitHub secret "))
        .toSet
      val directives0 = directives(os.read(root / projDir / "publish-conf.scala"))
      expect(directives0 == expectedDirectives)
      expect(ghSecrets == expectedGhSecrets)
    }
  }

  test("CI GitHub") {
    val expectedDirectives = Map(
      "publish.versionControl"    -> List(s"github:$ghUserName/tests"),
      "publish.organization"      -> List(s"io.github.$ghUserName"),
      "publish.ci.user"           -> List("env:PUBLISH_USER"),
      "publish.developer"         -> List(s"$devName|$devMail|$devUrl"),
      "publish.ci.password"       -> List("env:PUBLISH_PASSWORD"),
      "publish.name"              -> List(projName),
      "publish.license"           -> List("Apache-2.0"),
      "publish.url"               -> List(s"https://github.com/$ghUserName/tests"),
      "publish.ci.repository"     -> List("github"),
      "publish.ci.computeVersion" -> List("git:tag")
    )
    val expectedGhSecrets = Set("PUBLISH_USER", "PUBLISH_PASSWORD")
    testInputs.fromRoot { root =>
      configSetup(root / configFile, root)
      gitInit(root / projDir)
      val res = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "setup",
        "--ci",
        "--publish-repository",
        "github",
        "--dummy",
        projDir
      ).call(
        cwd = root,
        mergeErrIntoOut = true,
        env = envs
      )

      val ghSecrets = res.out.text()
        .linesIterator
        .filter(_.startsWith("Would have set GitHub secret "))
        .map(_.stripPrefix("Would have set GitHub secret "))
        .toSet
      val directives0 = directives(os.read(root / projDir / "publish-conf.scala"))
      expect(directives0 == expectedDirectives)
      expect(ghSecrets == expectedGhSecrets)
    }
  }

  test("local upload key") {
    val expectedDirectives = Map(
      "publish.versionControl"    -> Seq(s"github:$ghUserName/tests"),
      "publish.organization"      -> Seq(s"io.github.$ghUserName"),
      "publish.developer"         -> Seq(s"$devName|$devMail|$devUrl"),
      "publish.repository"        -> Seq("central-s01"),
      "publish.url"               -> Seq(s"https://github.com/$ghUserName/tests"),
      "publish.name"              -> Seq(projName),
      "publish.computeVersion"    -> Seq("git:tag"),
      "publish.license"           -> Seq("Apache-2.0"),
      "publish.secretKey"         -> Seq("file:key.skr"),
      "publish.secretKeyPassword" -> Seq("file:whatever_not_checked"),
      "publish.publicKey"         -> Seq("file:key.pub")
    )
    val expectedGhSecrets = Set.empty[String]
    testInputs.fromRoot { root =>
      configSetup(root / configFile, root)
      gitInit(root / projDir)

      val pgpCreateOutput = os.proc(
        TestUtil.cli,
        "--power",
        "pgp",
        "create",
        "--email",
        devMail,
        "--password",
        s"value:$password"
      )
        .call(
          cwd = root,
          mergeErrIntoOut = true,
          env = envs
        )

      val publicKeyRegex: Regex = "Wrote public key (\\w+) .*".r

      val publicKeyIdOpt = pgpCreateOutput.out.text()
        .linesIterator
        .toSeq
        .collect {
          case publicKeyRegex(publicKeyId) => publicKeyId
        }
        .headOption

      expect(publicKeyIdOpt.isDefined)

      val publicKeyId: String = publicKeyIdOpt.get

      val res = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "setup",
        "--dummy",
        "--secret-key",
        "file:key.skr",
        "--secret-key-password",
        s"file:whatever_not_checked",
        "--public-key",
        "file:key.pub",
        projDir
      ).call(
        cwd = root,
        mergeErrIntoOut = true,
        env = envs
      ).out.text()

      val ghSecrets = res
        .linesIterator
        .filter(_.startsWith("Would have set GitHub secret "))
        .map(_.stripPrefix("Would have set GitHub secret "))
        .toSet
      val directives0 = directives(os.read(root / projDir / "publish-conf.scala"))
      expect(directives0 == expectedDirectives)
      expect(ghSecrets == expectedGhSecrets)
      expect(!res.contains("found keys in config"))
      expect(res.contains(s"Would upload key 0x$publicKeyId"))
    }
  }

  test("local add public key and password") {
    val expectedDirectives = Map(
      "publish.versionControl" -> Seq(s"github:$ghUserName/tests"),
      "publish.organization"   -> Seq(s"io.github.$ghUserName"),
      "publish.developer"      -> Seq(s"$devName|$devMail|$devUrl"),
      "publish.repository"     -> Seq("central-s01"),
      "publish.url"            -> Seq(s"https://github.com/$ghUserName/tests"),
      "publish.name"           -> Seq(projName),
      "publish.computeVersion" -> Seq("git:tag"),
      "publish.license"        -> Seq("Apache-2.0"),
      "publish.secretKey"      -> Seq("file:key.skr")
    )
    val expectedGhSecrets = Set.empty[String]
    testInputs.fromRoot { root =>
      configSetup(root / configFile, root)
      gitInit(root / projDir)

      val pgpCreateOutput = os.proc(
        TestUtil.cli,
        "--power",
        "pgp",
        "create",
        "--email",
        devMail,
        "--password",
        s"value:$password"
      )
        .call(
          cwd = root,
          mergeErrIntoOut = true,
          env = envs
        )

      val publicKeyRegex: Regex = "Wrote public key (\\w+) .*".r

      val publicKeyIdOpt = pgpCreateOutput.out.text()
        .linesIterator
        .toSeq
        .collect {
          case publicKeyRegex(publicKeyId) => publicKeyId
        }
        .headOption

      expect(publicKeyIdOpt.isDefined)

      val publicKeyId: String = publicKeyIdOpt.get

      val res = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "setup",
        "--dummy",
        "--secret-key",
        "file:key.skr",
        projDir
      ).call(
        cwd = root,
        mergeErrIntoOut = true,
        env = envs
      ).out.text()

      val ghSecrets = res
        .linesIterator
        .filter(_.startsWith("Would have set GitHub secret "))
        .map(_.stripPrefix("Would have set GitHub secret "))
        .toSet
      val directives0 = directives(os.read(root / projDir / "publish-conf.scala"))
      expect(directives0 == expectedDirectives)
      expect(ghSecrets == expectedGhSecrets)
      expect(!res.contains("found keys in config"))
      expect(res.contains("Warning: no public key passed, not checking"))

      val res2 = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "setup",
        "--dummy",
        "--secret-key-password",
        s"file:whatever_not_checked",
        "--public-key",
        "file:key.pub",
        projDir
      ).call(
        cwd = root,
        mergeErrIntoOut = true,
        env = envs
      )

      System.err.write(res2.out.bytes)
      val ghSecrets2 = res2.out.text()
        .linesIterator
        .filter(_.startsWith("Would have set GitHub secret "))
        .map(_.stripPrefix("Would have set GitHub secret "))
        .toSet
      val directives2 = directives(os.read(root / projDir / "publish-conf.scala"))
      val expectedDirectivesAdded = Map(
        "publish.secretKeyPassword" -> Seq("file:whatever_not_checked"),
        "publish.publicKey"         -> Seq("file:key.pub")
      )

      expect(directives2 == (expectedDirectives ++ expectedDirectivesAdded))
      expect(ghSecrets2 == expectedGhSecrets)
      expect(!res2.out.text().contains("found keys in config"))
      expect(res2.out.text().contains(s"Would upload key 0x$publicKeyId"))
    }
  }

  test("local secret value written") {
    testInputs.fromRoot { root =>
      configSetup(root / configFile, root)
      gitInit(root / projDir)

      val res = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "setup",
        "--dummy",
        "--secret-key",
        "value:whatever",
        projDir
      ).call(
        cwd = root,
        mergeErrIntoOut = true,
        env = envs
      )

      expect(res.out.text().contains(
        "The secret value of PGP private key will be written to a potentially public file!"
      ))
    }
  }

}
