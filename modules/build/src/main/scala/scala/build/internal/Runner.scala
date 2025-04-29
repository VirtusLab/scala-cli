package scala.build.internal

import coursier.jvm.Execve
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.jsenv.{Input, JSEnv, RunConfig}
import org.scalajs.testing.adapter.TestAdapter as ScalaJsTestAdapter
import sbt.testing.{Framework, Status}

import java.io.File
import java.nio.file.{Files, Path, Paths}

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops.EitherSeqOps
import scala.build.errors.*
import scala.build.internals.EnvVar
import scala.build.testrunner.FrameworkUtils.*
import scala.build.testrunner.{AsmTestRunner, TestRunner}
import scala.scalanative.testinterface.adapter.TestAdapter as ScalaNativeTestAdapter
import scala.util.{Failure, Properties, Success}

object Runner {

  def maybeExec(
    commandName: String,
    command: Seq[String],
    logger: Logger,
    cwd: Option[os.Path] = None,
    extraEnv: Map[String, String] = Map.empty
  ): Process =
    run0(
      commandName,
      command,
      logger,
      allowExecve = true,
      cwd,
      extraEnv,
      inheritStreams = true
    )

  def run(
    command: Seq[String],
    logger: Logger,
    cwd: Option[os.Path] = None,
    extraEnv: Map[String, String] = Map.empty,
    inheritStreams: Boolean = true
  ): Process =
    run0(
      "unused",
      command,
      logger,
      allowExecve = false,
      cwd,
      extraEnv,
      inheritStreams
    )

  def run0(
    commandName: String,
    command: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    cwd: Option[os.Path],
    extraEnv: Map[String, String],
    inheritStreams: Boolean
  ): Process = {

    import logger.{log, debug}

    log(
      s"Running ${command.mkString(" ")}",
      "  Running" + System.lineSeparator() +
        command.iterator.map(_ + System.lineSeparator()).mkString
    )

    if (allowExecve && Execve.available()) {
      debug("execve available")

      for (dir <- cwd)
        Chdir.chdir(dir.toString)

      Execve.execve(
        findInPath(command.head).fold(command.head)(_.toString),
        commandName +: command.tail.toArray,
        (sys.env ++ extraEnv).toArray.sorted.map { case (k, v) => s"$k=$v" }
      )
      sys.error("should not happen")
    }
    else {
      val b = new ProcessBuilder(command: _*)
        .inheritIO()

      if (!inheritStreams) {
        b.redirectInput(ProcessBuilder.Redirect.PIPE)
        b.redirectOutput(ProcessBuilder.Redirect.PIPE)
      }

      if (extraEnv.nonEmpty) {
        val env = b.environment()
        for ((k, v) <- extraEnv)
          env.put(k, v)
      }
      for (dir <- cwd)
        b.directory(dir.toIO)
      val process = b.start()
      process
    }
  }

  def envCommand(env: Map[String, String]): Seq[String] =
    env.toVector.sortBy(_._1).map {
      case (k, v) =>
        s"$k=$v"
    }

  def jvmCommand(
    javaCommand: String,
    javaArgs: Seq[String],
    classPath: Seq[os.Path],
    mainClass: String,
    args: Seq[String],
    extraEnv: Map[String, String] = Map.empty,
    useManifest: Option[Boolean] = None,
    scratchDirOpt: Option[os.Path] = None
  ): Seq[String] = {

    def command(cp: Seq[os.Path]) =
      envCommand(extraEnv) ++
        Seq(javaCommand) ++
        javaArgs ++
        Seq(
          "-cp",
          cp.iterator.map(_.toString).mkString(File.pathSeparator),
          mainClass
        ) ++
        args

    val initialCommand = command(classPath)

    val useManifest0 = useManifest.getOrElse {
      Properties.isWin && {
        val commandLen = initialCommand.map(_.length).sum + (initialCommand.length - 1)
        // On Windows, total command lengths have this limit. Note that the same kind
        // of limit applies the environment, so that we can't sneak in info via env vars to
        // overcome the command length limit.
        // See https://devblogs.microsoft.com/oldnewthing/20031210-00/?p=41553
        commandLen >= 32767
      }
    }

    if (useManifest0) {
      val manifestJar = ManifestJar.create(classPath, scratchDirOpt = scratchDirOpt)
      command(Seq(manifestJar))
    }
    else initialCommand
  }

