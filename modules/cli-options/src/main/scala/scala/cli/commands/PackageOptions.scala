package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

// format: off
@HelpMessage("Compile and package Scala code")
final case class PackageOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    java: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    compileCross: CompileCrossOptions = CompileCrossOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),

  @Group("Package")
  @HelpMessage("Set the destination path")
  @Name("o")
    output: Option[String] = None,
  @Group("Package")
  @HelpMessage("Overwrite the destination file, if it exists")
  @Name("f")
    force: Boolean = false,

  @Group("Package")
  @HelpMessage("Generate a library JAR rather than an executable JAR")
    library: Boolean = false,
  @Group("Package")
  @HelpMessage("Generate a source JAR rather than an executable JAR")
    source: Boolean = false,
  @Group("Package")
  @HelpMessage("Generate a scaladoc JAR rather than an executable JAR")
  @ExtraName("scaladoc")
  @ExtraName("javadoc")
    doc: Boolean = false,
  @Group("Package")
  @HelpMessage("Generate an assembly JAR")
    assembly: Boolean = false,
  @Group("Package")
  @HelpMessage("For assembly JAR, whether to add a bash / bat preamble")
    preamble: Boolean = true,
  @Group("Package")
  @Hidden
  @HelpMessage("For assembly JAR, whether to specify a main class in the JAR manifest")
    mainClassInManifest: Option[Boolean] = None,
  @Group("Package")
  @Hidden
  @HelpMessage("Generate an assembly JAR for Spark (assembly that doesn't contain Spark, nor any of its dependencies)")
    spark: Boolean = false,
  @Group("Package")
  @HelpMessage("Package standalone JARs")
    standalone: Option[Boolean] = None,
  @Recurse
    packager: PackagerOptions = PackagerOptions(),
  @Group("Package")
  @HelpMessage("Build Debian package, available only on Linux")
    deb: Boolean = false,
  @Group("Package")
  @HelpMessage("Build dmg package, available only on macOS")
    dmg: Boolean = false,
  @Group("Package")
  @HelpMessage("Build rpm package, available only on Linux")
    rpm: Boolean = false,
  @Group("Package")
  @HelpMessage("Build msi package, available only on Windows")
    msi: Boolean = false,
  @Group("Package")
  @HelpMessage("Build pkg package, available only on macOS")
    pkg: Boolean = false,
  @Group("Package")
  @HelpMessage("Build Docker image")
    docker: Boolean = false,

  @Group("Package")
  @Hidden
  @HelpMessage("Exclude modules *and their transitive dependencies* from the JAR to be packaged")
  @ValueDescription("org:name")
    provided: List[String] = Nil,

  @Group("Package")
  @HelpMessage("Use default scaladoc options")
  @ExtraName("defaultScaladocOpts")
    defaultScaladocOptions: Option[Boolean] = None,

  @Group("Package")
  @HelpMessage("Build GraalVM native image")
  @ExtraName("graal")
    nativeImage: Boolean = false
)
// format: on

object PackageOptions {
  implicit lazy val parser: Parser[PackageOptions] = Parser.derive
  implicit lazy val help: Help[PackageOptions]     = Help.derive
}
