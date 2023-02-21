package scala.cli.exportCmd

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, WriterConfig, writeToStream}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import coursier.Dependency
import coursier.util.Artifact
import dependency.AnyDependency

import java.nio.charset.StandardCharsets

import scala.build.options.ConfigMonoid
import scala.cli.util.SeqHelpers.*
import scala.reflect.NameTransformer
import scala.util.{Properties, Using}

final case class JsonProject(
  projectName: Option[String] = None,
  scalaVersion: Option[String] = None,
  platform: Option[String] = None,
  jvmVersion: Option[String] = None,
  scalaJsVersion: Option[String] = None,
  jsEsVersion: Option[String] = None,
  scalaNativeVersion: Option[String] = None,
  mainClass: Option[String] = None,
  scopes: Map[String, ScopedJsonProject] = Map.empty
) extends Project {

  def +(other: JsonProject): JsonProject =
    JsonProject.monoid.orElse(this, other)

  def withScope(scopeName: String, scopedJsonProj: ScopedJsonProject): JsonProject =
    if (scopedJsonProj.sources.isEmpty)
      this
    else
      this.copy(
        scopes = this.scopes + (scopeName -> scopedJsonProj)
      )

  def writeTo(dir: os.Path): Unit = {
    val config = WriterConfig.withIndentionStep(1)

    Using(os.write.outputStream(dir / "export.json")) {
      outputStream =>
        writeToStream(
          this,
          outputStream,
          config
        )
    }
  }
}

final case class ScopedJsonProject(
  sources: Seq[String] = Nil,
  scalacOptions: Seq[String] = Nil,
  scalaCompilerPlugins: Seq[ExportDependencyFormat] = Nil,
  dependencies: Seq[ExportDependencyFormat] = Nil,
  resolvers: Seq[String] = Nil,
  resourcesDirs: Seq[String] = Nil,
  customJarsDecls: Seq[String] = Nil
) {

  def +(other: ScopedJsonProject): ScopedJsonProject =
    ScopedJsonProject.monoid.orElse(this, other)

  def sorted(using ord: Ordering[String]): ScopedJsonProject =
    ScopedJsonProject(
      this.sources.sorted,
      this.scalacOptions,
      this.scalaCompilerPlugins.sorted,
      this.dependencies.sorted,
      this.resolvers.sorted,
      this.resourcesDirs.sorted,
      this.customJarsDecls.sorted
    )
}

object ScopedJsonProject {
  implicit val monoid: ConfigMonoid[ScopedJsonProject]           = ConfigMonoid.derive
  implicit lazy val jsonCodec: JsonValueCodec[ScopedJsonProject] = JsonCodecMaker.make
}

object JsonProject {
  implicit val monoid: ConfigMonoid[JsonProject]           = ConfigMonoid.derive
  implicit lazy val jsonCodec: JsonValueCodec[JsonProject] = JsonCodecMaker.make
}

case class ExportDependencyFormat(groupId: String, artifactId: ArtifactId, version: String)

case class ArtifactId(name: String, fullName: String)

object ExportDependencyFormat {
  def apply(dep: Dependency): ExportDependencyFormat = {
    val scalaVersionStartIndex = dep.module.name.value.lastIndexOf('_')
    val shortDepName = if (scalaVersionStartIndex == -1)
      dep.module.name.value
    else
      dep.module.name.value.take(scalaVersionStartIndex)
    new ExportDependencyFormat(
      dep.module.organization.value,
      ArtifactId(shortDepName, dep.module.name.value),
      dep.version
    )
  }

  def apply(
    dep: AnyDependency,
    scalaParamsOpt: Option[dependency.ScalaParameters]
  ): ExportDependencyFormat = {
    import scala.build.internal.Util.*
    dep.toCs(scalaParamsOpt)
      .map(ExportDependencyFormat.apply)
      .getOrElse(
        ExportDependencyFormat(
          dep.module.organization,
          ArtifactId(dep.module.name, dep.module.name),
          dep.version
        )
      )
  }

  implicit val ordering: Ordering[ExportDependencyFormat] =
    Ordering.by(x => x.groupId + x.artifactId.fullName)
  implicit lazy val jsonCodec: JsonValueCodec[ExportDependencyFormat] = JsonCodecMaker.make
}
