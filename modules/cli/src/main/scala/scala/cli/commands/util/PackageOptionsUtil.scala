package scala.cli.commands
package util

import scala.build.BuildThreads
import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.compiler.{ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.errors.{BuildException, CompositeBuildException, ModuleFormatError}
import scala.build.options._
import scala.build.options.packaging._
import scala.cli.commands.PackageOptions
import scala.cli.commands.util.SharedOptionsUtil._

object PackageOptionsUtil {
  implicit class PackageOptionsOps(private val v: PackageOptions) {
    import v._

    def packageTypeOpt: Option[PackageType] =
      forcedPackageTypeOpt.orElse {
        if (v.library) Some(PackageType.LibraryJar)
        else if (source) Some(PackageType.SourceJar)
        else if (assembly) Some(PackageType.Assembly(addPreamble = preamble))
        else if (spark) Some(PackageType.Spark)
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

    def providedModules: Either[BuildException, Seq[dependency.AnyModule]] =
      provided
        .map { str =>
          dependency.parser.ModuleParser.parse(str)
            .left.map(err => new ModuleFormatError(str, err))
        }
        .sequence
        .left.map(CompositeBuildException(_))

    def baseBuildOptions: BuildOptions = {
      val baseOptions = shared.buildOptions()
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
        ),
        internal = baseOptions.internal.copy(
          // computing the provided modules sub-graph need the final Resolution instance
          // Spark packaging adds provided modules, so it needs it too
          keepResolution = provided.nonEmpty || packageTypeOpt.contains(PackageType.Spark)
        ),
        internalDependencies = baseOptions.internalDependencies.copy(
          addRunnerDependencyOpt = Some(false)
        )
      )
    }

    def finalBuildOptions: Either[BuildException, BuildOptions] = either {
      val baseOptions = baseBuildOptions
      baseOptions.copy(
        notForBloopOptions = baseOptions.notForBloopOptions.copy(
          packageOptions = baseOptions.notForBloopOptions.packageOptions.copy(
            provided = value(providedModules)
          )
        )
      )
    }

    def compilerMaker(threads: BuildThreads): ScalaCompilerMaker = {
      val maker = shared.compilerMaker(threads)
      if (forcedPackageTypeOpt.contains(PackageType.DocJar))
        ScalaCompilerMaker.IgnoreScala2(maker)
      else
        maker
    }
    def docCompilerMakerOpt: Option[ScalaCompilerMaker] =
      if (forcedPackageTypeOpt.contains(PackageType.DocJar))
        Some(SimpleScalaCompilerMaker("java", Nil, scaladoc = true))
      else
        None
  }
}