  def runJvm(
    javaCommand: String,
    javaArgs: Seq[String],
    classPath: Seq[os.Path],
    mainClass: String,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean = false,
    cwd: Option[os.Path] = None,
    extraEnv: Map[String, String] = Map.empty,
    useManifest: Option[Boolean] = None,
    scratchDirOpt: Option[os.Path] = None
  ): Process = {

    val command = jvmCommand(
      javaCommand,
      javaArgs,
      classPath,
      mainClass,
      args,
      Map.empty,
      useManifest,
      scratchDirOpt
    )

    if (allowExecve)
      maybeExec("java", command, logger, cwd = cwd, extraEnv = extraEnv)
    else
      run(command, logger, cwd = cwd, extraEnv = extraEnv)
  }

  private def endsWithCaseInsensitive(s: String, suffix: String): Boolean =
    s.length >= suffix.length &&
    s.regionMatches(true, s.length - suffix.length, suffix, 0, suffix.length)

  private def findInPath(app: String): Option[Path] = {
    val asIs = Paths.get(app)
    if (Paths.get(app).getNameCount >= 2) Some(asIs)
    else {
      def pathEntries =
        EnvVar.Misc.path.valueOpt
          .iterator
          .flatMap(_.split(File.pathSeparator).iterator)
      def pathSep =
        if (Properties.isWin)
          EnvVar.Misc.pathExt.valueOpt
            .iterator
            .flatMap(_.split(File.pathSeparator).iterator)
        else Iterator("")
      def matches = for {
        dir <- pathEntries
        ext <- pathSep
        app0 = if (endsWithCaseInsensitive(app, ext)) app else app + ext
        path = Paths.get(dir).resolve(app0)
        if Files.isExecutable(path)
      } yield path
      matches.take(1).toList.headOption
    }
  }

  def jsCommand(
    entrypoint: File,
    args: Seq[String],
    jsDom: Boolean = false
  ): Seq[String] = {

    val nodePath = findInPath("node").fold("node")(_.toString)
    val command  = Seq(nodePath, entrypoint.getAbsolutePath) ++ args

    if (jsDom)
      // FIXME We'd need to replicate what JSDOMNodeJSEnv does under-the-hood to get the command in that case.
      // --command is mostly for debugging purposes, so I'm not sure it matters much here…
      sys.error("Cannot get command when JSDOM is enabled.")
    else
      "node" +: command.tail
  }

  def runJs(
    entrypoint: File,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean = false,
    jsDom: Boolean = false,
    sourceMap: Boolean = false,
    esModule: Boolean = false
  ): Either[BuildException, Process] = either {
    val nodePath: String =
      value(findInPath("node")
        .map(_.toString)
        .toRight(NodeNotFoundError()))
    if !jsDom && allowExecve && Execve.available() then {
      val command = Seq(nodePath, entrypoint.getAbsolutePath) ++ args

      logger.log(
        s"Running ${command.mkString(" ")}",
        "  Running" + System.lineSeparator() +
          command.iterator.map(_ + System.lineSeparator()).mkString
      )

      logger.debug("execve available")
      Execve.execve(
        command.head,
        "node" +: command.tail.toArray,
        sys.env.toArray.sorted.map { case (k, v) => s"$k=$v" }
      )
      sys.error("should not happen")
    }
    else {
      val nodeArgs =
        // Scala.js runs apps by piping JS to node.
        // If we need to pass arguments, we must first make the piped input explicit
        // with "-", and we pass the user's arguments after that.
        if args.isEmpty then Nil else "-" :: args.toList
      val envJs =
        if jsDom then
          new JSDOMNodeJSEnv(
            JSDOMNodeJSEnv.Config()
              .withExecutable(nodePath)
              .withArgs(nodeArgs)
              .withEnv(Map.empty)
          )
        else
          new NodeJSEnv(
            NodeJSEnv.Config()
              .withExecutable(nodePath)
              .withArgs(nodeArgs)
              .withEnv(Map.empty)
              .withSourceMap(sourceMap)
          )

      val inputs =
        Seq(if esModule then Input.ESModule(entrypoint.toPath) else Input.Script(entrypoint.toPath))

      val config    = RunConfig().withLogger(logger.scalaJsLogger)
      val processJs = envJs.start(inputs, config)

      processJs.future.value.foreach {
        case Failure(t) => throw new Exception(t)
        case Success(_) =>
      }

      val processField =
        processJs.getClass.getDeclaredField("org$scalajs$jsenv$ExternalJSRun$$process")
      processField.setAccessible(true)
      val process = processField.get(processJs).asInstanceOf[Process]
      process
    }
  }

