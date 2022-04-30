package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.Positioned
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.cli.commands.RunOptions

case object UsingMainClassDirectiveHandler
    extends BuildOptionsHandler(Single(DirectiveValue.String)) {
  def name        = "Main class"
  def description = fromCommand("main-class", RunOptions.help)
  def keys        = Seq("main-class", "mainClass")

  def usagesCode: Seq[String] = Seq(
    "//> using mainClass <fqn>"
  )

  override def examples = Seq(
    "//> using mainClass \"foo.bar.Baz\"",
    "//> using main-class \"app.Main\""
  )

  def process(fqn: Positioned[String])(using Ctx) =
    Right(BuildOptions(mainClass = Some(fqn.value)))
}
