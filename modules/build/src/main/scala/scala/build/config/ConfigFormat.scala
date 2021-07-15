package scala.build.config

import dependency.parser.DependencyParser
import pureconfig.ConfigReader

import scala.build.{Build, Os}
import scala.build.config.reader.DerivedConfigReader
import scala.build.options.{BuildOptions, ClassPathOptions, JavaOptions, ScalaOptions}

final case class ConfigFormat(
  scala: Scala = Scala(),
  scalaJs: ScalaJs = ScalaJs(),
  jvm: Option[String] = None,
  jvmIndex: Option[String] = None,
  java: Java = Java(),
  dependencies: List[String] = Nil,
  repositories: List[String] = Nil,
  extraJars: List[String] = Nil,
  extraCompileOnlyJars: List[String] = Nil,
  extraSourceJars: List[String] = Nil
) {
  def buildOptions: BuildOptions =
    BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = scala.version,
        scalaBinaryVersion = scala.binaryVersion,
        scalacOptions = scala.options
      ),
      javaOptions = JavaOptions(
        javaHomeOpt = java.home.map(os.Path(_, Os.pwd)),
        jvmIdOpt = jvm,
        jvmIndexOpt = jvmIndex
      ),
      classPathOptions = ClassPathOptions(
        extraDependencies = dependencies.filter(_.nonEmpty).map { depStr =>
          DependencyParser.parse(depStr) match {
            case Left(err) => sys.error(s"Error parsing dependency '$depStr': $err")
            case Right(dep) => dep
          }
        },
        extraRepositories = repositories.filter(_.nonEmpty),
        extraJars = extraJars.map(p => os.Path(p, Os.pwd)),
        extraCompileOnlyJars = extraCompileOnlyJars.map(p => os.Path(p, Os.pwd)),
        extraSourceJars = extraSourceJars.map(p => os.Path(p, Os.pwd))
      )
    )
}

object ConfigFormat {
  implicit val reader = DerivedConfigReader[ConfigFormat]
}
