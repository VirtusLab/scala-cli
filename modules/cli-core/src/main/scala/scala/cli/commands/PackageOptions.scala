package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.Build

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
  @HelpMessage("Specify which main class to run")
  @ValueDescription("main-class")
  @Name("M")
    mainClass: Option[String] = None,
  @Group("Package")
  @HelpMessage("Build debian package, available only on linux")
    debian: Boolean = false,
  @Group("Package")
  @HelpMessage("Build dmg package, available only on centOS")
    dmg: Boolean = false,
  @Group("Package")
  @HelpMessage("Build pkg package, available only on centOS")
    pkg: Boolean = false,
) {
  import PackageOptions.PackageType
  def packageType: PackageType = {
    if (library) PackageType.LibraryJar
    else if (shared.js) PackageType.Js
    else if (shared.native) PackageType.Native
    else if (debian)  PackageType.Debian
    else if (dmg) PackageType.Dmg
    else if (pkg) PackageType.Pkg
    else PackageType.Bootstrap
  }

  def buildOptions(scalaVersions: ScalaVersions): Build.Options =
    shared.buildOptions(scalaVersions, enableJmh = false, jmhVersion = None)

}

object PackageOptions {

  sealed abstract class PackageType extends Product with Serializable
  sealed abstract class NativePackagerType extends PackageType
  object PackageType {
    case object Bootstrap extends PackageType
    case object LibraryJar extends PackageType
    case object Js extends PackageType
    case object Native extends PackageType
    case object Debian extends NativePackagerType
    case object Dmg extends NativePackagerType
    case object Pkg extends NativePackagerType
  }

  implicit val parser = Parser[PackageOptions]
  implicit val help = Help[PackageOptions]
}
