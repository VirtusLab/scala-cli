package scala.build.directives

import scala.annotation.StaticAnnotation

final case class DirectivePrefix(prefix: String)
    extends StaticAnnotation
