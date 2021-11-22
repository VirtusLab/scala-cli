package scala.build.options.validation

import scala.build.Position
import scala.build.errors.{BuildException, Severity}
import scala.build.options.BuildOptions

trait BuildOptionsRule {
  def validate(options: BuildOptions): Seq[ValidationException]
}

object BuildOptionsRule {
  def validateAll(options: BuildOptions): Seq[ValidationException] =
    List(JvmOptionsForNonJvmBuild).flatMap(_.validate(options))
}

class ValidationException(
  message: String,
  positions: Seq[Position] = Nil,
  severity0: Severity
) extends BuildException(message, positions) {
  def severity = this.severity0
}

object JvmOptionsForNonJvmBuild extends BuildOptionsRule {
  def validate(options: BuildOptions): List[ValidationException] = {
    val jvmOptions = options.javaOptions.javaOpts.find(p => p.value.nonEmpty)
    if (jvmOptions.nonEmpty && options.platform.value != scala.build.options.Platform.JVM)
      List(new ValidationException(
        "Conflicting options. Jvm Options are valid only for jvm platform.",
        options.platform.positions ++ jvmOptions.get.positions,
        Severity.WARNING
      ))
    else Nil
  }
}
