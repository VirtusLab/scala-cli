package scala.build.directives

import scala.annotation.StaticAnnotation

final case class DirectiveDescription(description: String, descriptionMd: String = "")
    extends StaticAnnotation
