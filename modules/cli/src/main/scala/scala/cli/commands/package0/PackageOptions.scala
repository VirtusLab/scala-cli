package scala.cli.commands.package0

import caseapp.*
import caseapp.core.help.Help

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.compiler.{ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.errors.{BuildException, CompositeBuildException, ModuleFormatError}
import scala.build.options.*
import scala.build.options.packaging.*
import scala.build.{BuildThreads, Positioned}
import scala.cli.commands.package0.PackageOptions
import scala.cli.commands.shared.*
import scala.cli.commands.tags

@HelpMessage(PackageOptions.helpMessage, "", PackageOptions.detailedHelpMessage)
// format: off
final case class PackageOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    java: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    compileCross: CrossOptions = CrossOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),

  @Group(HelpGroup.Package.toString)
  @HelpMessage("Set the destination path")
  @Name("o")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    output: Option[String] = None,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Overwrite the destination file, if it exists")
  @Name("f")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    force: Boolean = false,

  @Group(HelpGroup.Package.toString)
  @HelpMessage("Generate a library JAR rather than an executable JAR")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    library: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Generate a source JAR rather than an executable JAR")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    source: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Generate a scaladoc JAR rather than an executable JAR")
  @ExtraName("scaladoc")
  @ExtraName("javadoc")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    doc: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Generate an assembly JAR")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    assembly: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("For assembly JAR, whether to add a bash / bat preamble")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    preamble: Boolean = true,
  @Group(HelpGroup.Package.toString)
  @Hidden
  @HelpMessage("For assembly JAR, whether to specify a main class in the JAR manifest")
  @Tag(tags.restricted)
    mainClassInManifest: Option[Boolean] = None,
  @Group(HelpGroup.Package.toString)
  @Hidden
  @HelpMessage("Generate an assembly JAR for Spark (assembly that doesn't contain Spark, nor any of its dependencies)")
  @Tag(tags.experimental)
    spark: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Package standalone JARs")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    standalone: Option[Boolean] = None,
  @Recurse
    packager: PackagerOptions = PackagerOptions(),
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Build Debian package, available only on Linux")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    deb: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Build dmg package, available only on macOS")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    dmg: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Build rpm package, available only on Linux")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    rpm: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Build msi package, available only on Windows")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    msi: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Build pkg package, available only on macOS")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    pkg: Boolean = false,
  @Group(HelpGroup.Package.toString)
  @HelpMessage("Build Docker image")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    docker: Boolean = false,

  @Group(HelpGroup.Package.toString)
  @Hidden
  @HelpMessage("Exclude modules *and their transitive dependencies* from the JAR to be packaged")
  @ValueDescription("org:name")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    provided: List[String] = Nil,

  @Group(HelpGroup.Package.toString)
  @HelpMessage("Use default scaladoc options")
  @ExtraName("defaultScaladocOpts")
  @Tag(tags.implementation)
    defaultScaladocOptions: Option[Boolean] = None,

  @Group(HelpGroup.Package.toString)
  @HelpMessage("Build GraalVM native image")
  @ExtraName("graal")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    nativeImage: Boolean = false
) extends HasSharedOptions {
  // format: on

  def packageTypeOpt: Option[PackageType] =
    forcedPackageTypeOpt.orElse {
      if (library) Some(PackageType.LibraryJar)
      else if (source) Some(PackageType.SourceJar)
      else if (assembly) Some(
        PackageType.Assembly(
          addPreamble = preamble,
          mainClassInManifest = mainClassInManifest
        )
      )
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

  def baseBuildOptions: Either[BuildException, BuildOptions] = either {
    val baseOptions = value(shared.buildOptions())
    baseOptions.copy(
      mainClass = mainClass.mainClass.filter(_.nonEmpty),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        packageOptions = baseOptions.notForBloopOptions.packageOptions.copy(
          standalone = standalone,
          version = Some(packager.version),
          launcherApp = packager.launcherApp,
          maintainer = packager.maintainer,
          description = packager.description,
          output = output,
          packageTypeOpt = packageTypeOpt,
          logoPath = packager.logoPath.map(os.Path(_, os.pwd)),
          macOSidentifier = packager.identifier,
          debianOptions = DebianOptions(
            conflicts = packager.debianConflicts,
            dependencies = packager.debianDependencies,
            architecture = Some(packager.debArchitecture),
            priority = packager.priority,
            section = packager.section
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
            installerVersion = packager.installerVersion,
            wixUpgradeCodeGuid = packager.wixUpgradeCodeGuid
          ),
          dockerOptions = DockerOptions(
            from = packager.dockerFrom,
            imageRegistry = packager.dockerImageRegistry,
            imageRepository = packager.dockerImageRepository,
            imageTag = packager.dockerImageTag,
            cmd = packager.dockerCmd,
            isDockerEnabled = Some(docker)
          ),
          nativeImageOptions = NativeImageOptions(
            graalvmJvmId = packager.graalvmJvmId.map(_.trim).filter(_.nonEmpty),
            graalvmJavaVersion = packager.graalvmJavaVersion.filter(_ > 0),
            graalvmVersion = packager.graalvmVersion.map(_.trim).filter(_.nonEmpty),
            graalvmArgs =
              packager.graalvmArgs.map(_.trim).filter(_.nonEmpty).map(Positioned.commandLine)
          ),
          useDefaultScaladocOptions = defaultScaladocOptions
        ),
        addRunnerDependencyOpt = Some(false)
      ),
      internal = baseOptions.internal.copy(
        // computing the provided modules sub-graph need the final Resolution instance
        // Spark packaging adds provided modules, so it needs it too
        keepResolution = provided.nonEmpty || packageTypeOpt.contains(PackageType.Spark)
      )
    )
  }

  def finalBuildOptions: Either[BuildException, BuildOptions] = either {
    val baseOptions = value(baseBuildOptions)
    baseOptions.copy(
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        packageOptions = baseOptions.notForBloopOptions.packageOptions.copy(
          provided = value(providedModules)
        )
      )
    )
  }

  def compilerMaker(threads: BuildThreads): Either[BuildException, ScalaCompilerMaker] = either {
    val maker = value(shared.compilerMaker(threads))
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

object PackageOptions {
  implicit lazy val parser: Parser[PackageOptions] = Parser.derive
  implicit lazy val help: Help[PackageOptions]     = Help.derive

  val cmdName             = "package"
  private val helpHeader  = "Compile and package Scala code."
  val helpMessage: String = HelpMessages.shortHelpMessage(cmdName, helpHeader, needsPower = true)
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandConfigurations(cmdName)}
       |
       |${HelpMessages.acceptedInputs}
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
