package scala.cli.integration

import java.util.concurrent.TimeUnit

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Properties

abstract class ScalaCliSuite extends munit.FunSuite {
  given scalaCliSuite: ScalaCliSuite = this

  private val resourceTracker = new ResourceTracker

  private[integration] def trackSubprocess[P <: os.SubProcess](proc: P): P =
    resourceTracker.trackSubprocess(proc)

  private[integration] def trackThread[T <: Thread](thread: T): T =
    resourceTracker.trackThread(thread)

  private[integration] def trackFuture[F <: Future[?]](future: F): F =
    resourceTracker.trackFuture(future)

  implicit class BeforeEachOpts(munitContext: BeforeEach) {
    def locationAbsolutePath: os.Path = os.Path(munitContext.test.location.path)
  }

  implicit class AfterEachOpts(munitContext: AfterEach) {
    def locationAbsolutePath: os.Path = os.Path(munitContext.test.location.path)
  }
  val testStartEndLogger: Fixture[Unit] = new Fixture[Unit]("files") {
    def apply(): Unit = ()

    override def beforeEach(context: BeforeEach): Unit = {
      resourceTracker.clear()
      val fileName = context.locationAbsolutePath.baseName
      System.err.println(
        s">==== ${Console.CYAN}Running '${context.test.name}' from $fileName${Console.RESET}"
      )
    }

    override def afterEach(context: AfterEach): Unit = {
      resourceTracker.drain()
      System.out.flush()
      System.err.flush()
      val fileName = context.locationAbsolutePath.baseName
      System.err.println(
        s"X==== ${Console.CYAN}Finishing '${context.test.name}' from $fileName${Console.RESET}"
      )
    }

    override def afterAll(): Unit = {
      super.afterAll()
      // Clean up cached JDKs after all tests have run on Linux native CI runners
      if isCI && Properties.isLinux then TestUtil.cleanCachedJdks()
      else System.err.println("Skipping cached JDKs cleanup")
    }
  }

  override def munitTimeout: Duration = new FiniteDuration(300, TimeUnit.SECONDS)

  override def munitFixtures: List[Fixture[Unit]] = List(testStartEndLogger)
  def group: ScalaCliSuite.TestGroup              = ScalaCliSuite.TestGroup.Third

  override def munitIgnore: Boolean =
    Option(System.getenv("SCALA_CLI_IT_GROUP"))
      .flatMap(_.toIntOption)
      .exists(_ != group.idx)

  private def sanityOnly: Boolean =
    Option(System.getenv("SCALA_CLI_IT_SANITY")).exists(_.equalsIgnoreCase("true"))

  override def munitTests(): Seq[Test] =
    val all = super.munitTests()
    if sanityOnly then all.filter(_.tags.contains(ScalaCliSuite.sanity)) else all

  override def munitFlakyOK: Boolean = TestUtil.isCI

  def exitBloop: os.CommandResult = os.proc(TestUtil.cli, "--power", "bloop", "exit").call()

  val cleanBloopFixture =
    FunFixture[os.CommandResult](setup = _ => exitBloop, teardown = _ => exitBloop)
}

object ScalaCliSuite {
  val sanity: munit.Tag = new munit.Tag("sanity")

  sealed abstract class TestGroup(val idx: Int) extends Product with Serializable
  object TestGroup {
    case object First  extends TestGroup(1) // Scala 3 Next / default
    case object Second extends TestGroup(2) // Scala 2.13
    case object Third  extends TestGroup(3) // Scala 2.12
    case object Fourth extends TestGroup(4) // Scala 3.3 LTS
    case object Fifth  extends TestGroup(5) // Scala 3 Next RC
  }
}
