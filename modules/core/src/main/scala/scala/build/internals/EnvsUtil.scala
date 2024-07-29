package scala.build.internals

object EnvsUtil {

  /** @param name
    *   The name of the environment variable
    * @param description
    *   A short description what is it used for
    * @param passToIde
    *   Whether to pass this variable to the IDE/BSP client (true by default, should only be
    *   disabled for env vars which aren't safe to save on disk)
    */
  case class EnvVar(name: String, description: String, passToIde: Boolean = true) {
    def valueOpt: Option[String]  = Option(System.getenv(name))
    override def toString: String = s"$name=${valueOpt.getOrElse("")}"
  }
  object EnvVar {
    def all: Set[EnvVar] = Set(
      EnvVar.Java.all,
      EnvVar.Misc.all,
      EnvVar.Coursier.all,
      EnvVar.ScalaCli.all,
      EnvVar.Spark.all,
      EnvVar.Internal.all
    ).flatten
    def allBsp: Set[EnvVar] = all.filter(_.passToIde)
    object Java {
      def all         = Set(javaHome, javaOpts, jdkJavaOpts)
      val javaHome    = EnvVar("JAVA_HOME", "Java installation directory")
      val javaOpts    = EnvVar("JAVA_OPTS", "Java options")
      val jdkJavaOpts = EnvVar("JDK_JAVA_OPTIONS", "JDK Java options")
    }

    object Misc {
      def all = Set(
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
      val shell           = EnvVar("SHELL", "The currently used shell")
      val vcVarsAll       = EnvVar("VCVARSALL", "Visual C++ Redistributable Runtimes")
      val zDotDir         = EnvVar("ZDOTDIR", "Zsh configuration directory")
    }

    object Coursier {
      def all           = Set(coursierCache, coursierMode)
      val coursierCache = EnvVar("COURSIER_CACHE", "Coursier cache location")
      val coursierMode  = EnvVar("COURSIER_MODE", "Coursier mode (can be set to 'offline')")
    }

    object ScalaCli {
      def all = Set(
        config,
        home,
        interactive,
        interactiveInputs,
        power,
        printStackTraces,
        allowSodiumJni,
        vendoredZipInputStream
      )
      val config            = EnvVar("SCALA_CLI_CONFIG", "Scala CLI configuration file path")
      val home              = EnvVar("SCALA_CLI_HOME", "Scala CLI home directory")
      val interactive       = EnvVar("SCALA_CLI_INTERACTIVE", "Interactive mode toggle")
      val interactiveInputs = EnvVar("SCALA_CLI_INTERACTIVE_INPUTS", "Interactive mode inputs")
      val power             = EnvVar("SCALA_CLI_POWER", "Power mode toggle")
      val printStackTraces  = EnvVar("SCALA_CLI_PRINT_STACK_TRACES", "Print stack traces toggle")
      val allowSodiumJni    = EnvVar("SCALA_CLI_SODIUM_JNI_ALLOW", "Allow to load libsodiumjni")
      val vendoredZipInputStream =
        EnvVar("SCALA_CLI_VENDORED_ZIS", "Toggle io.github.scala_cli.zip.ZipInputStream")
    }

    object Spark {
      def all       = Set(sparkHome)
      val sparkHome = EnvVar("SPARK_HOME", "Spark installation directory")
    }

    object Internal {
      def all = Set(ci)
      val ci  = EnvVar("CI", "Marker for running on the CI")
    }
  }

}
