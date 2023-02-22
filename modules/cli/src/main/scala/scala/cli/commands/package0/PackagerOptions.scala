package scala.cli.commands.package0

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.{Constants, tags}

// format: off
final case class PackagerOptions(
  @HelpMessage("Set the version of the generated package")
  @Tag(tags.restricted)
    version: String = "1.0.0",
  @HelpMessage(
    "Path to application logo in PNG format, it will be used to generate icon and banner/dialog in msi installer"
  )
  @Tag(tags.restricted)
    logoPath: Option[String] = None,
  @HelpMessage("Set launcher app name, which will be linked to the PATH")
  @Tag(tags.restricted)
    launcherApp: Option[String] = None,
  @ValueDescription("Description")
    description: Option[String] = None,
  @HelpMessage("This should contain names and email addresses of co-maintainers of the package")
  @Name("m")
    maintainer: Option[String] = None,
  @Group(HelpGroup.Debian.toString)
  @HelpMessage(
    "The list of Debian package that this package is not compatible with"
  )
  @ValueDescription("Debian dependencies conflicts")
  @Tag(tags.restricted)
    debianConflicts: List[String] = Nil,
  @Group(HelpGroup.Debian.toString)
  @HelpMessage("The list of Debian packages that this package depends on")
  @ValueDescription("Debian dependencies")
  @Tag(tags.restricted)
    debianDependencies: List[String] = Nil,
  @Group(HelpGroup.Debian.toString)
  @HelpMessage(
    "Architectures that are supported by the repository (default: all)"
  )
  @Tag(tags.restricted)
    debArchitecture: String = "all",
  @Group(HelpGroup.Debian.toString)
  @HelpMessage(
    "This field represents how important it is that the user have the package installed"
  )
  @Tag(tags.restricted)
  priority: Option[String] = None,
  @Group(HelpGroup.Debian.toString)
  @HelpMessage(
    "This field specifies an application area into which the package has been classified"
  )
  @Tag(tags.restricted)
  section: Option[String] = None,
  @Group(HelpGroup.MacOS.toString)
  @HelpMessage(
  "CF Bundle Identifier"
  )
  @Tag(tags.restricted)
    identifier: Option[String] = None,
  @Group(HelpGroup.RedHat.toString)
  @HelpMessage(
    "Licenses that are supported by the repository (list of licenses: https://fedoraproject.org/wiki/Licensing:Main?rd=Licensing)"
  )
  @Tag(tags.restricted)
    license: Option[String] = None,
  @Group(HelpGroup.RedHat.toString)
  @HelpMessage(
    "The number of times this version of the software was released (default: 1)"
  )
  @Tag(tags.restricted)
    release: String = "1",
  @HelpMessage("Architectures that are supported by the repository (default: noarch)")
  @Tag(tags.restricted)
    rpmArchitecture: String = "noarch",
  @Group(HelpGroup.Windows.toString)
  @HelpMessage("Path to the license file")
  @Tag(tags.restricted)
    licensePath: Option[String] = None,
  @Group(HelpGroup.Windows.toString)
  @HelpMessage("Name of product (default: Scala packager)")
  @Tag(tags.restricted)
    productName: String = "Scala packager",
  @Group(HelpGroup.Windows.toString)
  @HelpMessage("Text that will be displayed on the exit dialog")
  @Tag(tags.restricted)
    exitDialog: Option[String] = None,
  @Group(HelpGroup.Windows.toString)
  @Tag(tags.restricted)
  @HelpMessage("Suppress Wix ICE validation (required for users that are neither interactive, not local administrators)")
    suppressValidation: Option[Boolean] = None,
  @Group(HelpGroup.Windows.toString)
  @Tag(tags.restricted)
  @HelpMessage("Path to extra WIX configuration content")
  @ValueDescription("path")
    extraConfig: List[String] = Nil,
  @Group(HelpGroup.Windows.toString)
  @Tag(tags.restricted)
  @HelpMessage("Whether a 64-bit executable is being packaged")
  @Name("64")
    is64Bits: Boolean = true,
  @Group(HelpGroup.Windows.toString)
  @HelpMessage("WIX installer version")
  @Tag(tags.restricted)
    installerVersion: Option[String] = None,
  @Group(HelpGroup.Windows.toString)
  @HelpMessage("The GUID to identify that the windows package can be upgraded.")
  @Tag(tags.restricted)
    wixUpgradeCodeGuid: Option[String] = None,
  @Group(HelpGroup.Docker.toString)
  @HelpMessage(
    "Building the container from base image"
  )
  @Tag(tags.restricted)
  dockerFrom: Option[String] = None,
  @Group(HelpGroup.Docker.toString)
  @HelpMessage(
    "The image registry; if empty, it will use the default registry"
  )
  @Tag(tags.restricted)
  dockerImageRegistry: Option[String] = None,
  @Group(HelpGroup.Docker.toString)
  @HelpMessage(
    "The image repository"
  )
  @Tag(tags.restricted)
  dockerImageRepository: Option[String] = None,
  @Group(HelpGroup.Docker.toString)
  @HelpMessage(
    "The image tag; the default tag is `latest`"
  )
  @Tag(tags.restricted)
  dockerImageTag: Option[String] = None,

  @Group(HelpGroup.NativeImage.toString)
  @HelpMessage(s"GraalVM Java major version to use to build GraalVM native images (${Constants.defaultGraalVMJavaVersion} by default)")
  @ValueDescription("java-major-version")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    graalvmJavaVersion: Option[Int] = None,
  @Group(HelpGroup.NativeImage.toString)
  @HelpMessage(s"GraalVM version to use to build GraalVM native images (${Constants.defaultGraalVMVersion} by default)")
  @ValueDescription("version")
  @Tag(tags.inShortHelp)
    graalvmVersion: Option[String] = None,
  @Group(HelpGroup.NativeImage.toString)
  @HelpMessage("JVM id of GraalVM distribution to build GraalVM native images (like \"graalvm-java17:22.0.0\")")
  @ValueDescription("jvm-id")
  @Tag(tags.restricted)
    graalvmJvmId: Option[String] = None,
  @Group(HelpGroup.NativeImage.toString)
  @HelpMessage("Pass args to GraalVM")
  @Tag(tags.restricted)
   graalvmArgs: List[String] = Nil
)
// format: on

object PackagerOptions {
  implicit lazy val parser: Parser[PackagerOptions] = Parser.derive
  implicit lazy val help: Help[PackagerOptions]     = Help.derive
}
