package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import java.util.jar.{JarOutputStream, Manifest as JarManifest}
import java.util.zip.ZipEntry

import scala.build.Ops.*
import scala.build.internal.MainClass
import scala.build.internal.MainClass.MainMethodKind
import scala.build.options.*
import scala.build.tests.TestUtil.*
import scala.build.{BuildThreads, Directories, LocalRepo}
import scala.util.Using

class MainClassTests extends TestUtil.ScalaCliBuildSuite {

  private val jep512MinJava        = scala.build.internal.Constants.jep512MinJavaVersion
  private val jep512PreviewMinJava = scala.build.internal.Constants.jep512PreviewMinJavaVersion
  private val defaultJvm           = scala.build.internal.Constants.defaultJavaVersion

  val buildThreads: BuildThreads = BuildThreads.create()
  val extraRepoTmpDir: os.Path   = os.temp.dir(prefix = "scala-cli-main-class-tests-extra-repo-")
  val directories: Directories   = Directories.under(extraRepoTmpDir)

  override def afterAll(): Unit = {
    TestInputs.tryRemoveAll(extraRepoTmpDir)
    buildThreads.shutdown()
  }

  val baseOptions: BuildOptions = BuildOptions(
    internal = InternalOptions(
      localRepository = LocalRepo.localRepo(directories.localRepoDir, TestLogger()),
      keepDiagnostics = true
    )
  )

  val defaultOptions: BuildOptions = baseOptions.copy(
    scalaOptions = baseOptions.scalaOptions.copy(
      scalaVersion = Some(MaybeScalaVersion(Constants.defaultScalaVersion)),
      scalaBinaryVersion = None
    )
  )

  private def findKinds(dir: os.Path): Seq[(String, MainMethodKind)] =
    MainClass.find(dir, TestLogger()).map(c => c.className -> c.kind)

  test("MainMethodKind.isSupportedByJvm covers the JEP 512 version matrix") {
    val jep512Kinds = Seq(
      MainMethodKind.InstanceWithArgs,
      MainMethodKind.StaticNoArgs,
      MainMethodKind.InstanceNoArgs
    )
    expect(MainMethodKind.StaticWithArgs.isSupportedByJvm(defaultJvm, previewEnabled = false))
    for kind <- jep512Kinds do {
      expect(kind.isSupportedByJvm(jep512MinJava, previewEnabled = false))
      expect(!kind.isSupportedByJvm(defaultJvm, previewEnabled = false))
      expect(!kind.isSupportedByJvm(jep512PreviewMinJava, previewEnabled = false))
      expect(kind.isSupportedByJvm(jep512PreviewMinJava, previewEnabled = true))
    }
  }

