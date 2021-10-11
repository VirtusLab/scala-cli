package scala.build.preprocessing.directives

import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
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
          parseDependency(depStr).map { dep =>
            BuildOptions(
              classPathOptions = ClassPathOptions(
                extraDependencies = Seq(dep)
              )
            )
          }
        Some(res)
      case _ => None
    }

  private def parseDependency(depStr: String): Either[BuildException, AnyDependency] =
    DependencyParser.parse(depStr)
      .left.map(err => new DependencyFormatError(depStr, err))

  override def keys = Seq("lib")
  override def handleValues(values: Seq[Any]): Either[BuildException, BuildOptions] = either {

    val extraDependencies = value {
      DirectiveUtil.stringValues(values)
        .map { dep =>
          // Really necessary? (might already be handled by the coursier-dependency library)
          val dep0 = dep.filter(!_.isSpaceChar)

          parseDependency(dep0)
        }
        .sequence
        .left.map(errors => errors.mkString(", "))
    }

    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraDependencies = extraDependencies
      )
    )
  }
}
