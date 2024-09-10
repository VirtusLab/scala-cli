package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import coursier.cache.shaded.dirs.ProjectDirectories

import scala.util.Properties

class InstallAndUninstallCompletionsTests extends ScalaCliSuite {
  val zshRcFile: String  = ".zshrc"
  val bashRcFile: String = ".bashrc"
  val fishRcFile: String = "config.fish"
  val rcContent: String = s"""
                             |dummy line
                             |dummy line""".stripMargin
  val testInputs: TestInputs = TestInputs(
    os.rel / zshRcFile                       -> rcContent,
    os.rel / bashRcFile                      -> rcContent,
    os.rel / ".config" / "fish" / fishRcFile -> rcContent
  )

  def runInstallAndUninstallCompletions(): Unit = {
    testInputs.fromRoot { root =>
      val zshRcPath  = root / zshRcFile
      val bashRcPath = root / bashRcFile
      val fishRcPath = root / ".config" / "fish" / fishRcFile
      // install completions to the dummy rc files
      os.proc(TestUtil.cli, "install-completions", "--rc-file", zshRcPath, "--shell", "zsh").call(
        cwd = root
      )
      os.proc(TestUtil.cli, "install-completions", "--rc-file", bashRcPath, "--shell", "bash").call(
        cwd = root
      )
      os.proc(TestUtil.cli, "install-completions", "--rc-file", fishRcPath, "--shell", "fish").call(
        cwd = root
      )
      expect(os.read(bashRcPath).contains(bashRcScript))
      expect(os.read(zshRcPath).contains(zshRcScript))
      expect(os.read(fishRcPath).contains(fishRcScript))
      // uninstall completions from the dummy rc files
      os.proc(TestUtil.cli, "uninstall-completions", "--rc-file", zshRcPath).call(cwd = root)
      os.proc(TestUtil.cli, "uninstall-completions", "--rc-file", bashRcPath).call(cwd = root)
      os.proc(TestUtil.cli, "uninstall-completions", "--rc-file", fishRcPath).call(cwd = root)
      expect(os.read(zshRcPath) == rcContent)
      expect(os.read(bashRcPath) == rcContent)
      expect(os.read(fishRcPath) == rcContent)
    }
  }

  def isWinShell: Boolean = Option(System.getenv("OSTYPE")).nonEmpty
  if (!Properties.isWin || isWinShell)
    test("installing and uninstalling completions") {
      runInstallAndUninstallCompletions()
    }

  lazy val bashRcScript: String = {
    val progName = "scala-cli"
    val ifs      = "\\n"
    val script =
      s"""_${progName}_completions() {
         |  local IFS=$$'$ifs'
         |  eval "$$($progName complete bash-v1 "$$(( $$COMP_CWORD + 1 ))" "$${COMP_WORDS[@]}")"
         |}
         |
         |complete -F _${progName}_completions $progName
         |""".stripMargin
    addTags(script)
  }

  lazy val fishRcScript: String = {
    val progName = "scala-cli"
    val script =
      s"""complete $progName -a '($progName complete fish-v1 (math 1 + (count (__fish_print_cmd_args))) (__fish_print_cmd_args))'"""
    addTags(script)
  }

  lazy val zshRcScript: String = {
    val projDirs = ProjectDirectories.from(null, null, "ScalaCli")
    val dir      = os.Path(projDirs.dataLocalDir, TestUtil.pwd) / "completions" / "zsh"
    val script = Seq(
      s"""fpath=("$dir" $$fpath)""",
      "compinit"
    ).map(_ + System.lineSeparator()).mkString
    addTags(script)
  }

  def addTags(script: String): String = {
    val start    = "# >>> scala-cli completions >>>\n"
    val end      = "# <<< scala-cli completions <<<\n"
    val withTags = "\n" + start + script.stripSuffix("\n") + "\n" + end
    withTags
  }
}
