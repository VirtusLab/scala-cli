package cli.tests

import bloop.rifle.BloopRifleConfig
import com.eed3si9n.expecty.Expecty.expect

import java.util.jar.Manifest as JarManifest
import java.util.zip.ZipFile

import scala.build.Ops.*
import scala.build.options.{BuildOptions, InternalOptions}
import scala.build.tests.util.BloopServer
import scala.build.tests.{TestInputs, TestLogger}
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.util.BuildCommandHelpers.copyOutput
import scala.jdk.CollectionConverters.*
import scala.util.Using

class CopyOutputJarTests extends TestUtil.ScalaCliSuite {
  val buildThreads: BuildThreads    = BuildThreads.create()
  def bloopConfig: BloopRifleConfig = BloopServer.bloopConfig

  val extraRepoTmpDir: os.Path = os.temp.dir(prefix = "scala-cli-tests-copy-output-jar-")
  val directories: Directories = Directories.under(extraRepoTmpDir)

  val defaultOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir, TestLogger())
    )
  )

  test("-d out.jar produces a JAR file with class entries and Main-Class") {
    TestInputs().fromRoot { root =>
      val options = defaultOptions.copy(mainClass = Some("Hello"))
      val inputs  = TestInputs(
        files = Seq(
          os.rel / "Hello.scala" ->
            """object Hello {
              |  def main(args: Array[String]): Unit = println("Hello")
              |}
              |""".stripMargin
        ),
        forceCwd = Some(root)
      )
      inputs.withBuild(options, buildThreads, Some(bloopConfig)) {
        (_, _, maybeBuild) =>
          val build  = maybeBuild.orThrow.successfulOpt.get
          val outJar = root / "out.jar"
          build.copyOutput(
            SharedOptions(compilationOutput = Some(outJar.toString)),
            TestLogger()
          )
          expect(os.isFile(outJar))
          Using.resource(ZipFile(outJar.toIO)) { zf =>
            val entries = zf.entries().asScala.map(_.getName).toSeq
            expect(entries.exists(_.endsWith("Hello.class")))
            expect(entries.exists(_ == "META-INF/MANIFEST.MF"))
            val manifest = JarManifest(zf.getInputStream(zf.getEntry("META-INF/MANIFEST.MF")))
            expect(manifest.getMainAttributes.getValue("Main-Class") == "Hello")
          }
      }
    }
  }

  test("-d out.jar preserves user META-INF/MANIFEST.MF from resources") {
    TestInputs().fromRoot { root =>
      val options = defaultOptions.copy(mainClass = Some("Hello"))
      val inputs  = TestInputs(
        files = Seq(
          os.rel / "Hello.scala" ->
            """//> using resourceDir resources
              |
              |object Hello {
              |  def main(args: Array[String]): Unit = println("Hello")
              |}
              |""".stripMargin,
          os.rel / "resources" / "META-INF" / "MANIFEST.MF" ->
            """Manifest-Version: 1.0
              |X-Custom: yes
              |""".stripMargin
        ),
        inputArgs = Seq("."),
        forceCwd = Some(root)
      )
      inputs.withBuild(options, buildThreads, Some(bloopConfig), fromDirectory = true) {
        (_, _, maybeBuild) =>
          val build  = maybeBuild.orThrow.successfulOpt.get
          val outJar = root / "out.jar"
          build.copyOutput(
            SharedOptions(compilationOutput = Some(outJar.toString)),
            TestLogger()
          )
          expect(os.isFile(outJar))
          Using.resource(ZipFile(outJar.toIO)) { zf =>
            val entries = zf.entries().asScala.map(_.getName).toSeq
            expect(entries.count(_ == "META-INF/MANIFEST.MF") == 1)
            val manifest = JarManifest(zf.getInputStream(zf.getEntry("META-INF/MANIFEST.MF")))
            expect(manifest.getMainAttributes.getValue("X-Custom") == "yes")
            expect(manifest.getMainAttributes.getValue("Main-Class") == "Hello")
          }
      }
    }
  }

  test("-d out.jar overwrites an existing JAR") {
    TestInputs().fromRoot { root =>
      val inputs = TestInputs(
        files = Seq(
          os.rel / "Hello.scala" ->
            """object Hello {
              |  def main(args: Array[String]): Unit = println("Hello")
              |}
              |""".stripMargin
        ),
        forceCwd = Some(root)
      )
      inputs.withBuild(defaultOptions, buildThreads, Some(bloopConfig)) {
        (_, _, maybeBuild) =>
          val build  = maybeBuild.orThrow.successfulOpt.get
          val outJar = root / "out.jar"
          os.write(outJar, Array[Byte](0, 1, 2, 3))
          expect(os.isFile(outJar))
          val previousSize = os.size(outJar)
          build.copyOutput(
            SharedOptions(compilationOutput = Some(outJar.toString)),
            TestLogger()
          )
          expect(os.isFile(outJar))
          expect(os.size(outJar) != previousSize)
          Using.resource(ZipFile(outJar.toIO)) { zf =>
            expect(zf.entries().asScala.exists(_.getName.endsWith("Hello.class")))
          }
      }
    }
  }

  test("-d outDir still produces a directory of class files") {
    TestInputs().fromRoot { root =>
      val inputs = TestInputs(
        files = Seq(
          os.rel / "Hello.scala" ->
            """object Hello {
              |  def main(args: Array[String]): Unit = println("Hello")
              |}
              |""".stripMargin
        ),
        forceCwd = Some(root)
      )
      inputs.withBuild(defaultOptions, buildThreads, Some(bloopConfig)) {
        (_, _, maybeBuild) =>
          val build  = maybeBuild.orThrow.successfulOpt.get
          val outDir = root / "outDir"
          build.copyOutput(
            SharedOptions(compilationOutput = Some(outDir.toString)),
            TestLogger()
          )
          expect(os.isDir(outDir))
          expect(os.walk(outDir).exists(_.last.endsWith("Hello.class")))
      }
    }
  }
}
