package scala.build.directives

import scala.annotation.StaticAnnotation

final case class DirectiveUsage(usage: String, usageMd: String = "") extends StaticAnnotation
