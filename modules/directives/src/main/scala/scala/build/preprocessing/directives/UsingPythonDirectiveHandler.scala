package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, MaybeScalaVersion, PostBuildOptions, ScalaOptions}

case object UsingPythonDirectiveHandler extends UsingDirectiveHandler {
  def name = "Python"

  def description = "Enable Python support"

  def usage = "//> using python"

  override def usageMd = "`//> using python"

  override def examples = Seq(
    "//> using python"
  )

  def keys = Seq("python")

  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val options = BuildOptions(
      notForBloopOptions = PostBuildOptions(
        python = Some(true)
      )
    )
    Right(ProcessedDirective(Some(options), Nil))
  }

  override def getSupportedTypes(key: String) =
    Set()
  override protected def getValueNumberBounds(key: String): UsingDirectiveValueNumberBounds =
    UsingDirectiveValueNumberBounds(0, 0)

}
