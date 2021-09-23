package scala.build.preprocessing.directives

import dependency.parser.DependencyParser

import scala.build.options.{BuildOptions, ClassPathOptions}

case object UsingDependencyDirectiveHandler extends UsingDirectiveHandler {
  def handle(directive: Directive): Option[Either[String, BuildOptions]] =
    directive.values match {
      case Seq(depStr) if depStr.split(":").count(_.trim.nonEmpty) == 3 =>
        val res =
          DependencyParser.parse(depStr) match {
            case Left(err) => Left(s"Error parsing dependency '$depStr': $err")
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
