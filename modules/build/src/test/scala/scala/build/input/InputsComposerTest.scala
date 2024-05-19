package scala.build.input

import scala.build.Build
import scala.build.errors.BuildException
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
        table <- toml.Toml.parse(configText)
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
      val argsToInputs: Seq[String] => Either[BuildException, ModuleInputs] = args => {
        val emptyInputs = ModuleInputs.empty(args.head)
        Right(Build.updateInputs(emptyInputs, BuildOptions()))
      }
      val modules = InputsComposer(Seq(root.toString), root, argsToInputs, true)
        .getModuleInputs
        .toSeq
        .flatten

      assert(modules.nonEmpty)
      assert(modules.head.baseProjectName.startsWith("webpage"), clue = modules.head.baseProjectName)

      val websiteModule = modules.head
      val coreModule = modules.last
      val coreProjectName = coreModule.projectName

      assert(websiteModule.moduleDependencies.toSet == Set(coreProjectName))
    }
  }
}
