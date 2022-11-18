package scala.cli.commands

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

// format: off
final case class SharedDependencyOptions(
  @Group("Dependency")
  @HelpMessage("Add dependencies")
  @Tag(tags.must)
  @Name("dep")
    dependency: List[String] = Nil,

  @Group("Dependency")
  @Tag(tags.should)
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
