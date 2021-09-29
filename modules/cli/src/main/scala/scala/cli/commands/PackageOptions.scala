package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.options._

// format: off
@HelpMessage("Compile and package Scala code")
final case class PackageOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    compileCross: CompileCrossOptions = CompileCrossOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),

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
  @HelpMessage("Package standalone JARs")
    standalone: Option[Boolean] = None,
  @Recurse
    packager: PackagerOptions = PackagerOptions(),
  @Group("Package")
  @HelpMessage("Build debian package, available only on linux")
    deb: Boolean = false,
  @Group("Package")
  @HelpMessage("Build dmg package, available only on macOS")
    dmg: Boolean = false,
  @Group("Package")
  @HelpMessage("Build rpm package, available only on linux")
    rpm: Boolean = false,
  @Group("Package")
  @HelpMessage("Build msi package, available only on windows")
    msi: Boolean = false,
  @Group("Package")
  @HelpMessage("Build pkg package, available only on macOS")
    pkg: Boolean = false,
  @Group("Package")
  @HelpMessage("Build docker image")
    docker: Boolean = false,
) {
  // format: on
  def packageTypeOpt: Option[PackageType] =
    if (library) Some(PackageType.LibraryJar)
    else if (assembly) Some(PackageType.Assembly)
    else if (deb) Some(PackageType.Debian)
    else if (dmg) Some(PackageType.Dmg)
    else if (pkg) Some(PackageType.Pkg)
    else if (rpm) Some(PackageType.Rpm)
    else if (msi) Some(PackageType.Msi)
    else None

  def buildOptions: BuildOptions = {
    val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
    baseOptions.copy(
      mainClass = mainClass.mainClass.filter(_.nonEmpty),
      packageOptions = baseOptions.packageOptions.copy(
        standalone = standalone,
        version = Some(packager.version),
        launcherApp = packager.launcherApp,
        maintainer = packager.maintainer,
        description = packager.description,
        packageTypeOpt = packageTypeOpt,
        logoPath = packager.logoPath.map(os.Path(_, os.pwd)),
        macOSidentifier = packager.identifier,
        debianOptions = DebianOptions(
          conflicts = packager.debianConflicts,
          dependencies = packager.debianDependencies,
          architecture = Some(packager.debArchitecture)
        ),
        redHatOptions = RedHatOptions(
          license = packager.license,
          release = Some(packager.release),
          architecture = Some(packager.rpmArchitecture)
        ),
        windowsOptions = WindowsOptions(
          licensePath = packager.licensePath.map(os.Path(_, os.pwd)),
          productName = Some(packager.productName),
          exitDialog = packager.exitDialog,
          suppressValidation = packager.suppressValidation,
          extraConfig = packager.extraConfig,
          is64Bits = Some(packager.is64Bits),
          installerVersion = packager.installerVersion
        ),
        dockerOptions = DockerOptions(
          from = packager.dockerFrom,
          imageRegistry = packager.dockerImageRegistry,
          imageRepository = packager.dockerImageRepository,
          imageTag = packager.dockerImageTag,
          isDockerEnabled = Some(docker)
        )
      )
    )
  }
}

object PackageOptions {
  implicit val parser = Parser[PackageOptions]
  implicit val help   = Help[PackageOptions]
}
