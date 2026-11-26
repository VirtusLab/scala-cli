package scala.build.internal

import coursier.cache.FileCache
import coursier.util.Task

/** The `User-Agent` Scala CLI sends when coursier fetches metadata or artifacts.
  *
  * Repositories like Maven Central ask tools to identify themselves with a name, a version and a
  * contact, so that they don't rate-limit them as generic coursier traffic (see
  * https://central.sonatype.org/faq/429-tooling-provider/ and
  * https://github.com/coursier/coursier/discussions/3841). This follows the format coursier uses
  * for its own default agent:
  * {{{
  * Scala-CLI/1.19 (+https://github.com/VirtusLab/scala-cli) Coursier/2.1
  * Scala-CLI/1.19 (+https://github.com/VirtusLab/scala-cli; ci) Coursier/2.1
  * }}}
  * with a `ci` comment token when the `CI` environment variable is set. Like coursier's own default
  * agent, it is overridden outright by the `coursier.http.agent` Java property.
  */
object CoursierUserAgent:
  private def projectUrl = s"https://github.com/${Constants.ghOrg}/${Constants.ghName}"

  /** `major.minor` of `version`, if it starts with those */
  private[internal] def majorMinor(version: String): Option[String] =
    version.split("[.-]", 3).toSeq match
      case Seq(major, minor, _*)
          if major.forall(_.isDigit) && minor.forall(_.isDigit) &&
          major.nonEmpty && minor.nonEmpty =>
        Some(s"$major.$minor")
      case _ => None

  private def product(name: String, version: String): String =
    majorMinor(version).fold(name)(v => s"$name/$v")

  private def runningOnCi: Boolean = System.getenv("CI") != null

  /** The Scala CLI part of `User-Agent`s, like `Scala-CLI/1.19 (+https://github.com/…; ci)` */
  def scalaCliProduct(
    scalaCliVersion: String = Constants.version,
    isCi: Boolean = runningOnCi
  ): String =
    val comments = s"+$projectUrl" +: (if isCi then Seq("ci") else Nil)
    s"${product("Scala-CLI", scalaCliVersion)} (${comments.mkString("; ")})"

  private[internal] def userAgent(
    scalaCliVersion: String,
    coursierVersion: String,
    isCi: Boolean
  ): String =
    s"${scalaCliProduct(scalaCliVersion, isCi)} ${product("Coursier", coursierVersion)}"

  /** The `User-Agent` to send, honouring the `coursier.http.agent` Java property */
  lazy val value: String =
    sys.props.getOrElse(
      "coursier.http.agent",
      userAgent(
        scalaCliVersion = Constants.version,
        coursierVersion = coursier.util.Properties.version,
        isCi = runningOnCi
      )
    )

  extension (cache: FileCache[Task])
    /** `cache`, sending the Scala CLI `User-Agent` */
    def withScalaCliUserAgent: FileCache[Task] = cache.withUserAgent(value)

  /** A default coursier cache, sending the Scala CLI `User-Agent` */
  def defaultCache(): FileCache[Task] = FileCache().withScalaCliUserAgent
