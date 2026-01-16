package scala.cli.exportCmd

import java.nio.charset.StandardCharsets

import scala.build.options.ConfigMonoid
import scala.cli.util.SeqHelpers.*
import scala.reflect.NameTransformer
import scala.util.Properties

final case class MillProject(
  millVersion: Option[String] = None,
  mainDeps: Seq[String] = Nil,
  mainCompileOnlyDeps: Seq[String] = Nil,
  testDeps: Seq[String] = Nil,
  testCompileOnlyDeps: Seq[String] = Nil,
  scalaVersion: Option[String] = None,
  scalacOptions: Seq[String] = Nil,
  scalaCompilerPlugins: Seq[String] = Nil,
  scalaJsVersion: Option[String] = None,
  scalaNativeVersion: Option[String] = None,
  nameOpt: Option[String] = None,
  launchers: Seq[(os.RelPath, Array[Byte])] = Nil,
  mainSources: Seq[(os.SubPath, String, Array[Byte])] = Nil,
  testSources: Seq[(os.SubPath, String, Array[Byte])] = Nil,
  extraDecls: Seq[String] = Nil,
  resourcesDirs: Seq[os.Path] = Nil,
  extraTestDecls: Seq[String] = Nil,
  mainClass: Option[String] = None
) extends Project {
  private lazy val isMill1OrNewer: Boolean = !millVersion.exists(_.startsWith("0."))

  def +(other: MillProject): MillProject =
    MillProject.monoid.orElse(this, other)

  private def name = nameOpt.getOrElse("project")

  def writeTo(dir: os.Path): Unit = {
    val nl      = System.lineSeparator()
    val charSet = StandardCharsets.UTF_8

    for ((relPath, content) <- launchers) {
      val dest = dir / relPath
      os.write(dest, content, createFolders = true)
      if (!Properties.isWin)
        os.perms.set(dest, "rwxr-xr-x")
    }

    for (ver <- millVersion)
      os.write(dir / ".mill-version", ver.getBytes(charSet), createFolders = true)

    val escapedName =
      if (NameTransformer.encode(name) == name) name
      else "`" + name + "`"
    val (parentModule, maybeExtraImport, maybePlatformVer) =
      if (scalaVersion.isEmpty) ("JavaModule", None, None)
      else
        scalaJsVersion
          .map(ver =>
            (
              "ScalaJSModule",
              Some("import mill.scalajslib._"),
              Some(s"""def scalaJSVersion = "$ver"""")
            )
          )
          .orElse(
            scalaNativeVersion.map(ver =>
              (
                "ScalaNativeModule",
                Some("import mill.scalanativelib._"),
                Some(s"""def scalaNativeVersion = "$ver"""")
              )
            )
          )
          .getOrElse(("ScalaModule", None, None))

    val maybeScalaVer = scalaVersion.map { sv =>
      s"""def scalaVersion = "$sv""""
    }
    val maybeScalacOptions =
      if (scalacOptions.isEmpty) None
      else {
        val optsString = scalacOptions.map(opt => s"\"$opt\"").mkString(", ")
        Some(s"""def scalacOptions = super.scalacOptions() ++ Seq($optsString)""")
      }
    def maybeDeps(deps: Seq[String], isCompileOnly: Boolean = false) = {
      val depsDefinition = isCompileOnly -> isMill1OrNewer match {
        case (true, true)   => Seq("def compileMvnDeps = super.compileMvnDeps() ++ Seq(")
        case (true, false)  => Seq("def compileIvyDeps = super.compileIvyDeps() ++ Seq(")
        case (false, true)  => Seq("def mvnDeps = super.mvnDeps() ++ Seq(")
        case (false, false) => Seq("def ivyDeps = super.ivyDeps() ++ Seq(")
      }
      if deps.isEmpty then Seq.empty[String]
      else
        depsDefinition ++
          deps
            .map {
              case dep if isMill1OrNewer => s"""  mvn"$dep""""
              case dep                   => s"""  ivy"$dep""""
            }
            .appendOnInit(",") ++
          Seq(")")
    }

    val maybeScalaCompilerPlugins =
      if scalaCompilerPlugins.isEmpty then Seq.empty
      else
        Seq(
          if isMill1OrNewer
          then "def scalacPluginMvnDeps = super.scalacPluginMvnDeps() ++ Seq("
          else "def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Seq("
        ) ++
          scalaCompilerPlugins
            .map {
              case dep if isMill1OrNewer => s"  mvn\"$dep\""
              case dep                   => s"  ivy\"$dep\""
            }
            .appendOnInit(",")
          ++ Seq(")")

    val maybeMain = mainClass.map { mc =>
      s"""def mainClass = Some("$mc")"""
    }
    val customResourcesDecls =
      if (resourcesDirs.isEmpty) Nil
      else {
        val resources =
          resourcesDirs.map {
            case p if isMill1OrNewer =>
              s"""mill.api.BuildCtx.workspaceRoot / os.RelPath("${p.relativeTo(dir)}")"""
            case p => s"""T.workspace / os.RelPath("${p.relativeTo(dir)}")"""
          }
        Seq("def runClasspath = super.runClasspath() ++ Seq(") ++
          resources.map(resource => s"  $resource").appendOnInit(",") ++
          Seq(").map(PathRef(_))")
      }
    val millScalaTestPlatform = if (scalaJsVersion.nonEmpty) "ScalaJSTests"
    else if (scalaNativeVersion.nonEmpty) "ScalaNativeTests"
    else "ScalaTests"
    val maybeTestDefinition = if (testSources.nonEmpty)
      Seq(
        "",
        s"  object test extends $millScalaTestPlatform {"
      ) ++
        maybeDeps(testDeps).map(s => s"    $s") ++
        maybeDeps(testCompileOnlyDeps, isCompileOnly = true).map(s => s"    $s") ++
        extraTestDecls.map(s => s"    $s") ++ Seq("  }")
    else Seq.empty

    val buildFileContent: String = {
      val parts: Seq[String] = Seq(
        "import mill._",
        "import mill.scalalib._"
      ) ++ maybeExtraImport ++
        Seq(
          s"object $escapedName extends $parentModule {"
        ) ++
        maybeScalaVer.map(s => s"  $s") ++
        maybePlatformVer.map(s => s"  $s") ++
        maybeScalacOptions.map(s => s"  $s") ++
        maybeDeps(mainDeps).map(s => s"  $s") ++
        maybeDeps(mainCompileOnlyDeps, isCompileOnly = true).map(s => s"  $s") ++
        maybeScalaCompilerPlugins.map(s => s"  $s") ++
        maybeMain.map(s => s"  $s") ++
        customResourcesDecls.map(s => s"  $s") ++
        extraDecls.map("  " + _) ++
        maybeTestDefinition ++
        Seq("}", "")
      parts.mkString(nl)
    }

    for ((path, _, content) <- mainSources) {
      val path0 = dir / name / "src" / path
      os.write(path0, content, createFolders = true)
    }
    for ((path, _, content) <- testSources) {
      val path0 = dir / name / "test" / "src" / path
      os.write(path0, content, createFolders = true)
    }

    val outputBuildFile = if isMill1OrNewer then dir / "build.mill" else dir / "build.sc"
    os.write(outputBuildFile, buildFileContent.getBytes(charSet))
  }
}

object MillProject {
  implicit val monoid: ConfigMonoid[MillProject] = ConfigMonoid.derive
}
