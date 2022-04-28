package scala.build.preprocessing.directives
import scala.build.Ops._
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  MalformedPlatformError,
  UnexpectedJvmPlatformVersionError
}
import scala.build.options._
import scala.build.{Logger, Positioned}
import org.scalameta.logger

case object UsingPlatformDirectiveHandler extends BuildOptionsUsingDirectiveHandler[::[Positioned[String]]] {
  def name              = "Platform"
  def description       = "Set the default platform to Scala.js or Scala Native"
  override def usageMd  = "`//> using platform `(`jvm`|`scala-js`|`scala-native`)+"
  override def examples = Seq(
    "//> using platform \"scala-js\"",
    "//> using platform \"jvm\", \"scala-native\""
  )

  def usagesCode = Seq("//> using platform (jvm|scala-js[:version]|scala-native[:version])+")

  def keys = Seq("platform", "platforms")

  def constrains = AtLeastOne(ValueType.String)

  case class ParsedPlatform(platform: Platform, version: Option[String])
  
  private def split(input: String): (String, Option[String]) = {
    val idx = input.indexOf(':')
    if (idx < 0) (input, None)
    else (input.take(idx), Some(input.drop(idx + 1)))
  }


  def process(values: ::[Positioned[String]])(using ctx: Ctx) = {
    val parsed = values.map { strValue =>
      val (platformStr, versionOpt) = split(strValue.value)
      Platform.parse(Platform.normalize(platformStr)) match {
        case None => strValue.error("Invalid platform, supported: `js`, `jvm` and `native`")
        case Some(Platform.JVM) if versionOpt.nonEmpty =>
          strValue.error(s"JVM platform does not accept version, plese use just `$platformStr`")
        case Some(platform) =>
          Right(strValue.map(_ => ParsedPlatform(platform, versionOpt)))        
      }
    }.sequenceToComposite
    parsed.flatMap { values =>
      val byPlatform = values.groupBy(_.value.platform)

      val problems = byPlatform.filter(_._2.size > 1).map { case (platform, values) =>
        val (noVersion, versioned) = values.map(_.map(_.version)).partition(_.value.nonEmpty)
        def warn(p: Positioned[_]): Unit = ctx.logger.diagnostic(s"Duplicated definition for platform $platform", positions = p.positions)
        versioned match
          case Seq() => Right(values.tail.foreach(warn)) // multiple directives without version, just warn
          case Seq(single) => Right(noVersion.tail.foreach(warn)) // single directice with version rest without - warn on those whitout version
          case declaration +: duplications=>
            duplications.map(_.error(s"Version for $platform is alredy defined")).sequenceToComposite
      }.toSeq.sequenceToComposite

      problems.map { _ =>
        val mainPlatfrom = values.head.map(_.platform)
        val extraPlatforms = (byPlatform - mainPlatfrom.value).map{ (k, v) => k -> v.head.map(_ => ()) }
        val jsVersion = byPlatform(Platform.JS).flatMap(_.value.version).headOption
        val nativeVersion = byPlatform(Platform.Native).flatMap(_.value.version).headOption
        BuildOptions(
          scalaOptions = ScalaOptions(platform = Some(mainPlatfrom), extraPlatforms = extraPlatforms),
          scalaJsOptions = ScalaJsOptions(version = jsVersion),
          scalaNativeOptions = ScalaNativeOptions(version = nativeVersion)
        )
      }
    }
  }
}