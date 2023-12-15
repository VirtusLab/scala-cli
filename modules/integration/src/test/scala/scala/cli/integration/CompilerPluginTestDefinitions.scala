package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.util.CompilerPluginUtil

trait CompilerPluginTestDefinitions { _: CompileTestDefinitions =>
  def compilerPluginInputs(pluginName: String, pluginErrorMsg: String): TestInputs =
    if (actualScalaVersion.startsWith("3"))
      CompilerPluginUtil.compilerPluginForScala3(pluginName, pluginErrorMsg)
    else CompilerPluginUtil.compilerPluginForScala2(pluginName, pluginErrorMsg)

  test("build a custom compiler plugin and use it") {
    val pluginName       = "divbyzero"
    val usePluginFile    = "Main.scala"
    val outputJar        = "div-by-zero.jar"
    val pluginErrorMsg   = "definitely division by zero"
    compilerPluginInputs(pluginName, pluginErrorMsg)
      .add(os.rel / usePluginFile ->
        s"""//> using option -Xplugin:$outputJar
           |
           |object Test {
           |  val five = 5
           |  val amount = five / 0
           |  def main(args: Array[String]): Unit = {
           |    println(amount)
           |  }
           |}
           |""".stripMargin)
      .fromRoot { root =>
        // build the compiler plugin
        os.proc(
          TestUtil.cli,
          "package",
          s"$pluginName.scala",
          "--power",
          "--with-compiler",
          "--library",
          "-o",
          outputJar,
          extraOptions
        ).call(cwd = root)
        expect(os.isFile(root / outputJar))

        // verify the plugin is loaded
        val pluginListResult = os.proc(
          TestUtil.cli,
          "compile",
          s"-Xplugin:$outputJar",
          "-Xplugin-list",
          extraOptions
        ).call(cwd = root, mergeErrIntoOut = true)
        expect(pluginListResult.out.text().contains(pluginName))

        // verify the compiler plugin phase is being added correctly
        os.proc(
          TestUtil.cli,
          "compile",
          s"-Xplugin:$outputJar",
          "-Xshow-phases",
          extraOptions
        ).call(cwd = root, mergeErrIntoOut = true)
        expect(pluginListResult.out.text().contains(pluginName))

        // verify the compiler plugin is working
        // TODO: this shouldn't require running with --server=false
        val res = os.proc(TestUtil.cli, "compile", usePluginFile, "--server=false", extraOptions)
          .call(cwd = root, mergeErrIntoOut = true, check = false)
        expect(res.exitCode == 1)
        expect(res.out.text().contains(pluginErrorMsg))
      }
  }
}