  def runNative(
    launcher: File,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean = false,
    extraEnv: Map[String, String] = Map.empty
  ): Process = {

    import logger.{log, debug}

    val command = Seq(launcher.getAbsolutePath) ++ args

    log(
      s"Running ${command.mkString(" ")}",
      "  Running" + System.lineSeparator() +
        command.iterator.map(_ + System.lineSeparator()).mkString
    )

    if (allowExecve && Execve.available()) {
      debug("execve available")
      Execve.execve(
        command.head,
        launcher.getName +: command.tail.toArray,
        (sys.env ++ extraEnv).toArray.sorted.map { case (k, v) => s"$k=$v" }
      )
      sys.error("should not happen")
    }
    else {
      val builder = new ProcessBuilder(command: _*)
        .inheritIO()
      val env = builder.environment()
      for ((k, v) <- extraEnv)
        env.put(k, v)
      builder.start()
    }
  }

  private def runTests(
    classPath: Seq[Path],
    frameworks: Seq[Framework],
    requireTests: Boolean,
    args: Seq[String],
    parentInspector: AsmTestRunner.ParentInspector
  ): Either[NoTestsRun, Boolean] = frameworks
    .flatMap { framework =>
      val taskDefs =
        AsmTestRunner.taskDefs(
          classPath,
          keepJars = false,
          framework.fingerprints().toIndexedSeq,
          parentInspector
        ).toArray

      val runner       = framework.runner(args.toArray, Array(), null)
      val initialTasks = runner.tasks(taskDefs)
      val events       = TestRunner.runTasks(initialTasks.toIndexedSeq, System.out)

      val doneMsg = runner.done()
      if doneMsg.nonEmpty then System.out.println(doneMsg)
      events
    } match {
    case events if requireTests && events.isEmpty => Left(new NoTestsRun)
    case events => Right {
        !events.exists { ev =>
          ev.status == Status.Error ||
          ev.status == Status.Failure ||
          ev.status == Status.Canceled
        }
      }
  }

  def frameworkNames(
    classPath: Seq[Path],
    parentInspector: AsmTestRunner.ParentInspector,
    logger: Logger
  ): Either[NoTestFrameworkFoundError, Seq[String]] = {
    logger.debug("Looking for test framework services on the classpath...")
    val foundFrameworkServices =
      AsmTestRunner.findFrameworkServices(classPath)
        .map(_.replace('/', '.').replace('\\', '.'))
    logger.debug(s"Found ${foundFrameworkServices.length} test framework services.")
    if foundFrameworkServices.nonEmpty then
      logger.debug(s"  - ${foundFrameworkServices.mkString("\n  - ")}")
    logger.debug("Looking for more test frameworks on the classpath...")
    val foundFrameworks =
      AsmTestRunner.findFrameworks(classPath, TestRunner.commonTestFrameworks, parentInspector)
        .map(_.replace('/', '.').replace('\\', '.'))
    logger.debug(s"Found ${foundFrameworks.length} additional test frameworks")
    if foundFrameworks.nonEmpty then
      logger.debug(s"  - ${foundFrameworks.mkString("\n  - ")}")
    val frameworks: Seq[String] = foundFrameworkServices ++ foundFrameworks
    logger.log(s"Found ${frameworks.length} test frameworks in total")
    if frameworks.nonEmpty then
      logger.debug(s"  - ${frameworks.mkString("\n  - ")}")
    if frameworks.nonEmpty then Right(frameworks) else Left(new NoTestFrameworkFoundError)
  }

