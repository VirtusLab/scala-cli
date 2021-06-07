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
    mainClass: Option[String] = None
) {
  import PackageOptions.PackageType
  def packageType: PackageType =
    if (library) PackageType.LibraryJar
    else if (shared.js) PackageType.Js
    else if (shared.native) PackageType.Native
    else PackageType.Bootstrap

  def buildOptions: Build.Options =
    shared.buildOptions(enableJmh = false, jmhVersion = None)
}

object PackageOptions {

  sealed abstract class PackageType extends Product with Serializable
  object PackageType {
    case object Bootstrap extends PackageType
    case object LibraryJar extends PackageType
    case object Js extends PackageType
    case object Native extends PackageType
  }

  implicit val parser = Parser[PackageOptions]
  implicit val help = Help[PackageOptions]
}
