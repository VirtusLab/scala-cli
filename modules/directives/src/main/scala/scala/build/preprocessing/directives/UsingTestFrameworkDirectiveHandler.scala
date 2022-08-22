package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, TestOptions}

case object UsingTestFrameworkDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Test framework"
  def description = "Set the test framework"
  def usage       = "using testFramework _class_name_ | using `test-framework` _class_name_"
  override def usageMd =
    "`//> using testFramework `_class_name_ | ``//> using `test-framework` ``_class_name_"
  override def examples = Seq(
    "//> using testFramework \"utest.runner.Framework\""
  )

  override def getValueNumberBounds(key: String): UsingDirectiveValueNumberBounds =
    UsingDirectiveValueNumberBounds(1, 1)

  def keys                  = Seq("test-framework", "testFramework")
  override def isRestricted = true
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValues =>
      val framework = groupedValues.scopedStringValues.head
      val options = BuildOptions(
        testOptions = TestOptions(
          frameworkOpt = Some(framework.positioned.value)
        )
      )
      ProcessedDirective(Some(options), Seq.empty)

    }

}
