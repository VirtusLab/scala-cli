package scala.cli.commands
package util

import scala.build.BuildThreads
import scala.build.compiler.{ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.options._
import scala.build.options.packaging._
import scala.cli.commands.PackageOptions

trait PackageOptionsUtil {
  implicit class PackageOptionsOps(v: PackageOptions) {
    import v._

    def packageTypeOpt: Option[PackageType] =
      forcedPackageTypeOpt.orElse {
        if (v.library) Some(PackageType.LibraryJar)
        else if (source) Some(PackageType.SourceJar)
        else if (assembly) Some(PackageType.Assembly)
        else if (deb) Some(PackageType.Debian)
        else if (dmg) Some(PackageType.Dmg)
        else if (pkg) Some(PackageType.Pkg)
        else if (rpm) Some(PackageType.Rpm)
        else if (msi) Some(PackageType.Msi)
        else if (nativeImage) Some(PackageType.GraalVMNativeImage)
        else None
      }
    def forcedPackageTypeOpt: Option[PackageType] =
      if (doc) Some(PackageType.DocJar)
      else None

    def buildOptions: BuildOptions = {
      val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
      baseOptions.copy(
        mainClass = mainClass.mainClass.filter(_.nonEmpty),
        notForBloopOptions = baseOptions.notForBloopOptions.copy(
          packageOptions = baseOptions.notForBloopOptions.packageOptions.copy(
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
            ),
            nativeImageOptions = NativeImageOptions(
              graalvmJvmId = packager.graalvmJvmId.map(_.trim).filter(_.nonEmpty),
              graalvmJavaVersion = packager.graalvmJavaVersion.filter(_ > 0),
              graalvmVersion = packager.graalvmVersion.map(_.trim).filter(_.nonEmpty)
            ),
            useDefaultScaladocOptions = defaultScaladocOptions
          )
        )
      )
    }

    def compilerMaker(threads: BuildThreads): ScalaCompilerMaker =
      if (forcedPackageTypeOpt.contains(PackageType.DocJar))
        SimpleScalaCompilerMaker("java", Nil, scaladoc = true)
      else
        shared.compilerMaker(threads)
  }
}
