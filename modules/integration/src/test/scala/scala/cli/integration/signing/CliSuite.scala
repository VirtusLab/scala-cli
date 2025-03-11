package scala.cli.signing.integration

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

class CliSuite extends munit.FunSuite {
  implicit class BeforeEachOpts(munitContext: BeforeEach) {
    def locationAbsolutePath: os.Path = os.pwd / os.RelPath(munitContext.test.location.path)
  }

  implicit class AfterEachOpts(munitContext: AfterEach) {
    def locationAbsolutePath: os.Path = os.pwd / os.RelPath(munitContext.test.location.path)
  }
  val testStartEndLogger: Fixture[Unit] = new Fixture[Unit]("files") {
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

  override def munitFlakyOK: Boolean = TestUtil.isCI

  override def munitTimeout: Duration = new FiniteDuration(300, TimeUnit.SECONDS)

  override def munitFixtures: List[Fixture[Unit]] = List(testStartEndLogger)

}
