package scala.cli

import coursier.jvm.Execve

import java.io.File
import java.nio.file.{Files, Path, Paths}

import scala.util.Properties

object Runner {

  def run(
    javaHome: String,
    classPath: Seq[File],
    mainClass: String,
    logger: Logger,
    allowExecve: Boolean = false
  ): Int = {

    import logger.{log, debug}

    val javaPath = Paths.get(javaHome).resolve("bin/java").toAbsolutePath
    val command = Seq(
      javaPath.toString,
      "-cp", classPath.iterator.map(_.getAbsolutePath).mkString(File.pathSeparator),
      mainClass
    )

    log(
      s"Running ${command.mkString(" ")}",
      "  Running" + System.lineSeparator() + command.iterator.map(_ + System.lineSeparator()).mkString
    )

    if (allowExecve && Execve.available()) {
      debug("execve available")
      Execve.execve(command.head, "java" +: command.tail.toArray, sys.env.toArray.sorted.map { case (k, v) => s"$k=$v" })
      sys.error("should not happen")
    } else
      new ProcessBuilder(command: _*)
        .inheritIO()
        .start()
        .waitFor()
  }

  private def findInPath(app: String): Option[Path] =
    if (Properties.isWin)
      None
    else
      Option(System.getenv("PATH"))
        .iterator
        .flatMap(_.split(File.pathSeparator).iterator)
        .map(Paths.get(_).resolve(app))
        .filter(Files.isExecutable(_))
        .toStream
        .headOption

  def runJs(
    // nodeHome: String, // ?
    entrypoint: File,
    logger: Logger,
    allowExecve: Boolean = false
  ): Int = {

    import logger.{log, debug}

    val nodePath = findInPath("node").fold("node")(_.toString)
    val command = Seq(nodePath, entrypoint.getAbsolutePath)

    log(
      s"Running ${command.mkString(" ")}",
      "  Running" + System.lineSeparator() + command.iterator.map(_ + System.lineSeparator()).mkString
    )

    if (allowExecve && Execve.available()) {
      debug("execve available")
      Execve.execve(command.head, "node" +: command.tail.toArray, sys.env.toArray.sorted.map { case (k, v) => s"$k=$v" })
      sys.error("should not happen")
    } else
      new ProcessBuilder(command: _*)
        .inheritIO()
        .start()
        .waitFor()
  }

  def runNative(
    launcher: File,
    logger: Logger,
    allowExecve: Boolean = false
  ): Int = {

    import logger.{log, debug}

    val command = Seq(launcher.getAbsolutePath)

    log(
      s"Running ${command.mkString(" ")}",
      "  Running" + System.lineSeparator() + command.iterator.map(_ + System.lineSeparator()).mkString
    )

    if (allowExecve && Execve.available()) {
      debug("execve available")
      Execve.execve(command.head, launcher.getName +: command.tail.toArray, sys.env.toArray.sorted.map { case (k, v) => s"$k=$v" })
      sys.error("should not happen")
    } else
      new ProcessBuilder(command: _*)
        .inheritIO()
        .start()
        .waitFor()
  }
}
