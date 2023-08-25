package scala.build.errors
import scala.build.Position

final class PasswordOptionError(message: String, positions: Seq[Position])
    extends BuildException(message, positions)
