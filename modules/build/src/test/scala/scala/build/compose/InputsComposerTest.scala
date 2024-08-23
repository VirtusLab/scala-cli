package scala.build.compose

import scala.build.Build
import scala.build.bsp.buildtargets.ProjectName
import scala.build.compose.InputsComposer
import scala.build.errors.BuildException
import scala.build.input.Module
import scala.build.internal.Constants
import scala.build.options.BuildOptions
import scala.build.tests.{TestInputs, TestUtil}

class InputsComposerTest extends TestUtil.ScalaCliBuildSuite {

  test("read simple module config") {
    val configText =
      """[modules.webpage]
        |dependsOn = ["core"]
        |
        |[modules.core]
        |roots = ["Core.scala", "Utils.scala"]
        |""".stripMargin

    val parsedModules = {
      for {
        table   <- toml.Toml.parse(configText)
        modules <- InputsComposer.readAllModules(table.values.get(InputsComposer.Keys.modules))
      } yield modules
    }.toSeq.flatten

    assert(parsedModules.nonEmpty)

    assert(parsedModules.head.name == "webpage")
    val webpageModule = parsedModules.head
    assert(webpageModule.roots.toSet == Set("webpage"))
    assert(webpageModule.dependsOn.toSet == Set("core"))

    val coreModule = parsedModules.last
    assert(coreModule.name == "core")
    assert(coreModule.roots.toSet == Set("Core.scala", "Utils.scala"))
    assert(coreModule.dependsOn.isEmpty)
  }

  test("compose module inputs from module config") {
    val testInputs = TestInputs(
      os.rel / Constants.moduleConfigFileName ->
        """[modules.webpage]
          |dependsOn = ["core"]
          |
          |[modules.core]
          |roots = ["Core.scala", "Utils.scala"]
          |""".stripMargin
    )

    testInputs.fromRoot { root =>
      val argsToInputs = InputsComposerUtils.argsToEmptyModules
      val modules = InputsComposer(Seq(root.toString), root, argsToInputs, true)
        .getInputs
        .toSeq
        .head.modules

      assert(modules.nonEmpty)
      assert(
        modules.head.baseProjectName.startsWith("webpage"),
        clue = modules.head.baseProjectName
      )

      val websiteModule   = modules.head
      val coreModule      = modules.last
      val coreProjectName = coreModule.projectName

      assert(websiteModule.moduleDependencies.toSet == Set(coreProjectName))
    }
  }

  test("correctly create module build order") {
    val testInputs = TestInputs(
      os.rel / Constants.moduleConfigFileName ->
        """[modules.root1]
          |dependsOn = ["core"]
          |
          |[modules.core]
          |
          |[modules.utils]
          |
          |[modules.root2]
          |dependsOn = ["core", "utils"]
          |
          |[modules.uberRoot]
          |dependsOn = ["root1", "root2"]
          |""".stripMargin
    )

    testInputs.fromRoot { root =>
      val argsToInputs = InputsComposerUtils.argsToEmptyModules
      val maybeInputs = InputsComposer(Seq(root.toString), root, argsToInputs, true)
        .getInputs

      assert(maybeInputs.isRight, clue = maybeInputs)

      val inputs = maybeInputs.toOption.get

      val buildOrder = inputs.modulesBuildOrder

      def baseProjectName(projectName: ProjectName): String =
        projectName.name.take(projectName.name.indexOf("_"))

      assert(
        buildOrder.map(_.projectName).map(baseProjectName) == Seq(
          "utils",
          "core",
          "root1",
          "root2",
          "uberRoot"
        ),
        clue = buildOrder.map(_.projectName).map(baseProjectName)
      )
    }
  }
}
