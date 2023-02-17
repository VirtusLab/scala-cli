package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

// format: off
final case class SharedDependencyOptions(
  @Group("Dependency")
  @HelpMessage("Add dependencies")
  @Tag(tags.must)
  @Tag(tags.important)
  @Name("dep")
    dependency: List[String] = Nil,

  @Group("Dependency")
  @Tag(tags.should)
  @Tag(tags.important)
  @HelpMessage("Add repositories")
  @Name("repo")
  @Name("r")
    repository: List[String] = Nil,
  @Group("Scala")
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
