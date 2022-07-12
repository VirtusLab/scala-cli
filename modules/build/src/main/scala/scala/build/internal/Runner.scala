package scala.build.internal

import coursier.jvm.Execve
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.jsenv.nodejs.NodeJSEnv
import org.scalajs.jsenv.{Input, RunConfig}
import sbt.testing.{Framework, Status}

import java.io.File
import java.nio.file.{Files, Path, Paths}

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.{
  NoFrameworkFoundByBridgeError,
  NoTestFrameworkFoundError,
  NoTestsRun,
  TestError,
  TooManyFrameworksFoundByBridgeError
}
import scala.build.testrunner.{AsmTestRunner, TestRunner}
import scala.util.Properties

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
      extraEnv
    )

  def run(
    command: Seq[String],
    logger: Logger,
    cwd: Option[os.Path] = None,
    extraEnv: Map[String, String] = Map.empty
  ): Process =
    run0(
      "unused",
      command,
      logger,
      allowExecve = false,
      cwd,
      extraEnv
    )

  def run0(
    commandName: String,
    command: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    cwd: Option[os.Path],
    extraEnv: Map[String, String]
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

  private def envCommand(env: Map[String, String]): Seq[String] =
    env.toVector.sortBy(_._1).map {
      case (k, v) =>
        s"$k=$v"
    }

  def jvmCommand(
    javaCommand: String,
    javaArgs: Seq[String],
    classPath: Seq[File],
    mainClass: String,
    args: Seq[String],
    extraEnv: Map[String, String] = Map.empty
  ): Seq[String] = {

    val command =
      Seq(javaCommand) ++
        javaArgs ++
        Seq(
          "-cp",
          classPath.iterator.map(_.getAbsolutePath).mkString(File.pathSeparator),
          mainClass
        ) ++
        args

    envCommand(extraEnv) ++ command
  }

  def runJvm(
    javaCommand: String,
    javaArgs: Seq[String],
    classPath: Seq[File],
    mainClass: String,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean = false,
    cwd: Option[os.Path] = None,
    extraEnv: Map[String, String] = Map.empty
  ): Process = {

    val command = jvmCommand(javaCommand, javaArgs, classPath, mainClass, args, Map.empty)

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
        Option(System.getenv("PATH"))
          .iterator
          .flatMap(_.split(File.pathSeparator).iterator)
      def pathSep =
        if (Properties.isWin)
          Option(System.getenv("PATHEXT"))
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
  ): Process = {

    import logger.{log, debug}

    val nodePath = findInPath("node").fold("node")(_.toString)

    if (!jsDom && allowExecve && Execve.available()) {

      val command = Seq(nodePath, entrypoint.getAbsolutePath) ++ args

      log(
        s"Running ${command.mkString(" ")}",
        "  Running" + System.lineSeparator() +
          command.iterator.map(_ + System.lineSeparator()).mkString
      )

      debug("execve available")
      Execve.execve(
        command.head,
        "node" +: command.tail.toArray,
        sys.env.toArray.sorted.map { case (k, v) => s"$k=$v" }
      )
      sys.error("should not happen")
    }
    else {

      val envJs =
        if (jsDom)
          new JSDOMNodeJSEnv(
            JSDOMNodeJSEnv.Config()
              .withExecutable(nodePath)
              .withArgs(Nil)
              .withEnv(Map.empty)
          )
        else new NodeJSEnv(
          NodeJSEnv.Config()
            .withExecutable(nodePath)
            .withArgs(Nil)
            .withEnv(Map.empty)
            .withSourceMap(sourceMap)
        )

      val inputs = Seq(
        if (esModule) Input.ESModule(entrypoint.toPath)
        else Input.Script(entrypoint.toPath)
      )

      val config    = RunConfig().withLogger(logger.scalaJsLogger)
      val processJs = envJs.start(inputs, config)

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
    allowExecve: Boolean = false
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
        sys.env.toArray.sorted.map { case (k, v) => s"$k=$v" }
      )
      sys.error("should not happen")
    }
    else {
      val process = new ProcessBuilder(command: _*)
        .inheritIO()
        .start()
      process
    }
  }

  private def runTests(
    classPath: Seq[Path],
    framework: Framework,
    requireTests: Boolean,
    args: Seq[String],
    parentInspector: AsmTestRunner.ParentInspector
  ): Either[NoTestsRun, Boolean] = {

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
    if (doneMsg.nonEmpty)
      System.out.println(doneMsg)

    if (requireTests && events.isEmpty)
      Left(new NoTestsRun)
    else
      Right {
        !events.exists { ev =>
          ev.status == Status.Error ||
          ev.status == Status.Failure ||
          ev.status == Status.Canceled
        }
      }
  }

  def frameworkName(
    classPath: Seq[Path],
    parentInspector: AsmTestRunner.ParentInspector
  ): Either[NoTestFrameworkFoundError, String] = {
    val fwOpt = AsmTestRunner.findFrameworkService(classPath)
      .orElse {
        AsmTestRunner.findFramework(
          classPath,
          TestRunner.commonTestFrameworks,
          parentInspector
        )
      }
    fwOpt match {
      case Some(fw) => Right(fw.replace('/', '.').replace('\\', '.'))
      case None     => Left(new NoTestFrameworkFoundError)
    }
  }

  def testJs(
    classPath: Seq[Path],
    entrypoint: File,
    requireTests: Boolean,
    args: Seq[String],
    testFrameworkOpt: Option[String],
    logger: Logger,
    jsDom: Boolean,
    esModule: Boolean
  ): Either[TestError, Int] = either {
    import org.scalajs.jsenv.Input
    import org.scalajs.jsenv.nodejs.NodeJSEnv
    import org.scalajs.testing.adapter.TestAdapter
    val nodePath = findInPath("node").fold("node")(_.toString)
    val jsEnv =
      if (jsDom)
        new JSDOMNodeJSEnv(
          JSDOMNodeJSEnv.Config()
            .withExecutable(nodePath)
            .withArgs(Nil)
            .withEnv(Map.empty)
        )
      else new NodeJSEnv(
        NodeJSEnv.Config()
          .withExecutable(nodePath)
          .withArgs(Nil)
          .withEnv(Map.empty)
          .withSourceMap(NodeJSEnv.SourceMap.Disable)
      )
    val adapterConfig = TestAdapter.Config().withLogger(logger.scalaJsLogger)
    val inputs = Seq(
      if (esModule) Input.ESModule(entrypoint.toPath)
      else Input.Script(entrypoint.toPath)
    )
    var adapter: TestAdapter = null

    logger.debug(s"JS tests class path: $classPath")

    val parentInspector = new AsmTestRunner.ParentInspector(classPath)
    val frameworkName0 = testFrameworkOpt match {
      case Some(fw) => fw
      case None     => value(frameworkName(classPath, parentInspector))
    }

    val res =
      try {
        adapter = new TestAdapter(jsEnv, inputs, adapterConfig)

        val frameworks = adapter.loadFrameworks(List(List(frameworkName0))).flatten

        if (frameworks.isEmpty)
          Left(new NoFrameworkFoundByBridgeError)
        else if (frameworks.length > 1)
          Left(new TooManyFrameworksFoundByBridgeError)
        else {
          val framework = frameworks.head
          runTests(classPath, framework, requireTests, args, parentInspector)
        }
      }
      finally if (adapter != null) adapter.close()

    if (value(res)) 0
    else 1
  }

  def testNative(
    classPath: Seq[Path],
    launcher: File,
    frameworkNameOpt: Option[String],
    requireTests: Boolean,
    args: Seq[String],
    logger: Logger
  ): Either[TestError, Int] = either {

    import scala.scalanative.testinterface.adapter.TestAdapter

    logger.debug(s"Native tests class path: $classPath")

    val parentInspector = new AsmTestRunner.ParentInspector(classPath)
    val frameworkName0 = frameworkNameOpt match {
      case Some(fw) => fw
      case None     => value(frameworkName(classPath, parentInspector))
    }

    val config = TestAdapter.Config()
      .withBinaryFile(launcher)
      .withEnvVars(sys.env.toMap)
      .withLogger(logger.scalaNativeTestLogger)

    var adapter: TestAdapter = null

    val res =
      try {
        adapter = new TestAdapter(config)

        val frameworks = adapter.loadFrameworks(List(List(frameworkName0))).flatten

        if (frameworks.isEmpty)
          Left(new NoFrameworkFoundByBridgeError)
        else if (frameworks.length > 1)
          Left(new TooManyFrameworksFoundByBridgeError)
        else {
          val framework = frameworks.head
          runTests(classPath, framework, requireTests, args, parentInspector)
        }
      }
      finally if (adapter != null) adapter.close()

    if (value(res)) 0
    else 1
  }
}
