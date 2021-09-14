package scala.cli.export

import java.nio.charset.StandardCharsets

final case class SbtProject(
  plugins: Seq[String],
  settings: Seq[Seq[String]],
  sbtVersion: String,
  mainSources: Seq[(os.SubPath, String, Array[Byte])],
  testSources: Seq[(os.SubPath, String, Array[Byte])]
) extends Project {
  def writeTo(dir: os.Path): Unit = {
    val nl      = System.lineSeparator()
    val charset = StandardCharsets.UTF_8

    val buildPropsContent = s"sbt.version=$sbtVersion" + nl
    os.write(
      dir / "project" / "build.properties",
      buildPropsContent.getBytes(charset),
      createFolders = true
    )

    if (plugins.nonEmpty) {
      val pluginsSbtContent = plugins
        .map { p =>
          s"addSbtPlugin($p)" + nl
        }
        .mkString
      os.write(dir / "project" / "plugins.sbt", pluginsSbtContent.getBytes(charset))
    }

    val buildSbtContent = settings
      .map { settings0 =>
        settings0.map(s => s + nl).mkString + nl
      }
      .mkString
    os.write(dir / "build.sbt", buildSbtContent.getBytes(charset))

    for ((path, language, content) <- mainSources) {
      val path0 = dir / "src" / "main" / language / path
      os.write(path0, content, createFolders = true)
    }
    for ((path, language, content) <- testSources) {
      val path0 = dir / "src" / "test" / language / path
      os.write(path0, content)
    }
  }
}
