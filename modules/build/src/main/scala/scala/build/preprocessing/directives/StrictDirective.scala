package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

case class StrictDirective(
  key: String,
  values: Seq[Value[_]]
)
