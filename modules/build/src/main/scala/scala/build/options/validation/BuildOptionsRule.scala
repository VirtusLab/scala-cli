package scala.build.options.validation
import scala.build.errors.{BuildException, Diagnostic, Severity}
import scala.build.options.BuildOptions

trait BuildOptionsRule {
  def validate(options: BuildOptions): Seq[Diagnostic]
}

object BuildOptionsRule {
  def validateAll(options: BuildOptions): Seq[Diagnostic] =
    List(JvmOptionsForNonJvmBuild).flatMap(_.validate(options))
}

class ValidationException(
  diagnostic: Diagnostic
) extends BuildException(diagnostic.message, diagnostic.positions)

object JvmOptionsForNonJvmBuild extends BuildOptionsRule {
  def validate(options: BuildOptions): List[Diagnostic] = {
    val jvmOptions = options.javaOptions.javaOpts.find(p => p.value.nonEmpty)
    if (jvmOptions.nonEmpty && options.platform.value != scala.build.options.Platform.JVM)
      List(Diagnostic(
        "Conflicting options. Jvm Options are valid only for jvm platform.",
        Severity.Warning,
        options.platform.positions ++ jvmOptions.get.positions
      ))
    else Nil
  }
}
