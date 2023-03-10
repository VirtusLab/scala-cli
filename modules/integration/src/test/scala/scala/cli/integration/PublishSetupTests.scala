package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import com.virtuslab.using_directives.config.Settings
import com.virtuslab.using_directives.custom.model.UsingDirectiveKind
import com.virtuslab.using_directives.reporter.ConsoleReporter
import com.virtuslab.using_directives.{Context, UsingDirectivesProcessor}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish

import scala.jdk.CollectionConverters._

class PublishSetupTests extends ScalaCliSuite {

  private def ghUserName = "foo"
  private def projName   = "project-name"
  private def devName    = "Alex Test"
  private def devMail    = "alex@alex.me"
  private def devUrl     = "https://alex.me"

  private def configSetup(configFile: os.Path, root: os.Path): Unit = {
    val envs = Map("SCALA_CLI_CONFIG" -> configFile.toString)
    os.proc(TestUtil.cli, "--power", "config", "publish.user.name", devName)
      .call(cwd = root, stdout = os.Inherit, env = envs)
    os.proc(TestUtil.cli, "--power", "config", "publish.user.email", devMail)
      .call(cwd = root, stdout = os.Inherit, env = envs)
    os.proc(TestUtil.cli, "--power", "config", "publish.user.url", devUrl)
      .call(cwd = root, stdout = os.Inherit, env = envs)
    os.proc(
      TestUtil.cli,
      "--power",
      "config",
      "publish.credentials",
      "s01.oss.sonatype.org",
      "value:uSeR",
      "value:1234"
    )
      .call(cwd = root, stdout = os.Inherit, env = envs)
    os.proc(TestUtil.cli, "--power", "config", "--create-pgp-key")
      .call(cwd = root, stdout = os.Inherit, env = envs)
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
      val res = os.proc(TestUtil.cli, "--power", "publish", "setup", projDir).call(
        cwd = root,
        mergeErrIntoOut = true,
        env = envs
      )
      System.err.write(res.out.bytes)
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

  test("CI") {
    val expectedDirectives = Map(
      "publish.versionControl"       -> List(s"github:$ghUserName/tests"),
      "publish.organization"         -> List(s"io.github.$ghUserName"),
      "publish.developer"            -> List(s"$devName|$devMail|$devUrl"),
      "publish.name"                 -> List(projName),
      "publish.license"              -> List("Apache-2.0"),
      "publish.url"                  -> List(s"https://github.com/$ghUserName/tests"),
      "publish.ci.secretKey"         -> List("env:PUBLISH_SECRET_KEY"),
      "publish.ci.user"              -> List("env:PUBLISH_USER"),
      "publish.ci.password"          -> List("env:PUBLISH_PASSWORD"),
      "publish.ci.secretKeyPassword" -> List("env:PUBLISH_SECRET_KEY_PASSWORD"),
      "publish.ci.repository"        -> List("central-s01"),
      "publish.ci.computeVersion"    -> List("git:tag")
    )
    val expectedGhSecrets =
      Set("PUBLISH_USER", "PUBLISH_PASSWORD", "PUBLISH_SECRET_KEY", "PUBLISH_SECRET_KEY_PASSWORD")
    testInputs.fromRoot { root =>
      configSetup(root / configFile, root)
      gitInit(root / projDir)
      val res =
        os.proc(TestUtil.cli, "--power", "publish", "setup", "--ci", "--dummy", projDir).call(
          cwd = root,
          mergeErrIntoOut = true,
          env = envs
        )
      System.err.write(res.out.bytes)
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
        "--publish-repository",
        "github",
        projDir
      ).call(
        cwd = root,
        mergeErrIntoOut = true,
        env = envs
      )
      System.err.write(res.out.bytes)
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
      System.err.write(res.out.bytes)
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

}
