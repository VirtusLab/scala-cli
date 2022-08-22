package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaNativeOptions}
import scala.build.preprocessing.directives.UsingDirectiveValueKind.UsingDirectiveValueKind

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

  override def isRestricted = false

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

  override def getValueNumberBounds(key: String): UsingDirectiveValueNumberBounds = key match {
    case "native-gc" | "nativeGc"     => UsingDirectiveValueNumberBounds(1, 1)
    case "native-mode" | "nativeMode" => UsingDirectiveValueNumberBounds(1, 1)
    case "native-clang" | "native-clang-pp" | "nativeClang" | "nativeClangPP" =>
      UsingDirectiveValueNumberBounds(1, 1)
    case "native-version" | "nativeVersion" => UsingDirectiveValueNumberBounds(1, 1)
    case "native-linking" | "nativeLinking" => UsingDirectiveValueNumberBounds(1, Int.MaxValue)
    case "native-compile" | "nativeCompile" => UsingDirectiveValueNumberBounds(1, Int.MaxValue)
  }

  override def getSupportedTypes(key: String): Set[UsingDirectiveValueKind] = Set(
    UsingDirectiveValueKind.STRING,
    UsingDirectiveValueKind.NUMERIC
  )

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValuesContainer =>
      val values = DirectiveUtil.concatAllValues(groupedValuesContainer)
      val scalaNativeOptions = scopedDirective.directive.key match {
        case "native-gc" | "nativeGc" =>
          ScalaNativeOptions(
            gcStr = Some(values.head.positioned.value)
          )
        case "native-mode" | "nativeMode" =>
          ScalaNativeOptions(
            modeStr = Some(values.head.positioned.value)
          )
        case "native-version" | "nativeVersion" =>
          ScalaNativeOptions(
            version = Some(values.head.positioned.value)
          )
        case "native-compile" | "nativeCompile" =>
          ScalaNativeOptions(
            compileOptions = values.map(_.positioned.value).toList
          )
        case "native-linking" | "nativeLinking" =>
          ScalaNativeOptions(
            linkingOptions = values.map(_.positioned.value).toList
          )
        case "native-clang" | "nativeClang" =>
          ScalaNativeOptions(
            clang = Some(values.head.positioned.value)
          )
        case "native-clang-pp" | "nativeClangPP" =>
          ScalaNativeOptions(
            clangpp = Some(values.head.positioned.value)
          )
      }
      val options = BuildOptions(scalaNativeOptions = scalaNativeOptions)
      ProcessedDirective(Some(options), Nil)
    }

}
