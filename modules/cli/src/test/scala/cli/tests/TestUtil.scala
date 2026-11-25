package cli.tests

import munit.AnyFixture

import java.io.IOException
import java.net.{HttpURLConnection, URI}
import java.util.concurrent.TimeUnit

import scala.annotation.tailrec
import scala.concurrent.duration.{DurationInt, FiniteDuration}

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

    val testStartEndLogger: Fixture[Unit] = new Fixture[Unit]("files") {
      def apply(): Unit = ()

      override def beforeEach(context: BeforeEach): Unit = {
        val fileName = context.locationAbsolutePath.baseName
        System.err.println(
          s">==== ${scala.build.tests.TestUtil.timestamp} ${Console.CYAN}Running '${context.test.name}' from $fileName${Console.RESET}"
        )
      }

      override def afterEach(context: AfterEach): Unit = {
        val fileName = context.locationAbsolutePath.baseName
        System.err.println(
          s"X==== ${scala.build.tests.TestUtil.timestamp} ${Console.CYAN}Finishing '${context.test.name}' from $fileName${Console.RESET}"
        )
      }
    }

    override def munitFixtures: Seq[AnyFixture[?]] = List(testStartEndLogger)
  }

  /** Whether `url` exists, checked with a HEAD request (following redirects).
    *
    * Retries on unexpected statuses (rate limiting, server errors, …) and I/O errors, and throws if
    * they persist, so that those aren't mistaken for a missing file.
    */
  @tailrec
  def remoteFileExists(url: String, attempts: Int = 3): Boolean =
    val conn = URI.create(url).toURL.openConnection().asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("HEAD")
    conn.setConnectTimeout(10.seconds.toMillis.toInt)
    conn.setReadTimeout(30.seconds.toMillis.toInt)
    val status =
      try Right(conn.getResponseCode)
      catch case e: IOException => Left(e)
      finally conn.disconnect()
    status match
      case Right(code) if code >= 200 && code < 300 => true
      case Right(HttpURLConnection.HTTP_NOT_FOUND)  => false
      case _ if attempts > 1                        =>
        Thread.sleep(5.seconds.toMillis)
        remoteFileExists(url, attempts - 1)
      case Right(code) => throw new Exception(s"Unexpected HTTP status $code for $url")
      case Left(e)     => throw new Exception(s"Error checking $url", e)

  val isCI: Boolean = System.getenv("CI") != null
}
