package scala.cli.commands.package0

import caseapp.{Group, Help, HelpMessage, Name, Parser, ValueDescription}

import scala.cli.commands.Constants

// format: off
final case class PackagerOptions(
  @HelpMessage("Set the version of the generated package")
    version: String = "1.0.0",
  @HelpMessage(
    "Path to application logo in PNG format, it will be used to generate icon and banner/dialog in msi installer"
  )
    logoPath: Option[String] = None,
  @HelpMessage("Set launcher app name, which will be linked to the PATH")
    launcherApp: Option[String] = None,
  @ValueDescription("Description")
    description: Option[String] = None,
  @HelpMessage("This should contain names and email addresses of co-maintainers of the package")
  @Name("m")
    maintainer: Option[String] = None,
  @Group("Debian")
  @HelpMessage(
    "The list of Debian package that this package is not compatible with"
  )
  @ValueDescription("Debian dependencies conflicts")
    debianConflicts: List[String] = Nil,
  @Group("Debian")
  @HelpMessage("The list of Debian packages that this package depends on")
  @ValueDescription("Debian dependencies")
    debianDependencies: List[String] = Nil,
  @Group("Debian")
  @HelpMessage(
    "Architectures that are supported by the repository (default: all)"
  )
    debArchitecture: String = "all",
  @Group("Debian")
  @HelpMessage(
    "This field represents how important it is that the user have the package installed"
  )
  priority: Option[String] = None,
  @Group("Debian")
  @HelpMessage(
    "This field specifies an application area into which the package has been classified"
  )
  section: Option[String] = None,
  @Group("MacOS")
  @HelpMessage(
  "CF Bundle Identifier"
  )
    identifier: Option[String] = None,
  @Group("RedHat")
  @HelpMessage(
    "Licenses that are supported by the repository (list of licenses: https://fedoraproject.org/wiki/Licensing:Main?rd=Licensing)"
  )
    license: Option[String] = None,
  @Group("RedHat")
  @HelpMessage(
    "The number of times this version of the software was released (default: 1)"
  )
    release: String = "1",
  @HelpMessage("Architectures that are supported by the repository (default: noarch)")
    rpmArchitecture: String = "noarch",
  @Group("Windows")
  @HelpMessage("Path to the license file")
    licensePath: Option[String] = None,
  @Group("Windows")
  @HelpMessage("Name of product (default: Scala packager)")
    productName: String = "Scala packager",
  @Group("Windows")
  @HelpMessage("Text that will be displayed on the exit dialog")
    exitDialog: Option[String] = None,
  @Group("Windows")
  @HelpMessage("Suppress Wix ICE validation (required for users that are neither interactive, not local administrators)")
    suppressValidation: Option[Boolean] = None,
  @Group("Windows")
  @HelpMessage("Path to extra WIX configuration content")
  @ValueDescription("path")
    extraConfig: List[String] = Nil,
  @Group("Windows")
  @HelpMessage("Whether a 64-bit executable is being packaged")
  @Name("64")
    is64Bits: Boolean = true,
  @Group("Windows")
  @HelpMessage("WIX installer version")
    installerVersion: Option[String] = None,
  @Group("Windows")
  @HelpMessage("The GUID to identify that the windows package can be upgraded.")
    wixUpgradeCodeGuid: Option[String] = None,
  @Group("Docker")
  @HelpMessage(
    "Building the container from base image"
  )
  dockerFrom: Option[String] = None,
  @Group("Docker")
  @HelpMessage(
    "The image registry; if empty, it will use the default registry"
  )
  dockerImageRegistry: Option[String] = None,
  @Group("Docker")
  @HelpMessage(
    "The image repository"
  )
  dockerImageRepository: Option[String] = None,
  @Group("Docker")
  @HelpMessage(
    "The image tag; the default tag is `latest`"
  )
  dockerImageTag: Option[String] = None,

  @Group("Native image")
  @HelpMessage(s"GraalVM Java major version to use to build GraalVM native images (${Constants.defaultGraalVMJavaVersion} by default)")
  @ValueDescription("java-major-version")
    graalvmJavaVersion: Option[Int] = None,
  @Group("Native image")
  @HelpMessage(s"GraalVM version to use to build GraalVM native images (${Constants.defaultGraalVMVersion} by default)")
  @ValueDescription("version")
    graalvmVersion: Option[String] = None,
  @Group("Native image")
  @HelpMessage("JVM id of GraalVM distribution to build GraalVM native images (like \"graalvm-java17:22.0.0\")")
  @ValueDescription("jvm-id")
    graalvmJvmId: Option[String] = None,
  @Group("Native image")
  @HelpMessage("Pass args to GraalVM")
   graalvmArgs: List[String] = Nil
)
// format: on

object PackagerOptions {
  implicit lazy val parser: Parser[PackagerOptions] = Parser.derive
  implicit lazy val help: Help[PackagerOptions]     = Help.derive
}
