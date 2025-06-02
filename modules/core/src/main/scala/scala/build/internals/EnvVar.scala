package scala.build.internals

import scala.build.internals.ConsoleUtils.ScalaCliConsole

/** @param name
  *   The name of the environment variable
  * @param description
  *   A short description what is it used for
  * @param passToIde
  *   Whether to pass this variable to the IDE/BSP client (true by default, should only be disabled
  *   for env vars which aren't safe to save on disk)
  * @param requiresPower
  *   Whether this variable is related to a feature that requires power mode; also used for internal
  *   toggles and such
  */
case class EnvVar(
  name: String,
  description: String,
  passToIde: Boolean = true,
  requiresPower: Boolean = false
) {
  def valueOpt: Option[String]            = Option(System.getenv(name))
  override def toString: String           = s"$name=${valueOpt.getOrElse("")}"
  def helpMessage(spaces: String): String = {
    val powerString =
      if requiresPower then s"${ScalaCliConsole.GRAY}(power)${Console.RESET} " else ""
    s"${Console.YELLOW}$name${Console.RESET}$spaces$powerString$description"
  }
}
object EnvVar {
  def helpMessage(isPower: Boolean): String =
    s"""The following is the list of environment variables used and recognized by Scala CLI.
       |It should by no means be treated as an exhaustive list
       |Some tools and libraries Scala CLI integrates with may have their own, which may or may not be listed here.
       |${if isPower then "" else "For the expanded list, pass --power." + System.lineSeparator()}
       |${
        val maxFullNameLength =
          EnvVar.all.filter(!_.requiresPower || isPower).map(_.name.length).max
        EnvVar.allGroups
          .map(_.subsectionMessage(maxFullNameLength, isPower))
          .filter(_.linesIterator.size > 1)
          .mkString(s"${System.lineSeparator() * 2}")
      }""".stripMargin

  trait EnvVarGroup {
    def all: Seq[EnvVar]
    def groupName: String
    def subsectionMessage(maxFullNameLength: Int, isPower: Boolean): String = {
      val envsToInclude = all.filter(!_.requiresPower || isPower)
      s"""$groupName
         |${
          envsToInclude
            .map(ev =>
              s"  ${ev.helpMessage(spaces = " " * (maxFullNameLength - ev.name.length + 2))}"
            )
            .mkString(System.lineSeparator())
        }""".stripMargin
    }
  }
  def allGroups: Seq[EnvVarGroup] = Seq(ScalaCli, Java, Bloop, Coursier, Spark, Misc, Internal)
  def all: Seq[EnvVar]            = allGroups.flatMap(_.all)
  def allBsp: Seq[EnvVar]         = all.filter(_.passToIde)
  object Java extends EnvVarGroup {
    override def groupName: String = "Java"
    override def all               = Seq(javaHome, javaOpts, jdkJavaOpts)
    val javaHome                   = EnvVar("JAVA_HOME", "Java installation directory")
    val javaOpts                   = EnvVar("JAVA_OPTS", "Java options")
    val jdkJavaOpts                = EnvVar("JDK_JAVA_OPTIONS", "JDK Java options")
  }

  object Misc extends EnvVarGroup {
    override def groupName: String = "Miscellaneous"
    override def all               = Seq(
      path,
      dyldLibraryPath,
      ldLibraryPath,
      pathExt,
      shell,
      vcVarsAll,
      zDotDir
    )
    val path            = EnvVar("PATH", "The app path variable")
    val dyldLibraryPath = EnvVar("DYLD_LIBRARY_PATH", "Runtime library paths on Mac OS X")
    val ldLibraryPath   = EnvVar("LD_LIBRARY_PATH", "Runtime library paths on Linux")
    val pathExt         = EnvVar("PATHEXT", "Executable file extensions on Windows")
    val pwd             = EnvVar("PWD", "Current working directory", passToIde = false)
    val shell           = EnvVar("SHELL", "The currently used shell")
    val vcVarsAll       = EnvVar("VCVARSALL", "Visual C++ Redistributable Runtimes")
    val zDotDir         = EnvVar("ZDOTDIR", "Zsh configuration directory")
    val mavenHome       = EnvVar("MAVEN_HOME", "Maven home directory")
  }

