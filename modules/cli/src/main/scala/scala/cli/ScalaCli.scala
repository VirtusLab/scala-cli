package scala.cli

import bloop.rifle.FailedToStartServerException
import coursier.core.Version
import sun.misc.{Signal, SignalHandler}

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Locale

import scala.build.Directories
import scala.build.internal.Constants
import scala.build.internals.EnvVar
import scala.cli.commands.CommandUtils
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.internal.Argv0
import scala.cli.javaLauncher.JavaLauncherCli
import scala.cli.launcher.{LauncherCli, LauncherOptions, PowerOptions}
import scala.cli.publish.BouncycastleSignerMaker
import scala.cli.util.ConfigDbUtils
import scala.collection.mutable.ListBuffer
import scala.util.Properties

object ScalaCli {

  if (Properties.isWin && isGraalvmNativeImage)
    // have to be initialized before running (new Argv0).get because Argv0SubstWindows uses csjniutils library
    // The DLL loaded by LoadWindowsLibrary is statically linke/d in
    // the Scala CLI native image, no need to manually load it.
    coursier.jniutils.LoadWindowsLibrary.assumeInitialized()

  private val defaultProgName = "scala-cli"
  var progName: String = {
    val argv0 = (new Argv0).get(defaultProgName)
    val last  = Paths.get(argv0).getFileName.toString
    last match {
      case s".${name}.aux" =>
        name // cs installs binaries under .app-name.aux and adds them to the PATH
      case _ => argv0
    }
  }
  private val scalaCliBinaryName = "scala-cli"
  private var isSipScala = {
    lazy val isPowerConfigDb = for {
      configDb   <- ConfigDbUtils.configDb.toOption
      powerEntry <- configDb.get(Keys.power).toOption
      power      <- powerEntry
    } yield power
    val isPowerEnv = EnvVar.ScalaCli.power.valueOpt.flatMap(_.toBooleanOption)
    val isPower    = isPowerEnv.orElse(isPowerConfigDb).getOrElse(false)
    !isPower
  }
  def setPowerMode(power: Boolean): Unit = isSipScala = !power
  def allowRestrictedFeatures            = !isSipScala
  def fullRunnerName =
    if (progName.contains(scalaCliBinaryName)) "Scala CLI" else "Scala code runner"
  def baseRunnerName = if (progName.contains(scalaCliBinaryName)) scalaCliBinaryName else "scala"
  private def isGraalvmNativeImage: Boolean =
    sys.props.contains("org.graalvm.nativeimage.imagecode")

  private var maybeLauncherOptions: Option[LauncherOptions] = None

  def launcherOptions: LauncherOptions = maybeLauncherOptions.getOrElse(LauncherOptions())
  def getDefaultScalaVersion: String =
    launcherOptions.scalaRunner.cliUserScalaVersion.getOrElse(Constants.defaultScalaVersion)

  private var launcherJavaPropArgs: List[String] = List.empty

  def getLauncherJavaPropArgs: List[String] = launcherJavaPropArgs

  private def partitionArgs(args: Array[String]): (Array[String], Array[String]) = {
    val systemProps = args.takeWhile(_.startsWith("-D"))
    (systemProps, args.drop(systemProps.size))
  }

  private def setSystemProps(systemProps: Array[String]): Unit = {
    systemProps.map(_.stripPrefix("-D")).foreach { prop =>
      prop.split("=", 2) match {
        case Array(key, value) =>
          System.setProperty(key, value)
        case Array(key) =>
          System.setProperty(key, "")
      }
    }
  }
  private def printThrowable(t: Throwable, out: PrintStream): Unit =
    if (t != null) {
      out.println(t.toString)
      // FIXME Print t.getSuppressed too?
      for (l <- t.getStackTrace)
        out.println(s"  $l")
      printThrowable(t.getCause, out)
    }

