package scala.build.options.validation

import scala.build.Position
import scala.build.errors.BuildException
import scala.build.options.BuildOptions

trait BuildOptionsRule {
  def validate(options: BuildOptions): Either[BuildException, Unit]
}

object BuildOptionsRule {
  def validateAll(options: BuildOptions): Either[BuildException, Unit] =
    List(JvmOptionsForNonJvmBuild).map(_.validate(options)).partition(_.isLeft) match {
      case (Nil, _)  => Right(())
      case (some, _) => some.head
    }
}

class ValidationException(
  message: String,
  positions: Seq[Position] = Nil
) extends BuildException(message, positions)

object JvmOptionsForNonJvmBuild extends BuildOptionsRule {
  def validate(options: BuildOptions): Either[BuildException, Unit] = {
    val jvmOptions = options.javaOptions.javaOpts.find(p => p.value.nonEmpty)
    if (jvmOptions.nonEmpty && options.platform.value != scala.build.options.Platform.JVM)
      Left(new ValidationException(
        "Conflicting options. Jvm Options are valid only for jvm platform.",
        options.platform.positions ++ jvmOptions.get.positions
      ))
    else Right(())
  }
}
