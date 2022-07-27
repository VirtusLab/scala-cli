package scala.cli.commands

import caseapp._
import caseapp.core.help.Help
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

// format: off
final case class SharedOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    js: ScalaJsOptions = ScalaJsOptions(),
  @Recurse
    native: ScalaNativeOptions = ScalaNativeOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),
  @Recurse
    dependencies: SharedDependencyOptions = SharedDependencyOptions(),
  @Recurse
    scalac: ScalacOptions = ScalacOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Recurse
    workspace: SharedWorkspaceOptions = SharedWorkspaceOptions(),

  @Group("Scala")
  @HelpMessage("Set the Scala version")
  @ValueDescription("version")
  @Name("scala")
  @Name("S")
    scalaVersion: Option[String] = None,
  @Group("Scala")
  @HelpMessage("Set the Scala binary version")
  @ValueDescription("version")
  @Hidden
  @Name("scalaBinary")
  @Name("scalaBin")
  @Name("B")
    scalaBinaryVersion: Option[String] = None,
  
  @Group("Scala")
  @HelpMessage("Show help for scalac. This is an alias for --scalac-option -help")
    scalacHelp: Boolean = false,

  @Recurse
    snippet: SnippetOptions = SnippetOptions(),

  @Group("Java")
  @HelpMessage("Add extra JARs in the class path")
  @ValueDescription("paths")
  @Name("jar")
  @Name("jars")
  @Name("extraJar")
    extraJars: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Add extra JARs in the class path, during compilation only")
  @ValueDescription("paths")
  @Name("compileOnlyJar")
  @Name("compileOnlyJars")
  @Name("extraCompileOnlyJar")
    extraCompileOnlyJars: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Add extra source JARs")
  @ValueDescription("paths")
  @Name("sourceJar")
  @Name("sourceJars")
  @Name("extraSourceJar")
    extraSourceJars: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Add a resource directory")
  @ValueDescription("paths")
  @Name("resourceDir")
    resourceDirs: List[String] = Nil,

  @Group("Scala")
  @Hidden
    scalaLibrary: Option[Boolean] = None,
  @Group("Java")
  @Hidden
    java: Option[Boolean] = None,
  @Hidden
    runner: Option[Boolean] = None,

  @Hidden
  @HelpMessage("Generate SemanticDBs")
    semanticDb: Option[Boolean] = None,
  @Hidden
    addStubs: Option[Boolean] = None,

  @Recurse
    input: SharedInputOptions = SharedInputOptions(),
  @Recurse
  helpGroups: HelpGroupOptions = HelpGroupOptions(),

  @Hidden
    strictBloopJsonCheck: Option[Boolean] = None,
)
  // format: on

object SharedOptions {
  implicit lazy val parser: Parser[SharedOptions]            = Parser.derive
  implicit lazy val help: Help[SharedOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedOptions] = JsonCodecMaker.make
}
