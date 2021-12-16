package scala.build.preprocessing.directives
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaNativeOptions}
import scala.build.preprocessing.ScopePath

case object UsingScalaNativeOptionsDirectiveHandler extends UsingDirectiveHandler {
  def name: String = "Scala Native options"

  def description: String = "Add Scala Native options"

  def usage: String = "// using nativeGc _value_ | using native-version _value_"

  override def usageMd: String =
    """`// using nativeGc` _value_
      |
      |`// using nativeVersion` _value_
      |
      |`// using nativeCompile` _value1_, _value2_
      |
      |`// using nativeLinking` _value1_, _value2_""".stripMargin

  override def examples: Seq[String] = Seq(
    "// using nativeVersion \"0.4.0\""
  )

  override def keys: Seq[String] =
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

  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, ProcessedUsingDirective] = {
    val scalaNativeOptions =
      DirectiveUtil.numericValues(directive.values, path, cwd).toList ++
        DirectiveUtil.stringValues(directive.values, path, cwd).toList
    val intermediate = directive.key match {
      case "native-gc" | "nativeGc" if scalaNativeOptions.size == 1 =>
        Right(BuildOptions(
          scalaNativeOptions = ScalaNativeOptions(
            gcStr = Some(scalaNativeOptions.head._1)
          )
        ))
      case "native-version" | "nativeVersion" if scalaNativeOptions.size == 1 =>
        Right(BuildOptions(
          scalaNativeOptions = ScalaNativeOptions(
            version = Some(scalaNativeOptions.head._1)
          )
        ))
      case "native-compile" | "nativeCompile" => Right(
          BuildOptions(
            scalaNativeOptions = ScalaNativeOptions(
              compileOptions = scalaNativeOptions.map(_._1)
            )
          )
        )
      case "native-linking" | "nativeLinking" =>
        val res = Right(BuildOptions(
          scalaNativeOptions = ScalaNativeOptions(
            linkingOptions = scalaNativeOptions.map(_._1)
          )
        ))
        res
      case "native-version" | "nativeVersion" =>
        Left(SingleValueExpected("native-version", scalaNativeOptions.map(_._1)))
      case "native-gc" | "nativeGc" =>
        Left(SingleValueExpected("native-gc", scalaNativeOptions.map(_._1)))
    }
    intermediate.map { bo =>
      ProcessedDirective(Some(bo), Seq.empty)
    }
  }

}

final case class SingleValueExpected(param: String, values: Seq[String]) extends BuildException(
      s"Expected single value for $param but found $values"
    )
