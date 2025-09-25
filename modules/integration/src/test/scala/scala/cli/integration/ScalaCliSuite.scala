package scala.cli.integration

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{Duration, FiniteDuration}

abstract class ScalaCliSuite extends munit.FunSuite {
  implicit class BeforeEachOpts(munitContext: BeforeEach) {
    def locationAbsolutePath: os.Path = os.Path(munitContext.test.location.path)
  }

  implicit class AfterEachOpts(munitContext: AfterEach) {
    def locationAbsolutePath: os.Path = os.Path(munitContext.test.location.path)
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

  override def munitTimeout: Duration = new FiniteDuration(300, TimeUnit.SECONDS)

  override def munitFixtures: List[Fixture[Unit]] = List(testStartEndLogger)
  def group: ScalaCliSuite.TestGroup              = ScalaCliSuite.TestGroup.Third

  override def munitIgnore: Boolean =
    Option(System.getenv("SCALA_CLI_IT_GROUP"))
      .flatMap(_.toIntOption)
      .exists(_ != group.idx)

  override def munitFlakyOK: Boolean = TestUtil.isCI
}

object ScalaCliSuite {
  sealed abstract class TestGroup(val idx: Int) extends Product with Serializable
  object TestGroup {
    case object First  extends TestGroup(1) // Scala 3 Next / default
    case object Second extends TestGroup(2) // Scala 2.13
    case object Third  extends TestGroup(3) // Scala 2.12
    case object Fourth extends TestGroup(4) // Scala 3.3 LTS
    case object Fifth  extends TestGroup(5) // Scala 3 Next RC
  }
}
