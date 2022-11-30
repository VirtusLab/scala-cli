package scala.cli.exportCmd

import java.nio.charset.StandardCharsets

import scala.build.options.ConfigMonoid
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
            ("ScalaJSModule", "import mill.scalajslib._", s"""def scalaJSVersion = "$ver"""" + nl)
          case None =>
            scalaNativeVersion match {
              case Some(ver) => (
                  "ScalaNativeModule",
                  "import mill.scalanativelib._",
                  s"""def scalaNativeVersion = "$ver"""" + nl
                )
              case None => ("ScalaModule", "", "")
            }
        }
    val maybeScalaVer = scalaVersion.fold("") { sv =>
      s"""def scalaVersion = "$sv"""" + nl
    }
    val maybeScalacOptions =
      if (scalacOptions.isEmpty) ""
      else {
        val optsString = scalacOptions.map(opt => s"\"$opt\"").mkString(", ")
        s"""def scalacOptions = super.scalacOptions() ++ Seq($optsString)"""
      }
    def maybeDeps(deps: Seq[String]) =
      if (deps.isEmpty) ""
      else {
        val depLen = deps.length
        "def ivyDeps = super.ivyDeps() ++ Seq(" + nl +
          deps
            .iterator
            .zipWithIndex
            .map {
              case (dep, idx) =>
                val maybeComma = if (idx == depLen - 1) "" else ","
                """  ivy"""" + dep + "\"" + maybeComma + nl
            }
            .mkString + nl +
          ")" + nl
      }

    val maybeScalaCompilerPlugins =
      if (scalaCompilerPlugins.isEmpty) ""
      else {
        val depLen = scalaCompilerPlugins.length
        "def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Seq(" + nl +
          scalaCompilerPlugins
            .iterator
            .zipWithIndex
            .map {
              case (dep, idx) =>
                val maybeComma = if (idx == depLen - 1) "" else ","
                """  ivy"""" + dep + "\"" + maybeComma + nl
            }
            .mkString + nl +
          ")" + nl
      }

    val maybeMain = mainClass.fold("") { mc =>
      s"""def mainClass = Some("$mc")""" + nl
    }

    val buildSc =
      s"""import mill._
         |import mill.scalalib._
         |$extraImports
         |
         |object $escapedName extends $parentModule {
         |  $maybeScalaVer
         |  $maybeScalacOptions
         |  $extraDecs
         |  ${maybeDeps(mainDeps)}
         |  $maybeScalaCompilerPlugins
         |  $maybeMain
         |  ${extraDecls.map("  " + _ + nl).mkString}
         |
         |  object test extends Tests {
         |    ${maybeDeps(testDeps)}
         |    ${extraTestDecls.map("  " + _ + nl).mkString}
         |  }
         |}
         |""".stripMargin

    for ((path, language, content) <- mainSources) {
      val path0 = dir / name / "src" / "main" / "scala" / path
      os.write(path0, content, createFolders = true)
    }
    for ((path, language, content) <- testSources) {
      val path0 = dir / name / "test" / "src" / "test" / path
      os.write(path0, content, createFolders = true)
    }

    os.write(dir / "build.sc", buildSc.getBytes(charSet))
  }
}

object MillProject {
  implicit val monoid: ConfigMonoid[MillProject] = ConfigMonoid.derive
}
