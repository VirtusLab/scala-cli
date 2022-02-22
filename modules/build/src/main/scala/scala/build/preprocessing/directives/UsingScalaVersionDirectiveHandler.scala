package scala.build.preprocessing.directives
import scala.build.{Logger, Positioned}
import scala.build.errors.{BuildException, NoScalaVersionProvidedError}
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.preprocessing.ScopePath

case object UsingScalaVersionDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Scala version"
  def description      = "Set the default Scala version"
  def usage            = "//> using scala _version_+"
  override def usageMd = "`//> using scala `_version_+"
  override def examples = Seq(
    "//> using scala \"3.0.2\"",
    "//> using scala \"2.13\"",
    "//> using scala \"2\"",
    "//> using scala \"2.13.6\", \"2.12.15\""
  )

  def keys = Seq("scala")
  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val values = directive.values

    println("directive values are: "+ values.toList.map(_.toString))
    val scalaVersions: Seq[(Positioned[String], Option[ScopePath])] =
      DirectiveUtil.stringValues(values, path, cwd)


    if (scalaVersions.isEmpty)
      Left(new NoScalaVersionProvidedError)
    else {
      val options = BuildOptions(
        scalaOptions = ScalaOptions(
          scalaVersion = scalaVersions.headOption.map(_._1.value),
          extraScalaVersions = scalaVersions.drop(1).map(_._1.value).toSet
        )
      )
      Right(ProcessedDirective(Some(options), Seq.empty))
    }
  }
}
