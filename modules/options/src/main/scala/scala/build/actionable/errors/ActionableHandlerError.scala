package scala.build.actionable.errors

import scala.build.errors.BuildException

final class ActionableHandlerError(message: String)
    extends BuildException(message)
