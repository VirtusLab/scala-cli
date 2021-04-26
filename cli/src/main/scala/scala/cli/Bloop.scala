package scala.cli

import bloop.bloopgun

import java.io.{ByteArrayInputStream, InputStream, PrintStream}
import java.nio.file.Path

import scala.cli.internal.Constants

object Bloop {

  private def emptyInputStream: InputStream =
    new ByteArrayInputStream(Array.emptyByteArray)

  def compile(
    workspace: Path,
    projectName: String,
    logger: Logger,
    bloopVersion: String = Constants.bloopVersion,
    stdout: PrintStream = System.out,
    stderr: PrintStream = System.err
  ): Path = {
    val bloopgun0 = new bloopgun.BloopgunCli(
      bloopVersion,
      emptyInputStream,
      stdout,
      stderr,
      bloopgun.core.Shell.default,
      workspace.resolve(".scala")
    )

    logger.debug("Running bloop compile")
    val compileRet = bloopgun0.run(Array("compile", projectName))
    if (compileRet != 0)
      sys.error(s"'bloop compile' failed ($compileRet)")
    logger.debug("bloop compile done")

    workspace.resolve(s".scala/.bloop/$projectName/bloop-bsp-clients-classes/classes-bloop-cli")
  }

}
