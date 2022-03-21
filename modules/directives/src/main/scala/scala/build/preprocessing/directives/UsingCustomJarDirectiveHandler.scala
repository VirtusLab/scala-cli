package scala.build.preprocessing.directives
import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.{BuildOptions, ClassPathOptions}
import scala.build.preprocessing.ScopePath

case object UsingCustomJarDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Custom JAR"
  def description = "Manually add JAR(s) to the class path"
  def usage       = "`//> using jar `_path_ | `//> using jars `_path1_, _path2_ …"
  override def usageMd =
    """//> using jar _path_
      |
      |//> using jars _path1_, _path2_ …""".stripMargin

  override def examples = Seq(
    "//> using jar \"/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar\""
  )

  def keys = Seq("jar", "jars")
  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = either {
    val values = directive.values
    val extraJars: Seq[Either[BuildException, os.Path]] =
      DirectiveUtil.stringValues(values, path, cwd).map {
        case (p, _) =>
          val root = Directive.osRoot(cwd, p.positions.headOption)
          // FIXME Handle malformed paths here
          root.map(os.Path(p.value, _))
      }

    val res = extraJars
      .sequence
      .left.map(CompositeBuildException(_))

    ProcessedDirective(
      Some(BuildOptions(
        classPathOptions = ClassPathOptions(
          extraClassPath = value(res)
        )
      )),
      Seq.empty
    )
  }
}
