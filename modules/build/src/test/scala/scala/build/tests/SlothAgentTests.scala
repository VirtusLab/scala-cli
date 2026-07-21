package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import scala.build.errors.SlothAgentError
import scala.build.internal.Constants
import scala.build.internal.util.WarningMessages
import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.build.postprocessing.SlothAgent

class SlothAgentTests extends TestUtil.ScalaCliBuildSuite:

  private val expectedAgentJarName =
    s"${Constants.slothAgentModuleName}-${Constants.slothAgentVersion}.jar"

  private def optionsWith(
    sloth: Boolean = false,
    slothAgent: Boolean = false
  ): BuildOptions =
    BuildOptions(notForBloopOptions =
      PostBuildOptions(
        slothOpt = Some(sloth).filter(identity),
        slothAgentOpt = Some(slothAgent).filter(identity)
      )
    )

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

  test("warnIfRedundantWithBatchPatching warns when both modes are enabled"):
    val logger = RecordingLogger()
    SlothAgent.warnIfRedundantWithBatchPatching(
      optionsWith(sloth = true, slothAgent = true),
      logger
    )
    expect(logger.messages.exists(_.contains(WarningMessages.slothModesMutuallyRedundant)))

  test("warnIfRedundantWithBatchPatching is silent when only one mode is enabled"):
    val logger = RecordingLogger()
    SlothAgent.warnIfRedundantWithBatchPatching(optionsWith(sloth = true), logger)
    SlothAgent.warnIfRedundantWithBatchPatching(optionsWith(slothAgent = true), logger)
    expect(logger.messages.isEmpty)
