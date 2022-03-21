package scala.build.errors

import scala.build.preprocessing.directives.{DirectiveUtil, StrictDirective}

final class SingleValueExpectedError(
  val directive: StrictDirective,
  val path: Either[String, os.Path]
) extends BuildException(
      s"Expected a single value for directive ${directive.key} " +
        s"(got ${directive.values.length} values: ${directive.values.map(_.get().toString).mkString(", ")})",
      positions = DirectiveUtil.positions(directive.values, path)
    ) {
  assert(directive.numericalOrStringValuesCount > 1)
}
