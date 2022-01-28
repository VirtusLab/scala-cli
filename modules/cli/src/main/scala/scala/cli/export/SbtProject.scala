package scala.cli.export

import java.nio.charset.StandardCharsets

import scala.build.options.ConfigMonoid

final case class SbtProject(
  plugins: Seq[String] = Nil,
  imports: Seq[String] = Nil,
  settings: Seq[Seq[String]] = Nil,
  sbtVersion: Option[String] = None,
  mainSources: Seq[(os.SubPath, String, Array[Byte])] = Nil,
  testSources: Seq[(os.SubPath, String, Array[Byte])] = Nil
) extends Project {

  def +(other: SbtProject): SbtProject =
    SbtProject.monoid.orElse(this, other)

  def writeTo(dir: os.Path): Unit = {
    val nl      = System.lineSeparator()
    val charset = StandardCharsets.UTF_8

    for (ver <- sbtVersion) {
      val buildPropsContent = s"sbt.version=$ver" + nl
      os.write(
        dir / "project" / "build.properties",
        buildPropsContent.getBytes(charset),
        createFolders = true
      )
    }

    if (plugins.nonEmpty) {
      val pluginsSbtContent = plugins
        .map { p =>
          s"addSbtPlugin($p)" + nl
        }
        .mkString
      os.write(dir / "project" / "plugins.sbt", pluginsSbtContent.getBytes(charset))
    }

    val buildSbtImportsContent = imports.map(_ + nl).mkString
    val buildSbtSettingsContent = settings
      .filter(_.nonEmpty)
      .map { settings0 =>
        settings0.map(s => s + nl).mkString + nl
      }
      .mkString
    val buildSbtContent = buildSbtImportsContent + buildSbtSettingsContent
    os.write(dir / "build.sbt", buildSbtContent.getBytes(charset))

    for ((path, language, content) <- mainSources) {
      val path0 = dir / "src" / "main" / language / path
      os.write(path0, content, createFolders = true)
    }
    for ((path, language, content) <- testSources) {
      val path0 = dir / "src" / "test" / language / path
      os.write(path0, content, createFolders = true)
    }
  }
}

object SbtProject {
  implicit val monoid: ConfigMonoid[SbtProject] = ConfigMonoid.derive
}
