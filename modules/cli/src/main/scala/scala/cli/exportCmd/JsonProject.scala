package scala.cli.exportCmd

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, WriterConfig, writeToStream}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import java.io.PrintStream

import scala.build.info.{BuildInfo, ExportDependencyFormat, ScopedBuildInfo}
import scala.util.Using

final case class JsonProject(buildInfo: BuildInfo) extends Project {
  def sorted = this.copy(
    buildInfo = buildInfo.copy(
      scopes = buildInfo.scopes.map { case (k, v) => k -> v.sorted }
    )
  )

  def withEmptyScopesRemoved = this.copy(
    buildInfo = buildInfo.copy(
      scopes = buildInfo.scopes.filter(_._2 != ScopedBuildInfo.empty)
    )
  )

  def writeTo(dir: os.Path): Unit = {
    val config = WriterConfig.withIndentionStep(1)

    Using(os.write.outputStream(dir / "export.json")) {
      outputStream =>
        writeToStream(
          sorted.withEmptyScopesRemoved.buildInfo,
          outputStream,
          config
        )(
          using JsonProject.jsonCodecBuildInfo
        )
    }
  }

  def print(printStream: PrintStream): Unit = {
    val config = WriterConfig.withIndentionStep(1)

    writeToStream(
      sorted.withEmptyScopesRemoved.buildInfo,
      printStream,
      config
    )(
      using JsonProject.jsonCodecBuildInfo
    )
  }
}

extension (s: ScopedBuildInfo) {
  def sorted(using ord: Ordering[String]) = s.copy(
    s.sources.sorted,
    s.scalacOptions.sorted,
    s.scalaCompilerPlugins.sorted(using JsonProject.ordering),
    s.dependencies.sorted(using JsonProject.ordering),
    s.compileOnlyDependencies.sorted(using JsonProject.ordering),
    s.resolvers.sorted,
    s.resourceDirs.sorted,
    s.customJarsDecls.sorted
  )
}

object JsonProject {
  implicit lazy val jsonCodecBuildInfo: JsonValueCodec[BuildInfo]             = JsonCodecMaker.make
  implicit lazy val jsonCodecScopedBuildInfo: JsonValueCodec[ScopedBuildInfo] = JsonCodecMaker.make

  implicit val ordering: Ordering[ExportDependencyFormat] =
    Ordering.by(x => x.groupId + x.artifactId.fullName)
  implicit lazy val jsonCodecExportDependencyFormat: JsonValueCodec[ExportDependencyFormat] =
    JsonCodecMaker.make
}
