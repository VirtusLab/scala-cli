package cli.tests

import bloop.rifle.BloopRifleConfig
import com.eed3si9n.expecty.Expecty.expect

import java.nio.file.FileSystems
import java.util.jar.Manifest as JarManifest
import java.util.zip.ZipFile

import scala.build.Ops.*
import scala.build.options.{BuildOptions, InternalOptions, PackageType}
import scala.build.tests.util.BloopServer
import scala.build.tests.{TestInputs, TestLogger}
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.cli.commands.package0.{Package, PackageOptions}
import scala.cli.packaging.Library
import scala.jdk.CollectionConverters.*
import scala.util.Using

class PackageTests extends TestUtil.ScalaCliSuite {
  val buildThreads: BuildThreads    = BuildThreads.create()
  def bloopConfig: BloopRifleConfig = BloopServer.bloopConfig

  val extraRepoTmpDir: os.Path = os.temp.dir(prefix = "scala-cli-tests-extra-repo-")
  val directories: Directories = Directories.under(extraRepoTmpDir)

  val defaultOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir, TestLogger())
    )
  )

  /** Fixes - https://github.com/VirtusLab/scala-cli/issues/2166 */
  test(s"should generate a correct jar library when the project was changed") {
    TestInputs().fromRoot { root =>
      val inputs = TestInputs(
        files = Seq(os.rel / "Hello.scala" ->
          """//> using platform scala-js
            |//> using options -Wvalue-discard -Wunused:all
            |
            |object Hello extends App {
            |  println("Hello, World World World")
            |}""".stripMargin),
        forceCwd = Some(root)
      )
      inputs.withBuild(defaultOptions, buildThreads, Some(bloopConfig)) {
        (_, _, maybeFirstBuild) =>
          val firstBuild      = maybeFirstBuild.orThrow.successfulOpt.get
          val firstLibraryJar = Library.libraryJar(Seq(firstBuild))
          expect(os.exists(firstLibraryJar)) // should create library jar

          // change Hello.scala and recompile
          os.write.over(
            root / "Hello.scala",
            """//> using platform scala-js
              |//> using options -Wvalue-discard -Wunused:all
              |
              |object Hello extends App {
              |  println("hello")
              |}""".stripMargin
          )

          inputs.withBuild(
            defaultOptions,
            buildThreads,
            Some(bloopConfig),
            skipCreatingSources = true
          ) {
            (_, _, maybeSecondBuild) =>
              val secondBuild = maybeSecondBuild.orThrow.successfulOpt.get
              val libraryJar  = Library.libraryJar(Seq(secondBuild))
              val fs = // should not throw "invalid CEN header (bad signature)" ZipException
                FileSystems.newFileSystem(libraryJar.toNIO, null: ClassLoader)
              expect(fs.isOpen)
              fs.close()
          }
      }
    }
  }

  /** Fixes - https://github.com/VirtusLab/scala-cli/issues/2303 */
  test("accept packageType-native when using native platform") {
    val inputs = TestInputs(
      files = Seq(os.rel / "Hello.scala" ->
        """//> using platform native
          |//> using packaging.packageType native
          |
          |object Hello extends App {
          |  println("Hello World")
          |}""".stripMargin)
    )
    inputs.withBuild(defaultOptions, buildThreads, Some(bloopConfig)) {
      (_, _, maybeFirstBuild) =>
        val build = maybeFirstBuild.orThrow.successfulOpt.get

        val packageType = Package.resolvePackageType(Seq(build), None).orThrow
        expect(packageType == PackageType.Native.Application)
    }
  }

  test("libraryJar preserves user META-INF/MANIFEST.MF from resources") {
    TestInputs().fromRoot { root =>
      val inputs = TestInputs(
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
      inputs.withBuild(defaultOptions, buildThreads, Some(bloopConfig), fromDirectory = true) {
        (_, _, maybeBuild) =>
          val build      = maybeBuild.orThrow.successfulOpt.get
          val libraryJar = Library.libraryJar(Seq(build), Some("Hello"))
          expect(os.exists(libraryJar))
          Using.resource(ZipFile(libraryJar.toIO)) { zf =>
            val entries = zf.entries().asScala.map(_.getName).toSeq
            expect(entries.count(_ == "META-INF/MANIFEST.MF") == 1)
            val manifest = JarManifest(zf.getInputStream(zf.getEntry("META-INF/MANIFEST.MF")))
            expect(manifest.getMainAttributes.getValue("X-Custom") == "yes")
            expect(manifest.getMainAttributes.getValue("Main-Class") == "Hello")
          }
      }
    }
  }

  test("jsEntryModule prefers main.js over internal modules regardless of listing order") {
    val mainJs: os.RelPath   = os.rel / "main.js"
    val loaderJs: os.RelPath = os.rel / "__loader.js"
    // Scala.js WebAssembly backend output, in the order an ext4 directory listing may yield it
    val wasmOutput = Seq(loaderJs, mainJs)
    expect(Package.jsEntryModule(wasmOutput, None) == Some(mainJs))
    expect(Package.jsEntryModule(wasmOutput.reverse, None) == Some(mainJs))
    expect(Package.jsEntryModule(wasmOutput, Some("foo.Main")) == Some(mainJs))
  }

  test("jsEntryModule falls back to <mainClass>.js") {
    val internalJs: os.RelPath  = os.rel / "internal-abc.js"
    val mainClassJs: os.RelPath = os.rel / "foo.Main.js"
    val files                   = Seq(internalJs, mainClassJs)
    expect(Package.jsEntryModule(files, Some("foo.Main")) == Some(mainClassJs))
  }

  test("jsEntryModule falls back deterministically, skipping __-prefixed modules") {
    val loaderJs: os.RelPath = os.rel / "__loader.js"
    val aJs: os.RelPath      = os.rel / "a.js"
    val bJs: os.RelPath      = os.rel / "b.js"
    val files                = Seq(loaderJs, bJs, aJs)
    expect(Package.jsEntryModule(files, None) == Some(aJs))
    expect(Package.jsEntryModule(files.reverse, None) == Some(aJs))
    expect(Package.jsEntryModule(Seq(loaderJs), None) == Some(loaderJs))
    expect(Package.jsEntryModule(Nil, None).isEmpty)
  }

  private def packageBuildOptions(args: String*) =
    PackageOptions.parser.parse(args) match
      case Right((options, Nil)) => options.finalBuildOptions(TestLogger())
      case other                 => fail(s"Unexpected parsing result: $other")

  test("--docker-exposed-ports is passed to docker options") {
    val dockerOptions = packageBuildOptions(
      "--docker-exposed-ports",
      "8080",
      "--docker-exposed-ports",
      "9000-9010",
      "--docker-exposed-ports",
      "53/udp"
    ).orThrow.notForBloopOptions.packageOptions.dockerOptions
    expect(dockerOptions.exposedPorts == Seq("8080", "9000-9010", "53/udp"))
  }

  test("invalid --docker-exposed-ports is reported") {
    packageBuildOptions("--docker-exposed-ports", "8080", "--docker-exposed-ports", "http") match
      case Left(e) =>
        expect(e.message.contains("docker-exposed-port"))
        expect(e.message.contains("\"http\""))
      case Right(_) => fail("Expected an invalid docker exposed port to be reported")
  }
}