  def testJs(
    classPath: Seq[Path],
    entrypoint: File,
    requireTests: Boolean,
    args: Seq[String],
    predefinedTestFrameworks: Seq[String],
    logger: Logger,
    jsDom: Boolean,
    esModule: Boolean
  ): Either[TestError, Int] = either {
    import org.scalajs.jsenv.Input
    import org.scalajs.jsenv.nodejs.NodeJSEnv
    logger.debug("Preparing to run tests with Scala.js...")
    logger.debug(s"Scala.js tests class path: $classPath")
    val nodePath = findInPath("node").fold("node")(_.toString)
    logger.debug(s"Node found at $nodePath")
    val jsEnv: JSEnv =
      if jsDom then {
        logger.log("Loading JS environment with JS DOM...")
        new JSDOMNodeJSEnv(
          JSDOMNodeJSEnv.Config()
            .withExecutable(nodePath)
            .withArgs(Nil)
            .withEnv(Map.empty)
        )
      }
      else {
        logger.log("Loading JS environment with Node...")
        new NodeJSEnv(
          NodeJSEnv.Config()
            .withExecutable(nodePath)
            .withArgs(Nil)
            .withEnv(Map.empty)
            .withSourceMap(NodeJSEnv.SourceMap.Disable)
        )
      }
    val adapterConfig = ScalaJsTestAdapter.Config().withLogger(logger.scalaJsLogger)
    val inputs =
      Seq(if esModule then Input.ESModule(entrypoint.toPath) else Input.Script(entrypoint.toPath))
    var adapter: ScalaJsTestAdapter = null

    logger.debug(s"JS tests class path: $classPath")

    val parentInspector = new AsmTestRunner.ParentInspector(classPath)
    val foundFrameworkNames: List[String] = predefinedTestFrameworks match {
      case f if f.nonEmpty => f.toList
      case Nil             => value(frameworkNames(classPath, parentInspector, logger)).toList
    }

    val res =
      try {
        adapter = new ScalaJsTestAdapter(jsEnv, inputs, adapterConfig)

        val loadedFrameworks =
          adapter
            .loadFrameworks(foundFrameworkNames.map(List(_)))
            .flatten
            .distinctBy(_.name())

        val finalTestFrameworks =
          loadedFrameworks
            .filter(
              !_.name().toLowerCase.contains("junit") ||
              !loadedFrameworks.exists(_.name().toLowerCase.contains("munit"))
            )
        if finalTestFrameworks.nonEmpty then
          logger.log(
            s"""Final list of test frameworks found:
               |  - ${finalTestFrameworks.map(_.description).mkString("\n  - ")}
               |""".stripMargin
          )

        if finalTestFrameworks.isEmpty then Left(new NoFrameworkFoundByBridgeError)
        else runTests(classPath, finalTestFrameworks, requireTests, args, parentInspector)
      }
      finally if adapter != null then adapter.close()

    if value(res) then 0 else 1
  }

  def testNative(
    classPath: Seq[Path],
    launcher: File,
    predefinedTestFrameworks: Seq[String],
    requireTests: Boolean,
    args: Seq[String],
    logger: Logger
  ): Either[TestError, Int] = either {
    logger.debug("Preparing to run tests with Scala Native...")
    logger.debug(s"Native tests class path: $classPath")

    val parentInspector = new AsmTestRunner.ParentInspector(classPath)
    val foundFrameworkNames: List[String] = predefinedTestFrameworks match {
      case f if f.nonEmpty => f.toList
      case Nil             => value(frameworkNames(classPath, parentInspector, logger)).toList
    }

    val config = ScalaNativeTestAdapter.Config()
      .withBinaryFile(launcher)
      .withEnvVars(sys.env)
      .withLogger(logger.scalaNativeTestLogger)

    var adapter: ScalaNativeTestAdapter = null

    val res =
      try {
        adapter = new ScalaNativeTestAdapter(config)

        val loadedFrameworks =
          adapter
            .loadFrameworks(foundFrameworkNames.map(List(_)))
            .flatten
            .distinctBy(_.name())

        val finalTestFrameworks =
          loadedFrameworks
            // .filter(
            //  _.name() != "Scala Native JUnit test framework" ||
            //    !loadedFrameworks.exists(_.name() == "munit")
            // )
            // TODO: add support for JUnit and then only hardcode filtering it out when passed with munit
            // https://github.com/VirtusLab/scala-cli/issues/3627
            .filter(_.name() != "Scala Native JUnit test framework")
        if finalTestFrameworks.nonEmpty then
          logger.log(
            s"""Final list of test frameworks found:
               |  - ${finalTestFrameworks.map(_.description).mkString("\n  - ")}
               |""".stripMargin
          )

        val skippedFrameworks = loadedFrameworks.diff(finalTestFrameworks)
        if skippedFrameworks.nonEmpty then
          logger.log(
            s"""The following test frameworks have been filtered out:
               |  - ${skippedFrameworks.map(_.description).mkString("\n  - ")}
               |""".stripMargin
          )

        if finalTestFrameworks.isEmpty then Left(new NoFrameworkFoundByBridgeError)
        else runTests(classPath, finalTestFrameworks, requireTests, args, parentInspector)
      }
      finally if adapter != null then adapter.close()

    if value(res) then 0 else 1
  }
}
