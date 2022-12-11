package scala.cli.exportCmd

import java.nio.charset.StandardCharsets

import scala.build.options.ConfigMonoid
import scala.cli.util.SeqHelpers._
import scala.reflect.NameTransformer
import scala.util.Properties

final case class MillProject(
  millVersion: Option[String] = None,
  mainDeps: Seq[String] = Nil,
  testDeps: Seq[String] = Nil,
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
    val (parentModule, extraImports, extraDecs) =
      if (scalaVersion.isEmpty) ("JavaModule", "", "")
      else
        scalaJsVersion match {
          case Some(ver) =>
            ("ScalaJSModule", "import mill.scalajslib._", s"""def scalaJSVersion = "$ver"""")
          case None =>
            scalaNativeVersion match {
              case Some(ver) => (
                  "ScalaNativeModule",
                  "import mill.scalanativelib._",
                  s"""def scalaNativeVersion = "$ver""""
                )
              case None => ("ScalaModule", "", "")
            }
        }
    val maybeScalaVer = scalaVersion.map { sv =>
      s"""def scalaVersion = "$sv""""
    }
    val maybeScalacOptions =
      if (scalacOptions.isEmpty) None
      else {
        val optsString = scalacOptions.map(opt => s"\"$opt\"").mkString(", ")
        Some(s"""def scalacOptions = super.scalacOptions() ++ Seq($optsString)""")
      }
    def maybeDeps(deps: Seq[String]) =
      if (deps.isEmpty) Seq.empty[String]
      else
        Seq("def ivyDeps = super.ivyDeps() ++ Seq(") ++
          deps
            .map(dep => """  ivy"""" + dep + "\"")
            .appendOnInit(",") ++
          Seq(")")

    val maybeScalaCompilerPlugins =
      if (scalaCompilerPlugins.isEmpty) Seq.empty
      else
        Seq("def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Seq(") ++
          scalaCompilerPlugins
            .map(dep => s"  ivy\"$dep\"")
            .appendOnInit(",")
          ++ Seq(")")

    val maybeMain = mainClass.map { mc =>
      s"""def mainClass = Some("$mc")"""
    }
    val customResourcesDecls =
      if (resourcesDirs.isEmpty) Nil
      else {
        val resources = resourcesDirs.map(p => s"""T.workspace / os.RelPath("${p.relativeTo(dir)}")""")
        Seq("def runClasspath = super.runClasspath() ++ Seq(") ++
          resources.map(resource => s"  $resource").appendOnInit(",") ++
          Seq(").map(PathRef(_))")
      }

    val buildSc: String = {
      val parts: Seq[String] = Seq(
        "import mill._",
        "import mill.scalalib._",
        extraImports,
        s"object $escapedName extends $parentModule {"
      ) ++
        maybeScalaVer.map(s => s"  $s") ++
        maybeScalacOptions.map(s => s"  $s") ++
        maybeDeps(mainDeps).map(s => s"  $s") ++
        maybeScalaCompilerPlugins.map(s => s"  $s") ++
        maybeMain.map(s => s"  $s") ++
        customResourcesDecls.map(s => s"  $s") ++
        extraDecls.map("  " + _) ++
        Seq(
          "",
          "  object test extends Tests {"
        ) ++
        maybeDeps(testDeps).map(s => s"    $s") ++
        extraTestDecls.map(s => s"    $s") ++ Seq(
          "  }",
          "}",
          ""
        )
      parts.mkString(nl)
    }

    for ((path, language, content) <- mainSources) {
      val path0 = dir / name / "src" / path
      os.write(path0, content, createFolders = true)
    }
    for ((path, language, content) <- testSources) {
      val path0 = dir / name / "test" / "src" / path
      os.write(path0, content, createFolders = true)
    }

    os.write(dir / "build.sc", buildSc.getBytes(charSet))
  }
}

object MillProject {
  implicit val monoid: ConfigMonoid[MillProject] = ConfigMonoid.derive
}
