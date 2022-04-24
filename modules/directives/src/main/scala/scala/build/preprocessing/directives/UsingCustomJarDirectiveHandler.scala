package scala.build.preprocessing.directives
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.build.Positioned
import scala.build.EitherCps.*
import scala.build.errors.BuildException

case object UsingCustomJarDirectiveHandler extends SimpleUsingDirectiveHandler(
      name = "Custom JAR",
      description = "Manually add JAR(s) to the class path",
      keys = Seq("jar", "jars"),
      constrains = AtLeastOne(ValueType.String)
    ) {
  def process(
    paths: ::[Positioned[String]])(
    using ctx: DirectiveContext
  ): Either[BuildException, BuildOptions] = either {
    val root = value(ctx.asRoot(paths.head))
    val pathSequence = value(
      paths.map { positioned =>
        val path = scala.util.Try(os.Path(positioned.value, root)).toEither
        path.left.flatMap { exception =>
          val msg = s"""The jar path argument in the using directives at could not be found!
                       |${exception.getLocalizedMessage}""".stripMargin
          positioned.error(msg)
        }
      }.sequenceToComposite
    )
    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraClassPath = pathSequence
      )
    )
  }
  def usagesCode = Seq(
    "//> using jar <path>",
    "//> using jars <path1>, <path2>"
  )

  def examples = Seq(
    "//> using jar \"/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar\""
  )
}