  private def printThrowable(t: Throwable): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    val ps   = new PrintStream(baos, true, StandardCharsets.UTF_8.name())
    printThrowable(t, ps)
    baos.toByteArray
  }

  private def isCI = EnvVar.Internal.ci.valueOpt.nonEmpty
  private def printStackTraces = EnvVar.ScalaCli.printStackTraces.valueOpt
    .map(_.toLowerCase(Locale.ROOT))
    .exists {
      case "true" | "1" => true
      case _            => false
    }

  private def ignoreSigpipe(): Unit =
    Signal.handle(new Signal("PIPE"), SignalHandler.SIG_IGN)

  private def isJava17ClassName(name: String): Boolean =
    name == "java/net/UnixDomainSocketAddress"

  private lazy val javaMajorVersion =
    sys.props.getOrElse("java.version", "0")
      .stripPrefix("1.")
      .takeWhile(_.isDigit)
      .toInt

  def main(args: Array[String]): Unit =
    try main0(args)
    catch {
      case e: Throwable if !isCI && !printStackTraces =>
        val workspace = CurrentParams.workspaceOpt.filter(os.isDir).getOrElse(os.pwd)
        val dir       = workspace / Constants.workspaceDirName / "stacktraces"
        os.makeDir.all(dir)
        import java.time.Instant

        val tempFile = os.temp(
          contents = printThrowable(e),
          dir = dir,
          prefix = Instant.now().getEpochSecond().toString() + "-",
          suffix = ".log",
          deleteOnExit = false
        )

        if (CurrentParams.verbosity <= 1) {
          System.err.println(s"Error: $e")
          System.err.println(s"For more details, please see '$tempFile'")
        }

        e match {
          case _: UnsupportedClassVersionError
              if javaMajorVersion < Constants.minimumBloopJavaVersion =>
            warnRequiresMinimumBloopJava()
          case _: NoClassDefFoundError
              if isJava17ClassName(e.getMessage) &&
              CurrentParams.verbosity <= 1 &&
              javaMajorVersion < Constants.minimumInternalJavaVersion =>
            // Actually Java >= 16 here, but let's recommend a LTS version…
            warnRequiresMinimumBloopJava()
          case _: FailedToStartServerException =>
            System.err.println(
              s"""Running
                 |  $progName --power bloop output
                 |might give more details.""".stripMargin
            )
          case ex: java.util.zip.ZipException
              if !Properties.isWin && ex.getMessage.contains("invalid entry CRC") =>
            // Suggest workaround of https://github.com/VirtusLab/scala-cli/pull/865
            // for https://github.com/VirtusLab/scala-cli/issues/828
            System.err.println(
              s"""Running
                 |  export ${EnvVar.ScalaCli.vendoredZipInputStream.name}=true
                 |before running $fullRunnerName might fix the issue.
                 |""".stripMargin
            )
          case _ =>
        }

        if (CurrentParams.verbosity >= 2) throw e
        else sys.exit(1)
    }

  private def warnRequiresMinimumBloopJava(): Unit =
    System.err.println(
      s"Java >= ${Constants.minimumBloopJavaVersion} is required to run $fullRunnerName (found Java $javaMajorVersion)"
    )

  def loadJavaProperties(cwd: os.Path) = {
    // load java properties from scala-cli-properties resource file
    val prop = new java.util.Properties()
    val cl   = getClass.getResourceAsStream("/java-properties/scala-cli-properties")
    if cl != null then
      prop.load(cl)
      prop.stringPropertyNames().forEach(name => System.setProperty(name, prop.getProperty(name)))
    // load java properties from .scala-jvmopts located in the current working directory and filter only java properties and warning if someone used other options
    val jvmopts = cwd / Constants.jvmPropertiesFileName
    if os.exists(jvmopts) && os.isFile(jvmopts) then
      val jvmoptsContent        = os.read(jvmopts)
      val jvmoptsLines          = jvmoptsContent.linesIterator.toSeq
      val (javaOpts, otherOpts) = jvmoptsLines.partition(_.startsWith("-D"))
      javaOpts.foreach { opt =>
        opt.stripPrefix("-D").split("=", 2) match {
          case Array(key, value) => System.setProperty(key, value)
          case _                 => System.err.println(s"Warning: Invalid java property: $opt")
        }
      }
      if otherOpts.nonEmpty then
        System.err.println(
          s"Warning: Only java properties are supported in .scala-jvmopts file. Other options are ignored: ${otherOpts.mkString(", ")}"
        )
    // load java properties from config
    for {
      configDb   <- ConfigDbUtils.configDb.toOption
      properties <- configDb.get(Keys.javaProperties).getOrElse(Nil)
    }
      properties.foreach { opt =>
        opt.stripPrefix("-D").split("=", 2) match {
          case Array(key, value) => System.setProperty(key, value)
          case _ => System.err.println(s"Warning: Invalid java property in config: $opt")
        }
      }

    // load java properties from JAVA_OPTS and JDK_JAVA_OPTIONS environment variables
    val javaOpts: Seq[String] =
      EnvVar.Java.javaOpts.valueOpt.toSeq ++ EnvVar.Java.jdkJavaOpts.valueOpt.toSeq

    val ignoredJavaOpts =
      javaOpts
        .flatMap(_.split("\\s+"))
        .flatMap { opt =>
          opt.stripPrefix("-D").split("=", 2) match {
            case Array(key, value) =>
              System.setProperty(key, value)
              None
            case ignored => Some(ignored) // non-property opts are ignored here
          }
        }.flatten
    if ignoredJavaOpts.nonEmpty then
      System.err.println(
        s"Warning: Only java properties are supported in ${EnvVar.Java.javaOpts.name} and ${EnvVar
            .Java.jdkJavaOpts.name} environment variables. Other options are ignored: ${ignoredJavaOpts.mkString(", ")}"
      )
  }

  private def main0(args: Array[String]): Unit = {
    loadJavaProperties(cwd = os.pwd) // load java properties to detect launcher kind
    val remainingArgs = LauncherOptions.parser.stopAtFirstUnrecognized.parse(args.toVector) match {
      case Left(e) =>
        System.err.println(e.message)
        sys.exit(1)
      case Right((launcherOpts, args0)) =>
        maybeLauncherOptions = Some(launcherOpts)
        launcherOpts.cliVersion.map(_.trim).filter(_.nonEmpty) match {
          case Some(ver) =>
            val powerArgs              = launcherOpts.powerOptions.toCliArgs
            val initialScalaRunnerArgs = launcherOpts.scalaRunner
            val finalScalaRunnerArgs = (Version(ver) match
              case v if v < Version("1.4.0") && !ver.contains("nightly") =>
                initialScalaRunnerArgs.copy(
                  skipCliUpdates = None,
                  predefinedCliVersion = None,
                  initialLauncherPath = None
                )
              case v
                  if v < Version("1.5.0-34-g31a88e428-SNAPSHOT") && v < Version("1.5.1") &&
                  !ver.contains("nightly") =>
                initialScalaRunnerArgs.copy(
                  predefinedCliVersion = None,
                  initialLauncherPath = None
                )
              case _ if initialScalaRunnerArgs.initialLauncherPath.nonEmpty =>
                initialScalaRunnerArgs
              case _ =>
                initialScalaRunnerArgs.copy(
                  predefinedCliVersion = Some(ver),
                  initialLauncherPath = Some(CommandUtils.getAbsolutePathToScalaCli(progName))
                )
            ).toCliArgs
            val newArgs = powerArgs ++ finalScalaRunnerArgs ++ args0
            LauncherCli.runAndExit(ver, launcherOpts, newArgs)
          case _ if
                javaMajorVersion < Constants.minimumBloopJavaVersion
                && sys.props.get("scala-cli.kind").exists(_.startsWith("jvm")) =>
            JavaLauncherCli.runAndExit(args)
          case None =>
            launcherOpts.scalaRunner.progName
              .foreach(pn => progName = pn)
            if launcherOpts.powerOptions.power then
              isSipScala = false
              args0.toArray
            else
              // Parse again to register --power at any position
              // Don't consume it, GlobalOptions parsing will do it
              PowerOptions.parser.ignoreUnrecognized.parse(args0) match {
                case Right((powerOptions, _)) =>
                  if powerOptions.power then
                    isSipScala = false
                  args0.toArray
                case Left(e) =>
                  System.err.println(e.message)
                  sys.exit(1)
              }
        }
    }
    val (systemProps, scalaCliArgs) = partitionArgs(remainingArgs)
    if systemProps.nonEmpty then launcherJavaPropArgs = systemProps.toList
    setSystemProps(systemProps)

    (new BouncycastleSignerMaker).maybeInit()

    coursier.Resolve.proxySetup()

    // Getting killed by SIGPIPE quite often when on musl (in the "static" native
    // image), but also sometimes on glibc, or even on macOS, when we use domain
    // sockets to exchange with Bloop. So let's just ignore those (which should
    // just make some read / write calls return -1).
    if (!Properties.isWin && isGraalvmNativeImage)
      ignoreSigpipe()

    if (Properties.isWin && System.console() != null && coursier.paths.Util.useJni())
      // Enable ANSI output in Windows terminal
      try
        coursier.jniutils.WindowsAnsiTerminal.enableAnsiOutput()
      catch {
        // ignore error resulting from redirect STDOUT to /dev/null
        case e: java.io.IOException
            if e.getMessage.contains("GetConsoleMode error 6") =>
      }

    new ScalaCliCommands(progName, baseRunnerName, fullRunnerName)
      .main(scalaCliArgs)
  }
}
