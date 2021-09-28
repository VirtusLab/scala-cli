package scala.build.preprocessing.directives

import scala.build.Os
import scala.build.options.{BuildOptions, ClassPathOptions}

case object UsingCustomJarDirectiveHandler extends UsingDirectiveHandler {
  def name        = "Custom JAR"
  def description = "Manually adds JAR to the class path"
  def usage       = "using jar _path_ | using jars _path1_ _path2_ …"
  override def usageMd =
    "`using jar `_path_ | `using jars `_path1_ _path2_ …"
  override def examples = Seq(
    "using jar \"/Users/alexandre/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/chuusai/shapeless_2.13/2.3.7/shapeless_2.13-2.3.7.jar\""
  )

  def handle(directive: Directive): Option[Either[String, BuildOptions]] =
    directive.values match {
      case Seq("jar" | "jars", paths @ _*) =>
        val paths0 = paths.map(os.Path(_, Os.pwd))
        val options = BuildOptions(
          classPathOptions = ClassPathOptions(
            extraClassPath = paths0
          )
        )
        Some(Right(options))
      case _ =>
        None
    }
}
