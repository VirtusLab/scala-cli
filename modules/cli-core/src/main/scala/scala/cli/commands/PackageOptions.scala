package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.options.BuildOptions

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
  import PackageOptions.PackageType
  def packageType: PackageType =
    if (library) PackageType.LibraryJar
    else if (assembly) PackageType.Assembly
    else if (shared.js.js) PackageType.Js
    else if (shared.native.native) PackageType.Native
    else if (deb)  PackageType.Debian
    else if (dmg) PackageType.Dmg
    else if (pkg) PackageType.Pkg
    else if (rpm) PackageType.Rpm
    else if (msi) PackageType.Msi
    else PackageType.Bootstrap

  def buildOptions: BuildOptions =
    shared.buildOptions(enableJmh = false, jmhVersion = None)
}

object PackageOptions {

  sealed abstract class PackageType extends Product with Serializable
  sealed abstract class NativePackagerType extends PackageType
  object PackageType {
    case object Bootstrap extends PackageType
    case object LibraryJar extends PackageType
    case object Assembly extends PackageType
    case object Js extends PackageType
    case object Native extends PackageType
    case object Debian extends NativePackagerType
    case object Dmg extends NativePackagerType
    case object Pkg extends NativePackagerType
    case object Rpm extends NativePackagerType
    case object Msi extends NativePackagerType
  }

  implicit val parser = Parser[PackageOptions]
  implicit val help = Help[PackageOptions]
}
