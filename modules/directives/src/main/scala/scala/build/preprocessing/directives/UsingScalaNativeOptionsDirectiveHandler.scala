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
      |`//> using nativeMode` _value_
      |
      |`//> using nativeVersion` _value_
      |
      |`//> using nativeCompile` _value1_, _value2_
      |
      |`//> using nativeLinking` _value1_, _value2_
      |
      |`//> using nativeClang` _value_
      |
      |`//> using nativeClangPP` _value_""".stripMargin

  override def examples: Seq[String] = Seq(
    "//> using nativeVersion \"0.4.0\""
  )

  def keys: Seq[String] =
    Seq(
      "native-gc",
      "native-mode",
      "native-version",
      "native-compile",
      "native-linking",
      "native-clang",
      "native-clang-pp",
      "nativeGc",
      "nativeMode",
      "nativeVersion",
      "nativeCompile",
      "nativeLinking",
      "nativeClang",
      "nativeClangPP"
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

    def withOneValue(setter: String => ScalaNativeOptions)
      : Either[BuildException, ScalaNativeOptions] =
      values match {
        case Seq(value0) =>
          Right(setter(value0._1.value))
        case Seq() =>
          assert(directive.numericalOrStringValuesCount == 0)
          Left(new NoValueProvidedError(directive.key))
        case _ =>
          Left(new SingleValueExpectedError(directive, path))
      }

    val scalaNativeOptions = directive.key match {
      case "native-gc" | "nativeGc" =>
        value(withOneValue { value =>
          ScalaNativeOptions(gcStr = Some(value))
        })
      case "native-mode" | "nativeMode" =>
        value(withOneValue { value =>
          ScalaNativeOptions(modeStr = Some(value))
        })
      case "native-version" | "nativeVersion" =>
        value(withOneValue { value =>
          ScalaNativeOptions(version = Some(value))
        })
      case "native-compile" | "nativeCompile" =>
        ScalaNativeOptions(
          compileOptions = values.map(_._1.value)
        )
      case "native-linking" | "nativeLinking" =>
        ScalaNativeOptions(
          linkingOptions = values.map(_._1.value)
        )
      case "native-clang" | "nativeClang" =>
        value(withOneValue { value =>
          ScalaNativeOptions(clang = Some(value))
        })
      case "native-clang-pp" | "nativeClangPP" =>
        value(withOneValue { value =>
          ScalaNativeOptions(clangpp = Some(value))
        })
    }
    val options = BuildOptions(scalaNativeOptions = scalaNativeOptions)
    ProcessedDirective(Some(options), Nil)
  }
}