  test("detect all launchable JEP 512 main method shapes in Java sources") {
    TestInputs(
      os.rel / "Classic.java" ->
        s"""//> using jvm $jep512MinJava
           |public class Classic {
           |  public static void main(String[] args) {}
           |}
           |""".stripMargin,
      os.rel / "ProtectedStatic.java" ->
        """public class ProtectedStatic {
          |  protected static void main(String[] args) {}
          |}
          |""".stripMargin,
      os.rel / "StaticNoArgs.java" ->
        """public class StaticNoArgs {
          |  public static void main() {}
          |}
          |""".stripMargin,
      os.rel / "InstanceWithArgs.java" ->
        """public class InstanceWithArgs {
          |  public void main(String[] args) {}
          |}
          |""".stripMargin,
      os.rel / "InstanceNoArgs.java" ->
        """public class InstanceNoArgs {
          |  public void main() {}
          |}
          |""".stripMargin,
      os.rel / "PackagePrivate.java" ->
        """public class PackagePrivate {
          |  void main() {}
          |}
          |""".stripMargin,
      os.rel / "Both.java" ->
        """public class Both {
          |  public static void main() {}
          |  public void main(String[] args) {}
          |}
          |""".stripMargin
      // baseOptions (no Scala version) so this stays a pure Java project and uses javac from JDK 25
    ).withBuild(baseOptions, buildThreads, None, buildTests = false) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow.successfulOpt.get
      expect(
        findKinds(build.output).toMap == Map(
          "Classic"          -> MainMethodKind.StaticWithArgs,
          "ProtectedStatic"  -> MainMethodKind.StaticWithArgs,
          "StaticNoArgs"     -> MainMethodKind.StaticNoArgs,
          "InstanceWithArgs" -> MainMethodKind.InstanceWithArgs,
          "InstanceNoArgs"   -> MainMethodKind.InstanceNoArgs,
          "PackagePrivate"   -> MainMethodKind.InstanceNoArgs,
          "Both"             -> MainMethodKind.InstanceWithArgs
        )
      )
    }
  }

  test("ignore main methods that cannot be launched, in Java sources") {
    TestInputs(
      os.rel / "PrivateStatic.java" ->
        s"""//> using jvm $jep512MinJava
           |public class PrivateStatic {
           |  private static void main(String[] args) {}
           |}
           |""".stripMargin,
      os.rel / "PrivateInstance.java" ->
        """public class PrivateInstance {
          |  private void main() {}
          |}
          |""".stripMargin,
      os.rel / "NonVoid.java" ->
        """public class NonVoid {
          |  public static int main() { return 0; }
          |}
          |""".stripMargin,
      os.rel / "PrivateCtor.java" ->
        """public class PrivateCtor {
          |  private PrivateCtor() {}
          |  public void main() {}
          |}
          |""".stripMargin,
      os.rel / "AbstractMain.java" ->
        """public abstract class AbstractMain {
          |  public void main() {}
          |}
          |""".stripMargin,
      os.rel / "IfaceMain.java" ->
        """public interface IfaceMain {
          |  default void main() {}
          |}
          |""".stripMargin
    ).withBuild(baseOptions, buildThreads, None, buildTests = false) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow.successfulOpt.get
      expect(os.walk(build.output).exists(_.ext == "class"))
      expect(findKinds(build.output).isEmpty)
    }
  }

  test("detect instance mains inside a jar of compiled classes") {
    TestInputs(
      os.rel / "Hello.java" ->
        s"""//> using jvm $jep512MinJava
           |public class Hello {
           |  public void main() {}
           |}
           |""".stripMargin
    ).withBuild(baseOptions, buildThreads, None, buildTests = false) { (root, _, maybeBuild) =>
      val build      = maybeBuild.orThrow.successfulOpt.get
      val fromDir    = findKinds(build.output)
      val jarPath    = root / "run.jar"
      val classFiles = os.walk(build.output).filter(_.ext == "class")
      Using.resource(new JarOutputStream(os.write.outputStream(jarPath), new JarManifest())) {
        jos =>
          for classFile <- classFiles do {
            val entryName = classFile.relativeTo(build.output).toString
            jos.putNextEntry(new ZipEntry(entryName))
            jos.write(os.read.bytes(classFile))
            jos.closeEntry()
          }
      }
      expect(fromDir == Seq("Hello" -> MainMethodKind.InstanceNoArgs))
      expect(MainClass.find(jarPath, TestLogger()).map(c => c.className -> c.kind) == fromDir)
    }
  }

  test("detect an instance main method in a Scala class on JDK 25") {
    TestInputs(
      os.rel / "A.scala" ->
        s"""//> using jvm $jep512MinJava
           |class A { def main(): Unit = println(1) }
           |""".stripMargin
    ).withBuild(defaultOptions, buildThreads, None, buildTests = false) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow.successfulOpt.get
      expect(build.foundMainClasses() == Seq("A"))
    }
  }

  test("an object with a no-arg main is detected once, via its static forwarder") {
    TestInputs(
      os.rel / "O.scala" ->
        s"""//> using jvm $jep512MinJava
           |object O { def main(): Unit = println(1) }
           |""".stripMargin
    ).withBuild(defaultOptions, buildThreads, None, buildTests = false) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow.successfulOpt.get
      expect(build.foundMainClasses() == Seq("O"))
    }
  }

  test("do not treat an instance main as supported on JDK 17") {
    TestInputs(
      os.rel / "A.scala" ->
        s"""//> using jvm $defaultJvm
           |class A { def main(): Unit = println(1) }
           |""".stripMargin
    ).withBuild(defaultOptions, buildThreads, None, buildTests = false) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow.successfulOpt.get
      expect(build.foundMainClasses().isEmpty)
      expect(build.unsupportedMainMethodsNote.isDefined)
      expect(build.unsupportedMainMethodsNote.get.contains("JEP 512"))
      expect(build.unsupportedMainMethodsNote.get.contains("A"))
    }
  }

  test("detect an instance main on JDK 21 with --enable-preview") {
    TestInputs(
      os.rel / "A.scala" ->
        s"""//> using jvm $jep512PreviewMinJava
           |//> using javaOpt --enable-preview
           |class A { def main(): Unit = println(1) }
           |""".stripMargin
    ).withBuild(defaultOptions, buildThreads, None, buildTests = false) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow.successfulOpt.get
      expect(build.foundMainClasses() == Seq("A"))
    }
  }

  test("do not detect an instance main on Scala.js") {
    TestInputs(
      os.rel / "A.scala" -> "class A { def main(): Unit = println(1) }"
    ).withBuild(defaultOptions.enableJs, buildThreads, None, buildTests = false) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow.successfulOpt.get
        expect(build.foundMainClasses().isEmpty)
    }
  }

  test("do not detect an instance main on Scala Native") {
    TestInputs(
      os.rel / "A.scala" -> "class A { def main(): Unit = println(1) }"
    ).withBuild(defaultOptions.enableNative, buildThreads, None, buildTests = false) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow.successfulOpt.get
        expect(build.foundMainClasses().isEmpty)
    }
  }

  test("detect a classic main on Scala.js") {
    TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println(1)
          |}
          |""".stripMargin
    ).withBuild(defaultOptions.enableJs, buildThreads, None, buildTests = false) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow.successfulOpt.get
        expect(build.foundMainClasses() == Seq("Main"))
    }
  }

  test("detect a classic main on Scala Native") {
    TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println(1)
          |}
          |""".stripMargin
    ).withBuild(defaultOptions.enableNative, buildThreads, None, buildTests = false) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow.successfulOpt.get
        expect(build.foundMainClasses() == Seq("Main"))
    }
  }

  test("detect a script main on Scala.js") {
    TestInputs(
      os.rel / "simple.sc" -> "println(1)"
    ).withBuild(defaultOptions.enableJs, buildThreads, None, buildTests = false) {
      (_, _, maybeBuild) =>
        val build = maybeBuild.orThrow.successfulOpt.get
        expect(build.foundMainClasses().contains("simple_sc"))
    }
  }

  test("ordinary static main(String[]) gains no extra candidates") {
    TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = println(1)
          |}
          |""".stripMargin
    ).withBuild(defaultOptions, buildThreads, None, buildTests = false) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow.successfulOpt.get
      expect(build.foundMainClasses() == Seq("Main"))
    }
  }

  test("script with a top-level no-arg main does not list the wrapper user-code class") {
    TestInputs(
      os.rel / "hello.sc" ->
        s"""//> using jvm $jep512MinJava
           |def main(): Unit = println(1)
           |""".stripMargin
    ).withBuild(defaultOptions, buildThreads, None, buildTests = false) { (_, _, maybeBuild) =>
      val build = maybeBuild.orThrow.successfulOpt.get
      val found = build.foundMainClasses()
      expect(!found.exists(_.endsWith("$_")))
      expect(found.contains("hello_sc"))
    }
  }
}
