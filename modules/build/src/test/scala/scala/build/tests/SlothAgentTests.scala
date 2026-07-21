package scala.build.tests

import scala.build.errors.SlothAgentError
import scala.build.internal.Constants
import scala.build.postprocessing.SlothAgent

class SlothAgentTests extends TestUtil.ScalaCliBuildSuite:

  private val expectedAgentJarName =
    s"${Constants.slothAgentModuleName}-${Constants.slothAgentVersion}.jar"

  test("selectAgentJar picks the agent jar, not a transitive dependency"):
    val decoyJar  = os.root / "cache" / "asm-9.10.1.jar"
    val agentJar  = os.root / "cache" / expectedAgentJarName
    val artifacts = Seq(
      ("https://repo1.maven.org/asm-9.10.1.jar", decoyJar),
      (s"https://repo1.maven.org/$expectedAgentJarName", agentJar)
    )

    val result = SlothAgent.selectAgentJar(artifacts)

    assert(result.isRight, s"Expected Right but got $result")
    assert(result.toOption.get == agentJar, s"Expected $agentJar but got ${result.toOption.get}")

  test("selectAgentJar returns error when agent jar not found"):
    val decoyJar  = os.root / "cache" / "asm-9.10.1.jar"
    val otherJar  = os.root / "cache" / "some-other-lib-1.0.jar"
    val artifacts = Seq(
      ("https://repo1.maven.org/asm-9.10.1.jar", decoyJar),
      ("https://repo1.maven.org/some-other-lib-1.0.jar", otherJar)
    )

    val result = SlothAgent.selectAgentJar(artifacts)

    result match
      case Left(_: SlothAgentError) => ()
      case other                    => fail(s"Expected Left(SlothAgentError) but got $other")