  object Coursier extends EnvVarGroup {
    override def groupName: String = "Coursier"
    override def all               = Seq(
      coursierBinDir,
      coursierCache,
      coursierConfigDir,
      coursierCredentials,
      insideEmacs,
      coursierExperimental,
      coursierJni,
      coursierMode,
      coursierNoTerm,
      coursierProgress,
      coursierRepositories,
      coursierVendoredZis,
      csMavenHome
    )
    val coursierBinDir       = EnvVar("COURSIER_BIN_DIR", "Coursier app binaries directory")
    val coursierCache        = EnvVar("COURSIER_CACHE", "Coursier cache location")
    val coursierConfigDir    = EnvVar("COURSIER_CONFIG_DIR", "Coursier configuration directory")
    val coursierCredentials  = EnvVar("COURSIER_CREDENTIALS", "Coursier credentials")
    val coursierExperimental = EnvVar("COURSIER_EXPERIMENTAL", "Experimental mode toggle")
    val coursierJni          = EnvVar("COURSIER_JNI", "Coursier JNI toggle")
    val coursierMode         = EnvVar("COURSIER_MODE", "Coursier mode (can be set to 'offline')")
    val coursierNoTerm       = EnvVar("COURSIER_NO_TERM", "Terminal toggle")
    val coursierProgress     = EnvVar("COURSIER_PROGRESS", "Progress bar toggle")
    val coursierRepositories = EnvVar("COURSIER_REPOSITORIES", "Coursier repositories")
    val coursierVendoredZis  =
      EnvVar("COURSIER_VENDORED_ZIS", "Toggle io.github.scala_cli.zip.ZipInputStream")
    val csMavenHome = EnvVar("CS_MAVEN_HOME", "Coursier Maven home directory")
    val insideEmacs = EnvVar("INSIDE_EMACS", "Emacs toggle")
  }

  object ScalaCli extends EnvVarGroup {
    override def groupName: String = "Scala CLI"
    def all                        = Seq(
      config,
      home,
      interactive,
      interactiveInputs,
      power,
      printStackTraces,
      allowSodiumJni,
      vendoredZipInputStream
    )
    val config                 = EnvVar("SCALA_CLI_CONFIG", "Scala CLI configuration file path")
    val extraTimeout           = Bloop.bloopExtraTimeout.copy(requiresPower = false)
    val home                   = EnvVar("SCALA_CLI_HOME", "Scala CLI home directory")
    val interactive            = EnvVar("SCALA_CLI_INTERACTIVE", "Interactive mode toggle")
    val interactiveInputs      = EnvVar("SCALA_CLI_INTERACTIVE_INPUTS", "Interactive mode inputs")
    val power                  = EnvVar("SCALA_CLI_POWER", "Power mode toggle")
    val printStackTraces       = EnvVar("SCALA_CLI_PRINT_STACK_TRACES", "Print stack traces toggle")
    val allowSodiumJni         = EnvVar("SCALA_CLI_SODIUM_JNI_ALLOW", "Allow to load libsodiumjni")
    val vendoredZipInputStream =
      EnvVar("SCALA_CLI_VENDORED_ZIS", "Toggle io.github.scala_cli.zip.ZipInputStream")
  }

  object Spark extends EnvVarGroup {
    override def groupName: String = "Spark"
    override def all               = Seq(sparkHome)
    val sparkHome = EnvVar("SPARK_HOME", "Spark installation directory", requiresPower = true)
  }

  object Bloop extends EnvVarGroup {
    override def groupName: String = "Bloop"
    override def all               = Seq(
      bloopComputationCores,
      bloopDaemonDir,
      bloopJavaOpts,
      bloopModule,
      bloopPort,
      bloopScalaVersion,
      bloopVersion,
      bloopServer,
      bloopExtraTimeout
    )
    val bloopComputationCores = EnvVar(
      "BLOOP_COMPUTATION_CORES",
      "Number of computation cores to be used",
      requiresPower = true
    )
    val bloopDaemonDir = EnvVar("BLOOP_DAEMON_DIR", "Bloop daemon directory", requiresPower = true)
    val bloopJavaOpts  = EnvVar("BLOOP_JAVA_OPTS", "Bloop Java options", requiresPower = true)
    val bloopModule    = EnvVar("BLOOP_MODULE", "Bloop default module", requiresPower = true)
    val bloopPort      = EnvVar("BLOOP_PORT", "Bloop default port", requiresPower = true)
    val bloopScalaVersion =
      EnvVar("BLOOP_SCALA_VERSION", "Bloop default Scala version", requiresPower = true)
    val bloopVersion      = EnvVar("BLOOP_VERSION", "Bloop default version", requiresPower = true)
    val bloopServer       = EnvVar("BLOOP_SERVER", "Bloop default host", requiresPower = true)
    val bloopExtraTimeout = EnvVar("SCALA_CLI_EXTRA_TIMEOUT", "Extra timeout", requiresPower = true)
  }

  object Internal extends EnvVarGroup {
    override def groupName: String = "Internal"
    def all                        = Seq(ci)
    val ci = EnvVar("CI", "Marker for running on the CI", requiresPower = true)
  }
}
