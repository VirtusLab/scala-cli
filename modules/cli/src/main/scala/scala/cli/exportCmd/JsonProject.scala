package scala.cli.exportCmd

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, WriterConfig, writeToStream}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import coursier.Dependency
import coursier.util.Artifact

import java.nio.charset.StandardCharsets

import scala.build.options.ConfigMonoid
import scala.cli.util.SeqHelpers.*
import scala.reflect.NameTransformer
import scala.util.{Properties, Using}

final case class JsonProject(
  projectName: Option[String] = None,
  scalaVersion: Option[String] = None,
  platform: Option[String] = None,
  scalacOptions: Seq[String] = Nil,
  scalaCompilerPlugins: Seq[String] = Nil,
  scalaJsVersion: Option[String] = None,
  scalaNativeVersion: Option[String] = None,
  mainDeps: Seq[ExportDependencyFormat] = Nil,
  testDeps: Seq[ExportDependencyFormat] = Nil,
  mainSources: Seq[String] = Nil,
  testSources: Seq[String] = Nil,
  resolvers: Seq[ExportResolverFormat] = Nil,
  extraDecls: Seq[String] = Nil,
  resourcesDirs: Seq[String] = Nil,
  mainClass: Option[String] = None
) extends Project {

  def +(other: JsonProject): JsonProject =
    JsonProject.monoid.orElse(this, other)

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

case class ExportResolverFormat(
  name: String,
  location: Option[String],
  user: Option[String] = None,
  password: Option[String] = None,
  headers: Seq[(String, String)] = Nil
)

object ExportResolverFormat {
  implicit lazy val jsonCodec: JsonValueCodec[ExportResolverFormat] =
    JsonCodecMaker.make(CodecMakerConfig
      .withTransientNone(false)
      .withTransientEmpty(false))
  implicit lazy val StringSeqCodec: JsonValueCodec[Seq[String]] = JsonCodecMaker.make
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

  implicit val ordering: Ordering[ExportDependencyFormat] =
    Ordering.by(x => x.groupId + x.artifactId.fullName)
  implicit lazy val jsonCodec: JsonValueCodec[ExportDependencyFormat] = JsonCodecMaker.make
}
