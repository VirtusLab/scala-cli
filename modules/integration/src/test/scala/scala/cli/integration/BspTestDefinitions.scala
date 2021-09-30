package scala.cli.integration

import ch.epfl.scala.{bsp4j => b}
import com.eed3si9n.expecty.Expecty.expect

import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Paths

import scala.annotation.tailrec
import scala.async.Async.{async, await}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.io.Codec
import scala.util.control.NonFatal
import scala.util.{Failure, Properties, Success, Try}

abstract class BspTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  import BspTestDefinitions._

  def initParams(root: os.Path): b.InitializeBuildParams =
    new b.InitializeBuildParams(
      "Scala CLI ITs",
      "0",
      Constants.bspVersion,
      root.toNIO.toUri.toASCIIString,
      new b.BuildClientCapabilities(List("java", "scala").asJava)
    )

  val pool      = TestUtil.threadPool("bsp-tests-jsonrpc", 4)
  val scheduler = TestUtil.scheduler("bsp-tests-scheduler")

  def completeIn(duration: FiniteDuration): Future[Unit] = {
    val p = Promise[Unit]()
    scheduler.schedule(
      new Runnable {
        def run(): Unit =
          try p.success(())
          catch {
            case t: Throwable =>
              System.err.println(s"Caught $t while trying to complete timer, ignoring it")
          }
      },
      duration.length,
      duration.unit
    )
    p.future
  }

  override def afterAll(): Unit = {
    pool.shutdown()
  }

  def withBsp[T](
    inputs: TestInputs,
    args: Seq[String],
    attempts: Int = 3,
    pauseDuration: FiniteDuration = 5.seconds
  )(
    f: (
      os.Path,
      TestBspClient,
      b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer
    ) => Future[T]
  ): T = {

    def attempt(): Try[T] = Try {
      val root = inputs.root()

      val proc = os.proc(TestUtil.cli, "bsp", extraOptions, args)
        .spawn(cwd = root)
      var remoteServer: b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer = null

      try {
        val (localClient, remoteServer0, _) =
          TestBspClient.connect(proc.stdout, proc.stdin, pool)
        remoteServer = remoteServer0
        Await.result(remoteServer.buildInitialize(initParams(root)).asScala, 3.minutes)
        Await.result(f(root, localClient, remoteServer), 3.minutes)
      }
      finally {
        if (remoteServer != null)
          try Await.result(remoteServer.buildShutdown().asScala, 20.seconds)
          catch {
            case NonFatal(e) =>
              System.err.println(s"Ignoring $e while shutting down BSP server")
          }
        proc.join(2.seconds.toMillis)
        proc.destroy()
        proc.join(2.seconds.toMillis)
        proc.destroyForcibly()
      }
    }

    @tailrec
    def helper(count: Int): T =
      attempt() match {
        case Success(t) => t
        case Failure(ex) =>
          if (count <= 1)
            throw new Exception(ex)
          else {
            System.err.println(s"Caught $ex, trying again in $pauseDuration…")
            Thread.sleep(pauseDuration.toMillis)
            helper(count - 1)
          }
      }

    helper(attempts)
  }

  def checkTargetUri(root: os.Path, uri: String): Unit = {
    val baseUri = TestUtil.normalizeUri((root / ".scala").toNIO.toUri.toASCIIString)
      .stripSuffix("/")
    val expectedPrefixes = Set(
      baseUri + "?id=",
      baseUri + "/?id="
    )
    expect(expectedPrefixes.exists(uri.startsWith))
  }

  test("setup-ide") {
    val inputs = TestInputs(
      Seq(
        os.rel / "simple.sc" ->
          s"""val msg = "Hello"
             |println(msg)
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "setup-ide", ".", extraOptions).call(cwd = root, stdout = os.Inherit)
      val bspFile = root / ".bsp" / "scala-cli.json"
      expect(os.isFile(bspFile))
      val json = ujson.read(
        os.read(bspFile: os.ReadablePath, charSet = Codec(Charset.defaultCharset()))
      )
      // check that we can decode the connection details
      val details = upickle.default.read(json)(detailsCodec)
      expect(details.argv.length >= 2)
      expect(details.argv(1) == "bsp")
    }
  }

  val importPprintOnlyProject = TestInputs(
    Seq(
      os.rel / "simple.sc" -> s"import $$ivy.`com.lihaoyi::pprint:${Constants.pprintVersion}`"
    )
  )

  test("setup-ide should succeed for valid dependencies") {
    importPprintOnlyProject.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "setup-ide",
        ".",
        extraOptions,
        "--dependency",
        s"org.scalameta::munit:${Constants.munitVersion}"
      ).call(cwd = root)
    }
  }

  test("setup-ide should fail for invalid dependencies") {
    importPprintOnlyProject.fromRoot { root =>

      val p = os.proc(
        TestUtil.cli,
        "setup-ide",
        ".",
        extraOptions,
        "--dependency",
        "org.scalameta::munit:0.7.119999"
      ).call(cwd = root, check = false, stderr = os.Pipe)
      expect(p.err.text().contains(s"Error downloading org.scalameta:munit"))
      expect(p.exitCode == 1)
    }
  }

  test("simple") {
    val inputs = TestInputs(
      Seq(
        os.rel / "simple.sc" ->
          s"""val msg = "Hello"
             |println(msg)
             |""".stripMargin
      )
    )

    withBsp(inputs, Seq(".")) { (root, _, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets().asScala.map(_.getId).toSeq
          expect(targets.length == 1)
          targets.head
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }
          if (actualScalaVersion.startsWith("2.")) {
            expect(foundDepSources.length == 1)
            expect(foundDepSources.forall(_.startsWith("scala-library-")))
          }
          else {
            expect(foundDepSources.length == 2)
            expect(foundDepSources.exists(_.startsWith("scala-library-")))
            expect(foundDepSources.exists(_.startsWith("scala3-library_3-3")))
          }
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
        }

        {
          val resp = await(remoteServer.buildTargetSources(new b.SourcesParams(targets)).asScala)
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundSources = resp.getItems.asScala
            .map(_.getSources.asScala.map(_.getUri).toSeq)
            .toSeq
            .map(_.map(TestUtil.normalizeUri))
          val expectedSources = Seq(
            Seq(
              TestUtil.normalizeUri((root / "simple.sc").toNIO.toUri.toASCIIString)
            )
          )
          expect(foundSources == expectedSources)
        }

        val scalacOptionsResp = {
          val resp = await {
            remoteServer
              .buildTargetScalacOptions(new b.ScalacOptionsParams(targets))
              .asScala
          }
          val foundTargets = resp
            .getItems
            .asScala
            .map(_.getTarget.getUri)
            .map(TestUtil.normalizeUri)
          expect(foundTargets == Seq(targetUri))
          val foundOptions = resp.getItems.asScala.flatMap(_.getOptions.asScala).toSeq
          if (actualScalaVersion.startsWith("2."))
            expect(foundOptions.exists { opt =>
              opt.startsWith("-Xplugin:") && opt.contains("semanticdb-scalac")
            })
          else
            expect(foundOptions.contains("-Xsemanticdb"))
          resp
        }

        {
          val resp = await {
            remoteServer.buildTargetJavacOptions(new b.JavacOptionsParams(targets)).asScala
          }
          val foundTargets = resp
            .getItems
            .asScala
            .map(_.getTarget.getUri)
            .map(TestUtil.normalizeUri)
          expect(foundTargets == Seq(targetUri))
        }

        val classDir = os.Path(
          Paths.get(new URI(scalacOptionsResp.getItems.asScala.head.getClassDirectory))
        )

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        val compileProducts = os.walk(classDir).filter(os.isFile(_)).map(_.relativeTo(classDir))

        expect(compileProducts.contains(os.rel / "simple.class"))
        expect(
          compileProducts.contains(os.rel / "META-INF" / "semanticdb" / "simple.sc.semanticdb")
        )
      }
    }
  }

  def runScalaCli(args: String*) = os.proc(TestUtil.cli, args)

  def testScalaTermination(
    currentBloopVersion: String,
    expectedBloopVersionAfterScalaCliRun: String
  ): Unit = TestUtil.retry() {
    def runBloop(args: String*) =
      os.proc(TestUtil.cs, "launch", s"bloop-jvm:$currentBloopVersion", "--", args)

    runBloop("exit").call()
    runBloop("about").call(stdout = os.Inherit, stderr = os.Inherit)
    runScalaCli("bloop", "start", "-v", "-v", "-v").call(
      stdout = os.Inherit,
      stderr = os.Inherit
    )
    val versionLine = runBloop("about").call().out.lines()(0)
    expect(versionLine == "bloop v" + expectedBloopVersionAfterScalaCliRun)
  }

  test("scala-cli terminates incompatible bloop") {
    testScalaTermination(Constants.oldBloopVersion, Constants.bloopVersion)
  }

  test("scala-cli keeps compatible bloop running") {
    testScalaTermination(Constants.newBloopVersion, Constants.newBloopVersion)
  }

  test("invalid bloop options passed via cli cause bloop start failure") {
    runScalaCli("bloop", "exit").call()
    val res = runScalaCli("bloop", "start", "--bloop-java-opt", "-Xmx1k").call(
      stderr = os.Pipe,
      check = false
    )
    expect(res.exitCode == 1)
    expect(res.err.text().contains("Server didn't start") || res.err.text().contains(
      "java.lang.OutOfMemoryError: Garbage-collected heap size exceeded"
    ))
  }

  test("invalid bloop options passed via global bloop config json file cause bloop start failure") {
    val inputs = TestInputs(
      Seq(
        os.rel / "bloop.json" ->
          """|{
             | "javaOptions" : ["-Xmx1k"]
             | }""".stripMargin
      )
    )

    inputs.fromRoot { root =>
      runScalaCli("bloop", "exit").call()
      val res = runScalaCli(
        "bloop",
        "start",
        "--bloop-global-options-file",
        (root / "bloop.json").toString()
      )
        .call(stderr = os.Pipe, check = false)
      expect(res.exitCode == 1)
      expect(res.err.text().contains("Server didn't start") || res.err.text().contains(
        "java.lang.OutOfMemoryError: Garbage-collected heap size exceeded"
      ))
    }
  }

  test("diagnostics") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Test.scala" ->
          s"""object Test {
             |  val msg = "Hello"
             |  zz
             |  println(msg)
             |}
             |""".stripMargin
      )
    )

    withBsp(inputs, Seq(".")) { (root, localClient, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets().asScala.map(_.getId).toSeq
          expect(targets.length == 1)
          targets.head
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        val compileResp = await {
          remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala
        }
        expect(compileResp.getStatusCode == b.StatusCode.ERROR)

        val diagnosticsParams = {
          val params = localClient.latestDiagnostics().getOrElse {
            sys.error("No diagnostics found")
          }
          expect(params.getBuildTarget.getUri == targetUri)
          expect(
            TestUtil.normalizeUri(params.getTextDocument.getUri) ==
              TestUtil.normalizeUri((root / "Test.scala").toNIO.toUri.toASCIIString)
          )
          params
        }

        val diagnostics = diagnosticsParams.getDiagnostics.asScala.toSeq
        expect(diagnostics.length == 1)

        val diag = diagnostics.head

        expect(diag.getSeverity == b.DiagnosticSeverity.ERROR)
        expect(diag.getRange.getStart.getLine == 2)
        expect(diag.getRange.getStart.getCharacter == 2)
        expect(diag.getRange.getEnd.getLine == 2)
        if (actualScalaVersion.startsWith("2.")) {
          expect(diag.getMessage == "not found: value zz")
          expect(diag.getRange.getEnd.getCharacter == 4)
        }
        else if (actualScalaVersion == "3.0.0") {
          expect(diag.getMessage == "Not found: zz")
          expect(diag.getRange.getEnd.getCharacter == 2)
        }
        else {
          expect(diag.getMessage == "Not found: zz")
          expect(diag.getRange.getEnd.getCharacter == 4)
        }
      }
    }
  }

  test("diagnostics in script") {
    val inputs = TestInputs(
      Seq(
        os.rel / "test.sc" ->
          s"""val msg = "Hello"
             |zz
             |println(msg)
             |""".stripMargin
      )
    )

    withBsp(inputs, Seq(".")) { (root, localClient, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets().asScala.map(_.getId).toSeq
          expect(targets.length == 1)
          targets.head
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        val compileResp = await {
          remoteServer
            .buildTargetCompile(new b.CompileParams(targets))
            .asScala
        }
        expect(compileResp.getStatusCode == b.StatusCode.ERROR)

        val diagnosticsParams = {
          val params = localClient.latestDiagnostics().getOrElse {
            sys.error("No diagnostics found")
          }
          expect(params.getBuildTarget.getUri == targetUri)
          expect(
            TestUtil.normalizeUri(params.getTextDocument.getUri) ==
              TestUtil.normalizeUri((root / "test.sc").toNIO.toUri.toASCIIString)
          )
          params
        }

        val diagnostics = diagnosticsParams.getDiagnostics.asScala.toSeq
        expect(diagnostics.length == 1)

        val diag = diagnostics.head

        expect(diag.getSeverity == b.DiagnosticSeverity.ERROR)
        expect(diag.getRange.getStart.getLine == 1)
        expect(diag.getRange.getStart.getCharacter == 0)
        expect(diag.getRange.getEnd.getLine == 1)
        if (actualScalaVersion.startsWith("2.")) {
          expect(diag.getMessage == "not found: value zz")
          expect(diag.getRange.getEnd.getCharacter == 2)
        }
        else if (actualScalaVersion == "3.0.0") {
          expect(diag.getMessage == "Not found: zz")
          expect(diag.getRange.getEnd.getCharacter == 0)
        }
        else {
          expect(diag.getMessage == "Not found: zz")
          expect(diag.getRange.getEnd.getCharacter == 2)
        }
      }
    }
  }

  test("workspace update") {
    val inputs = TestInputs(
      Seq(
        os.rel / "simple.sc" ->
          s"""val msg = "Hello"
             |println(msg)
             |""".stripMargin
      )
    )
    val extraArgs =
      if (Properties.isWin) Seq("-v", "-v", "-v")
      else Nil

    withBsp(inputs, Seq(".") ++ extraArgs) { (root, localClient, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets().asScala.map(_.getId).toSeq
          expect(targets.length == 1)
          targets.head
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }
          if (actualScalaVersion.startsWith("2.")) {
            expect(foundDepSources.length == 1)
            expect(foundDepSources.forall(_.startsWith("scala-library-")))
          }
          else {
            expect(foundDepSources.length == 2)
            expect(foundDepSources.exists(_.startsWith("scala-library-")))
            expect(foundDepSources.exists(_.startsWith("scala3-library_3-3")))
          }
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
        }

        val didChangeParamsFuture = localClient.buildTargetDidChange()
        val updatedContent =
          """using "com.lihaoyi::pprint:0.6.6"
            |val msg = "Hello"
            |pprint.log(msg)
            |""".stripMargin
        os.write.over(root / "simple.sc", updatedContent)

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        val didChangeParamsOptFuture = Future.firstCompletedOf(Seq(
          didChangeParamsFuture.map(Some(_)),
          completeIn(5.seconds).map(_ => None)
        ))
        val didChangeParams = await(didChangeParamsOptFuture).getOrElse {
          sys.error("No buildTargetDidChange notification received")
        }

        val changes = didChangeParams.getChanges.asScala.toSeq
        expect(changes.length == 1)

        val change = changes.head
        expect(change.getTarget.getUri == targetUri)
        expect(change.getKind == b.BuildTargetEventKind.CHANGED)

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }
          expect(foundDepSources.length > 1)
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
          expect(foundDepSources.exists(_.startsWith("scala-library-")))
          expect(foundDepSources.exists(_.startsWith("pprint_")))
          resp
        }
      }
    }
  }

  test("workspace update - new file") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Test.scala" ->
          s"""object Test {
             |  val msg = "Hello"
             |  println(msg)
             |}
             |""".stripMargin
      )
    )

    withBsp(inputs, Seq(".")) { (root, localClient, remoteServer) =>
      async {
        val buildTargetsResp = await(remoteServer.workspaceBuildTargets().asScala)
        val target = {
          val targets = buildTargetsResp.getTargets().asScala.map(_.getId).toSeq
          expect(targets.length == 1)
          targets.head
        }

        val targetUri = TestUtil.normalizeUri(target.getUri)
        checkTargetUri(root, targetUri)

        val targets = List(target).asJava

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        {
          val resp = await {
            remoteServer
              .buildTargetDependencySources(new b.DependencySourcesParams(targets))
              .asScala
          }
          val foundTargets = resp.getItems.asScala.map(_.getTarget.getUri).toSeq
          expect(foundTargets == Seq(targetUri))
          val foundDepSources = resp.getItems.asScala
            .flatMap(_.getSources.asScala)
            .toSeq
            .map { uri =>
              val idx = uri.lastIndexOf('/')
              uri.drop(idx + 1)
            }
          if (actualScalaVersion.startsWith("2.")) {
            expect(foundDepSources.length == 1)
            expect(foundDepSources.forall(_.startsWith("scala-library-")))
          }
          else {
            expect(foundDepSources.length == 2)
            expect(foundDepSources.exists(_.startsWith("scala-library-")))
            expect(foundDepSources.exists(_.startsWith("scala3-library_3-3")))
          }
          expect(foundDepSources.forall(_.endsWith("-sources.jar")))
        }

        val didChangeParamsFuture = localClient.buildTargetDidChange()

        val newFileContent =
          """object Messages {
            |  def msg = "Hello"
            |}
            |""".stripMargin
        os.write(root / "Messages.scala", newFileContent)
        val updatedContent =
          """object Test {
            |  println(Messages.msg)
            |}
            |""".stripMargin
        os.write.over(root / "Test.scala", updatedContent)

        {
          val resp = await(remoteServer.buildTargetCompile(new b.CompileParams(targets)).asScala)
          expect(resp.getStatusCode == b.StatusCode.OK)
        }

        val didChangeParamsOptFuture = Future.firstCompletedOf(Seq(
          didChangeParamsFuture.map(Some(_)),
          completeIn(5.seconds).map(_ => None)
        ))
        val didChangeParams = await(didChangeParamsOptFuture).getOrElse {
          sys.error("No buildTargetDidChange notification received")
        }

        val changes = didChangeParams.getChanges.asScala.toSeq
        expect(changes.length == 1)

        val change = changes.head
        expect(change.getTarget.getUri == targetUri)
        expect(change.getKind == b.BuildTargetEventKind.CHANGED)
      }
    }
  }
}

object BspTestDefinitions {

  private final case class Details(
    name: String,
    version: String,
    bspVersion: String,
    argv: List[String],
    languages: List[String]
  )
  private val detailsCodec = upickle.default.macroRW[Details]

}
