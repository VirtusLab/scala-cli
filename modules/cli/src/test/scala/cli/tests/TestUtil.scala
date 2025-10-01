package cli.tests

import coursier.cache.{ArtifactError, FileCache}
import coursier.util.{Artifact, Task}

import java.io.File
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.control.NonFatal

object TestUtil {
  abstract class ScalaCliSuite extends munit.FunSuite {
    extension (munitContext: BeforeEach | AfterEach) {
      def locationAbsolutePath: os.Path =
        os.Path {
          (munitContext match {
            case beforeEach: BeforeEach => beforeEach.test
            case afterEach: AfterEach   => afterEach.test
          }).location.path
        }
    }

    override def munitTimeout = new FiniteDuration(120, TimeUnit.SECONDS)

    val testStartEndLogger = new Fixture[Unit]("files") {
      def apply(): Unit = ()

      override def beforeEach(context: BeforeEach): Unit = {
        val fileName = context.locationAbsolutePath.baseName
        System.err.println(
          s">==== ${Console.CYAN}Running '${context.test.name}' from $fileName${Console.RESET}"
        )
      }

      override def afterEach(context: AfterEach): Unit = {
        val fileName = context.locationAbsolutePath.baseName
        System.err.println(
          s"X==== ${Console.CYAN}Finishing '${context.test.name}' from $fileName${Console.RESET}"
        )
      }
    }

    override def munitFixtures = List(testStartEndLogger)
  }

  def downloadFile(url: String): Either[ArtifactError, Array[Byte]] = {
    val artifact = Artifact(url).withChanging(true)
    val cache    = FileCache()

    val file: Either[ArtifactError, File] = cache.logger.use {
      try cache.withTtl(0.seconds).file(artifact).run.unsafeRun()(using cache.ec)
      catch {
        case NonFatal(e) => throw new Exception(e)
      }
    }

    file.map(f => os.read.bytes(os.Path(f, os.pwd)))
  }
  val isCI: Boolean = System.getenv("CI") != null
}
