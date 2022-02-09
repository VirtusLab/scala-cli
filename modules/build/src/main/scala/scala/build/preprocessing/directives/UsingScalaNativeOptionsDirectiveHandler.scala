package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.{BuildException, NoValueProvidedError, SingleValueExpectedError}
import scala.build.options.{BuildOptions, ScalaNativeOptions}
import scala.build.preprocessing.ScopePath

case object UsingScalaNativeOptionsDirectiveHandler extends UsingDirectiveHandler {
  def name: String = "Scala Native options"

  def description: String = "Add Scala Native options"

  def usage: String = "//> using nativeGc _value_ | using native-version _value_"

  override def usageMd: String =
    """`//> using nativeGc` _value_
      |
      |`//> using nativeVersion` _value_
      |
      |`//> using nativeCompile` _value1_, _value2_
      |
      |`//> using nativeLinking` _value1_, _value2_""".stripMargin

  override def examples: Seq[String] = Seq(
    "//> using nativeVersion \"0.4.0\""
  )

  def keys: Seq[String] =
    Seq(
      "native-gc",
      "native-version",
      "native-compile",
      "native-linking",
      "nativeGc",
      "nativeVersion",
      "nativeCompile",
      "nativeLinking"
    )

  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = either {
    val values =
      DirectiveUtil.numericValues(directive.values, path, cwd).toList ++
        DirectiveUtil.stringValues(directive.values, path, cwd).toList
    val scalaNativeOptions = directive.key match {
      case "native-gc" | "nativeGc" =>
        values match {
          case Seq(value0) =>
            ScalaNativeOptions(
              gcStr = Some(value0._1.value)
            )
          case Seq() =>
            value(Left(new NoValueProvidedError(directive)))
          case _ =>
            value(Left(new SingleValueExpectedError(directive, path)))
        }
      case "native-version" | "nativeVersion" =>
        values match {
          case Seq(value0) =>
            ScalaNativeOptions(
              version = Some(value0._1.value)
            )
          case Seq() =>
            value(Left(new NoValueProvidedError(directive)))
          case _ =>
            value(Left(new SingleValueExpectedError(directive, path)))
        }
      case "native-compile" | "nativeCompile" =>
        ScalaNativeOptions(
          compileOptions = values.map(_._1.value)
        )
      case "native-linking" | "nativeLinking" =>
        ScalaNativeOptions(
          linkingOptions = values.map(_._1.value)
        )
    }
    val options = BuildOptions(scalaNativeOptions = scalaNativeOptions)
    ProcessedDirective(Some(options), Nil)
  }
}
