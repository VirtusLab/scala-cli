package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, MaybeScalaVersion, ScalaOptions}

case object UsingScalaVersionDirectiveHandler extends UsingDirectiveHandler {
  def name = "Scala version"

  def description = "Set the default Scala version"

  def usage = "//> using scala _version_+"

  override def usageMd = "`//> using scala `_version_+"

  override def examples = Seq(
    "//> using scala \"3.0.2\"",
    "//> using scala \"2.13\"",
    "//> using scala \"2\"",
    "//> using scala \"2.13.6\", \"2.12.16\""
  )

  def keys = Seq("scala")

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValues =>
      val scalaVersions = groupedValues.scopedStringValues ++ groupedValues.scopedNumericValues

      val options = BuildOptions(
        scalaOptions = ScalaOptions(
          scalaVersion = scalaVersions.headOption
            .map(_.positioned.value)
            .map(MaybeScalaVersion(_)),
          extraScalaVersions = scalaVersions.drop(1).map(_.positioned.value).toSet
        )
      )
      ProcessedDirective(Some(options), Seq.empty)

    }

  override def getSupportedTypes(key: String) =
    Set(UsingDirectiveValueKind.STRING, UsingDirectiveValueKind.NUMERIC)

}
