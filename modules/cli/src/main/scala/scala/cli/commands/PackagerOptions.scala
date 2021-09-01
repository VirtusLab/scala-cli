package scala.cli.commands

import caseapp.{Group, Help, HelpMessage, Name, Parser, ValueDescription}

// format: off
final case class PackagerOptions(
  @HelpMessage("The version of generated package")
    version: String = "1.0.0",
  @HelpMessage(
    "Path to application logo in png format, it will be used to generate icon and banner/dialog in msi installer"
  )
    logoPath: Option[String] = None,
  @HelpMessage("Set launcher app name which will be linked to PATH")
    launcherAppName: Option[String] = None,
  @ValueDescription("Description")
    description: Option[String] = None,
  @HelpMessage("It should contains names and email addresses of co-maintainers of the package")
  @Name("m")
    maintainer: Option[String] = None,
  @Group("Debian")
  @HelpMessage(
    "The list of debian package that this package is absolute incompatibility"
  )
  @ValueDescription("debian dependencies conflicts")
    debianConflicts: List[String] = Nil,
  @Group("Debian")
  @HelpMessage("The list of debian package that this package depends on")
  @ValueDescription("debian dependencies")
    debianDependencies: List[String] = Nil,
  @Group("Debian")
  @HelpMessage(
    "Architecture that are supported by the repository, default: all"
  )
    debArchitecture: String = "all",
  @Group("MacOS")
  @HelpMessage(
  "CF Bundle Identifier"
  )
    identifier: Option[String] = None,
  @Group("RedHat")
  @HelpMessage(
    "License that are supported by the repository - list of licenses https://fedoraproject.org/wiki/Licensing:Main?rd=Licensing"
  )
    license: Option[String] = None,
  @Group("RedHat")
  @HelpMessage(
    "The number of times this version of the software was released, default: 1"
  )
    release: String = "1",
  @HelpMessage("Architecture that are supported by the repository, default: ")
    rpmArchitecture: String = "noarch",
  @Group("Windows")
  @HelpMessage("Path to license file")
    licensePath: Option[String] = None,
  @Group("Windows")
  @HelpMessage("Name of product, default: Scala packager")
    productName: String = "Scala packager",
  @Group("Windows")
  @HelpMessage("Text will be displayed on exit dialog")
    exitDialog: Option[String] = None,
  @Group("Windows")
  @HelpMessage("Suppress Wix ICE validation (required for users that are neither interactive, not local administrators)")
    suppressValidation: Option[Boolean] = None
)
// format: on

object PackagerOptions {
  implicit val parser = Parser[PackagerOptions]
  implicit val help   = Help[PackagerOptions]
}
