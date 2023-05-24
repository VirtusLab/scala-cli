package scala.build.errors

sealed abstract class UsingDirectiveExpectationError(
  message: String
) extends BuildException(message)

final class UsingDirectiveWrongValueTypeError(
  maybePath: Either[String, os.Path],
  key: String,
  expectedTypes: Seq[String],
  hint: String = ""
) extends UsingDirectiveExpectationError(
      s"""${expectedTypes.mkString(
          ", or "
        )} expected for the $key using directive key${maybePath.map(path => s" at $path").getOrElse(
          ""
        )}.
         |$hint""".stripMargin
    )

final class UsingDirectiveValueNumError(
  maybePath: Either[String, os.Path],
  key: String,
  expectedValueNum: Int,
  providedValueNum: Int
) extends UsingDirectiveExpectationError({
      val pathString = maybePath.map(p => s" at $p").getOrElse("")
      s"""Encountered an error when parsing the `$key` using directive$pathString.
         |Expected $expectedValueNum values, but got $providedValueNum values instead.""".stripMargin
    })

final class ToolkitDirectiveMissingVersionError(
  val maybePath: Either[String, os.Path],
  val key: String
) extends UsingDirectiveExpectationError({
      val pathString = maybePath.map(p => s" at $p").getOrElse("")
      s"""Encountered an error when parsing the `$key` using directive$pathString.
         |Expected a version to be passed.
         |Example: `//> using $key latest`
         |""".stripMargin
    })
object ToolkitDirectiveMissingVersionError {
  def apply(maybePath: Either[String, os.Path], key: String): ToolkitDirectiveMissingVersionError =
    new ToolkitDirectiveMissingVersionError(maybePath, key)
  def unapply(arg: ToolkitDirectiveMissingVersionError): (Either[String, os.Path], String) =
    arg.maybePath -> arg.key
}
