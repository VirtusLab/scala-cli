package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.build.preprocessing.directives.Repository
import scala.cli.commands.tags

// format: off
final case class SharedDependencyOptions(
  @Group(HelpGroup.Dependency.toString)
  @HelpMessage("Add dependencies")
  @Tag(tags.must)
  @Name("dep")
    dependency: List[String] = Nil,

  @Group(HelpGroup.Dependency.toString)
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
  @HelpMessage(Repository.usageMsg)
  @Name("r")
  @Name("repo")
    repository: List[String] = Nil,
  @Group(HelpGroup.Scala.toString)
  @Name("P")
  @Name("plugin")
  @Tag(tags.must)
  @HelpMessage("Add compiler plugin dependencies")
  compilerPlugin: List[String] = Nil
)
// format: on

object SharedDependencyOptions {
  implicit lazy val parser: Parser[SharedDependencyOptions]            = Parser.derive
  implicit lazy val help: Help[SharedDependencyOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedDependencyOptions] = JsonCodecMaker.make
}
