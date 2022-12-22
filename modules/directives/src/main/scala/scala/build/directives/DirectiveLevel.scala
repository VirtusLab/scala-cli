package scala.build.directives

import scala.annotation.StaticAnnotation
import scala.cli.commands.SpecificationLevel

final case class DirectiveLevel(level: SpecificationLevel) extends StaticAnnotation
