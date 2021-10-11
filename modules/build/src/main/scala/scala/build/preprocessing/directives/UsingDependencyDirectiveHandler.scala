package scala.build.preprocessing.directives

import dependency.parser.DependencyParser

import scala.build.errors.{BuildException, DependencyFormatError}
import scala.build.options.{BuildOptions, ClassPathOptions}

case object UsingDependencyDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Dependency"
  def description      = "Adds dependencies"
  def usage            = "using org:name:ver"
  override def usageMd = "`using `_org_`:`name`:`ver"
  override def examples = Seq(
    "using org.typelevel::cats-effect:3.2.9",
    "using dev.zio::zio:1.0.12"
  )

  def handle(directive: Directive): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq(depStr) if depStr.split(":").count(_.trim.nonEmpty) == 3 =>
        val res =
          DependencyParser.parse(depStr) match {
            case Left(err) => Left(new DependencyFormatError(depStr, err))
            case Right(dep) =>
              val options = BuildOptions(
                classPathOptions = ClassPathOptions(
                  extraDependencies = Seq(dep)
                )
              )
              Right(options)
          }
        Some(res)
      case _ => None
    }
}
