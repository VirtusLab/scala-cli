package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, JavaOptions, MaybeScalaVersion, ScalaOptions}
import scala.build.preprocessing.directives.UsingMainClassDirectiveHandler.checkIfValuesAreExpected

case object UsingJvmDirectiveHandler extends UsingDirectiveHandler {
  def name = "JVM version"

  def description = "Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`"

  def usage = "//> using jvm _value_"

  override def usageMd = "`//> using jvm` _value_"

  override def examples = Seq(
    "//> using jvm \"11\"",
    "//> using jvm \"adopt:11\"",
    "//> using jvm \"graalvm:21\""
  )

  def keys = Seq("jvm")

  override def isRestricted = false

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).map { groupedValues =>
      val jvmIdOpts = groupedValues.scopedStringValues.map(_.positioned)
      val options = BuildOptions(
        javaOptions = JavaOptions(
          jvmIdOpt = jvmIdOpts.headOption
        )
      )
      ProcessedDirective(Some(options), Seq.empty)
    }

  override def getValueNumberBounds(key: String) = UsingDirectiveValueNumberBounds(1, 1)

}
