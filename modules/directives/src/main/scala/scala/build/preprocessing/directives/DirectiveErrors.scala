package scala.build.preprocessing.directives

import scala.build.Position

final class DirectiveErrors(errors: ::[String], positions: Seq[Position.File])
    extends scala.cli.directivehandler.DirectiveException(
      "Directives errors: " + errors.mkString(", "),
      positions = positions.map { f =>
        scala.cli.directivehandler.Position.File(f.path, f.startPos, f.endPos)
      }
    )
