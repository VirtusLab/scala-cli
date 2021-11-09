package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

import scala.build.errors.{BuildException, NoScalaVersionProvidedError}
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.build.preprocessing.ScopePath

case object UsingScalaVersionDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Scala version"
  def description      = "Set the default Scala version"
  def usage            = "using scala _version_+"
  override def usageMd = "`using scala `_version_+"
  override def examples = Seq(
    "using scala 3.0.2",
    "using scala 2.13",
    "using scala 2",
    "using scala 2.13.6 2.12.15"
  )

  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq("scala", scalaVersions @ _*) if scalaVersions.nonEmpty =>
        val options = BuildOptions(
          scalaOptions = ScalaOptions(
            scalaVersion = Some(scalaVersions.head),
            extraScalaVersions = scalaVersions.tail.toSet
          )
        )
        // TODO Validate that scalaVer looks like a version?
        Some(Right(options))
      case _ =>
        None
    }

  override def keys = Seq("scala")
  override def handleValues(
    values: Seq[Value[_]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] = {
    val scalaVersions = DirectiveUtil.stringValues(values, path)
    if (scalaVersions.isEmpty)
      Left(new NoScalaVersionProvidedError)
    else {
      val options = BuildOptions(
        scalaOptions = ScalaOptions(
          scalaVersion = Some(scalaVersions.head._1),
          extraScalaVersions = scalaVersions.tail.map(_._1).toSet
        )
      )
      Right(options)
    }
  }
}
