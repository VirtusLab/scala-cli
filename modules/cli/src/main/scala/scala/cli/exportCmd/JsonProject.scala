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
  mainClass: Option[String] = None,
  scopes: Seq[ScopedJsonProject] = Nil,
  extraDecls: Seq[String] = Nil
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

final case class ScopedJsonProject(
  scopeName: Option[String] = None,
  sources: Seq[String] = Nil,
  dependencies: Seq[String] = Nil,
  resolvers: Seq[String] = Nil,
  resourcesDirs: Seq[String] = Nil,
  extraDecls: Seq[String] = Nil
) extends Project {

  def +(other: ScopedJsonProject): ScopedJsonProject =
    ScopedJsonProject.monoid.orElse(this, other)

  def writeTo(dir: os.Path): Unit = {
    val config = WriterConfig.withIndentionStep(1)

    Using(os.write.outputStream(dir / s"${scopeName.getOrElse("")}_export.json")) {
      outputStream =>
        writeToStream(
          this,
          outputStream,
          config
        )
    }
  }

  def sorted(using ord: Ordering[String]): ScopedJsonProject =
    ScopedJsonProject(
      this.scopeName,
      this.sources.sorted,
      this.dependencies.sorted,
      this.resolvers.sorted,
      this.resourcesDirs.sorted,
      this.extraDecls.sorted
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
