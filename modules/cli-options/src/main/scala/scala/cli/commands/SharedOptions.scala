package scala.cli.commands

import caseapp.*
import caseapp.core.help.Help
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.Constants
import scala.cli.commands.common.HasLoggingOptions

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
  @HelpMessage(s"Set the Scala version (${Constants.defaultScalaVersion} by default)")
  @ValueDescription("version")
  @Name("scala")
  @Name("S")
  @Tag(tags.must)
    scalaVersion: Option[String] = None,
  @Group("Scala")
  @HelpMessage("Set the Scala binary version")
  @ValueDescription("version")
  @Hidden
  @Name("scalaBinary")
  @Name("scalaBin")
  @Name("B")
  @Tag(tags.must)
    scalaBinaryVersion: Option[String] = None,

  @Recurse
    scalacExtra: ScalacExtraOptions = ScalacExtraOptions(),

  @Recurse
    snippet: SnippetOptions = SnippetOptions(),

  @Recurse
    markdown: MarkdownOptions = MarkdownOptions(),

  @Group("Java")
  @HelpMessage("Add extra JARs and compiled classes to the class path")
  @ValueDescription("paths")
  @Name("jar")
  @Name("jars")
  @Name("extraJar")
  @Name("class")
  @Name("extraClass")
  @Name("classes")
  @Name("extraClasses")
  @Name("-classpath")
  @Name("-cp")
  @Name("classpath")
  @Name("classPath")
  @Name("extraClassPath")
  @Tag(tags.must)
    extraJars: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.")
  @ValueDescription("paths")
  @Name("compileOnlyJar")
  @Name("compileOnlyJars")
  @Name("extraCompileOnlyJar")
  @Tag(tags.should)
    extraCompileOnlyJars: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Add extra source JARs")
  @ValueDescription("paths")
  @Name("sourceJar")
  @Name("sourceJars")
  @Name("extraSourceJar")
  @Tag(tags.should)
    extraSourceJars: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Add a resource directory")
  @ValueDescription("paths")
  @Name("resourceDir")
  @Tag(tags.must)
    resourceDirs: List[String] = Nil,

  @HelpMessage("Specify platform")
  @ValueDescription("scala-js|scala-native|jvm")
  @Tag(tags.should)
    platform: Option[String] = None,

  @Group("Scala")
  @Tag(tags.implementation)
  @Hidden
    scalaLibrary: Option[Boolean] = None,
  @Group("Java")
  @HelpMessage("Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.")
  @Tag(tags.implementation)
  @Hidden
    java: Option[Boolean] = None,
  @HelpMessage("Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.")
  @Tag(tags.implementation)
  @Hidden
    runner: Option[Boolean] = None,

  @Hidden
  @Tag(tags.should)
  @HelpMessage("Generate SemanticDBs")
    semanticDb: Option[Boolean] = None,
  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Add dependency for stubs needed to make $ivy and $dep imports to work.")
    addStubs: Option[Boolean] = None,

  @Recurse
    input: SharedInputOptions = SharedInputOptions(),
  @Recurse
  helpGroups: HelpGroupOptions = HelpGroupOptions(),

  @Hidden
    strictBloopJsonCheck: Option[Boolean] = None,

  @Name("output-directory")
  @Name("d")
  @Name("destination")
  @Name("compileOutput")
  @Name("compileOut")
  @HelpMessage("Copy compilation results to output directory using either relative or absolute path")
  @ValueDescription("/example/path")
  @Tag(tags.must)
    compilationOutput: Option[String] = None,
) extends HasLoggingOptions
  // format: on

object SharedOptions {
  implicit lazy val parser: Parser[SharedOptions]            = Parser.derive
  implicit lazy val help: Help[SharedOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedOptions] = JsonCodecMaker.make
}
