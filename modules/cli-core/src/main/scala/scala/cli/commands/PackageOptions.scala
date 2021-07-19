package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.options.{BuildOptions, PackageType}

@HelpMessage("Compile and package Scala code")
final case class PackageOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @Group("Package")
  @HelpMessage("Set destination path")
  @Name("o")
    output: Option[String] = None,
  @Group("Package")
  @HelpMessage("Overwrite destination file if it exists")
  @Name("f")
    force: Boolean = false,

  @Group("Package")
  @HelpMessage("Generate a library JAR rather than an executable JAR")
    library: Boolean = false,
  @Group("Package")
  @HelpMessage("Generate an assembly JAR")
    assembly: Boolean = false,

  @Group("Package")
  @HelpMessage("Specify which main class to run")
  @ValueDescription("main-class")
  @Name("M")
    mainClass: Option[String] = None,
  @Group("Package")
  @HelpMessage("Build debian package, available only on linux")
    deb: Boolean = false,
  @Group("Package")
  @HelpMessage("Build dmg package, available only on centOS")
    dmg: Boolean = false,
  @Group("Package")
  @HelpMessage("Build rpm package, available only on linux")
    rpm: Boolean = false,
  @Group("Package")
  @HelpMessage("Build msi package, available only on windows")
    msi: Boolean = false,
  @Group("Package")
  @HelpMessage("Build pkg package, available only on centOS")
    pkg: Boolean = false,
) {
  def packageTypeOpt: Option[PackageType] =
    if (library) Some(PackageType.LibraryJar)
    else if (assembly) Some(PackageType.Assembly)
    else if (deb) Some(PackageType.Debian)
    else if (dmg) Some(PackageType.Dmg)
    else if (pkg) Some(PackageType.Pkg)
    else if (rpm) Some(PackageType.Rpm)
    else if (msi) Some(PackageType.Msi)
    else None

  def buildOptions: BuildOptions =
    shared.buildOptions(enableJmh = false, jmhVersion = None)
}

object PackageOptions {
  implicit val parser = Parser[PackageOptions]
  implicit val help = Help[PackageOptions]
}
