package scala.build.blooprifle

import java.io.{File, InputStream, OutputStream}

import scala.build.blooprifle.internal.Constants
import scala.concurrent.duration._
import scala.util.Try

final case class BloopRifleConfig(
  host: String,
  port: Int,
  javaPath: String,
  javaOpts: Seq[String],
  classPath: String => Either[Throwable, Seq[File]],
  bspSocketOrPort: Option[() => BspConnectionAddress],
  bspStdin: Option[InputStream],
  bspStdout: Option[OutputStream],
  bspStderr: Option[OutputStream],
  period: FiniteDuration,
  timeout: FiniteDuration,
  startCheckPeriod: FiniteDuration,
  startCheckTimeout: FiniteDuration,
  initTimeout: FiniteDuration,
  acceptBloopVersion: Option[String => Boolean],
  acceptBloopJvm: Option[String => Boolean],
  retainedBloopVersion: String
)

object BloopRifleConfig {

  def hardCodedDefaultHost = "127.0.0.1"
  def hardCodedDefaultPort = 8212

  lazy val defaultHost: String = {
    val fromEnv   = Option(System.getenv("BLOOP_SERVER")).filter(_.nonEmpty)
    def fromProps = sys.props.get("bloop.server").filter(_.nonEmpty)
    fromEnv
      .orElse(fromProps)
      .getOrElse(hardCodedDefaultHost)
  }
  lazy val defaultPort: Int = {
    val fromEnv = Option(System.getenv("BLOOP_PORT"))
      .filter(_.nonEmpty)
      .flatMap(s => Try(s.toInt).toOption)
    def fromProps = sys.props.get("bloop.port")
      .filter(_.nonEmpty)
      .flatMap(s => Try(s.toInt).toOption)
    fromEnv
      .orElse(fromProps)
      .getOrElse(hardCodedDefaultPort)
  }

  // from https://github.com/scalacenter/bloop/blob/cbddb8baaf639a4e08ee630f1ebc559dc70255a8/bloopgun/src/main/scala/bloop/bloopgun/util/Environment.scala#L89-L93
  def hardCodedDefaultJavaOpts: Seq[String] =
    Seq(
      "-Xss4m",
      "-XX:MaxInlineLevel=20", // Specific option for faster C2, ignored by GraalVM
      "-XX:+UseParallelGC"     // Full parallel GC is the best choice for Scala compilation
    )

  lazy val defaultJavaOpts: Seq[String] = {
    val fromEnv   = Option(System.getenv("BLOOP_JAVA_OPTS")).filter(_.nonEmpty)
    def fromProps = sys.props.get("bloop.java-opts").filter(_.nonEmpty)
    fromEnv
      .orElse(fromProps)
      .map(_.split("\\s+").toSeq)
      .getOrElse(hardCodedDefaultJavaOpts)
  }

  def hardCodedDefaultModule: String =
    "ch.epfl.scala:bloop-frontend_2.12"
  def hardCodedDefaultVersion: String =
    Constants.bloopVersion
  def hardCodedDefaultScalaVersion: String =
    Constants.bloopScalaVersion

  lazy val defaultModule: String = {
    val fromEnv   = Option(System.getenv("BLOOP_MODULE")).map(_.trim).filter(_.nonEmpty)
    def fromProps = sys.props.get("bloop.module").map(_.trim).filter(_.nonEmpty)
    fromEnv
      .orElse(fromProps)
      .getOrElse(hardCodedDefaultModule)
  }
  lazy val defaultVersion: String = {
    val fromEnv   = Option(System.getenv("BLOOP_VERSION")).filter(_.nonEmpty)
    def fromProps = sys.props.get("bloop.version").filter(_.nonEmpty)
    fromEnv
      .orElse(fromProps)
      .getOrElse(hardCodedDefaultVersion)
  }
  lazy val defaultScalaVersion: String = {
    val fromEnv   = Option(System.getenv("BLOOP_SCALA_VERSION")).filter(_.nonEmpty)
    def fromProps = sys.props.get("bloop.scala-version").filter(_.nonEmpty)
    fromEnv
      .orElse(fromProps)
      .getOrElse(hardCodedDefaultScalaVersion)
  }

  def default(
    bloopClassPath: String => Either[Throwable, Seq[File]]
  ): BloopRifleConfig =
    BloopRifleConfig(
      host = defaultHost,
      port = defaultPort,
      javaPath = "java",
      javaOpts = defaultJavaOpts,
      classPath = bloopClassPath,
      bspSocketOrPort = None,
      bspStdin = None,
      bspStdout = None,
      bspStderr = None,
      period = 100.milliseconds,
      timeout = 10.seconds,
      startCheckPeriod = 100.millis,
      startCheckTimeout = 1.minute,
      initTimeout = 30.seconds,
      acceptBloopVersion = None,
      acceptBloopJvm = None,
      retainedBloopVersion = Constants.bloopVersion
    )
}
