package scala.cli.internal

import java.net.{HttpURLConnection, URL, URLConnection}
import java.nio.charset.StandardCharsets
import java.util.concurrent.{CancellationException, CompletableFuture, CompletionException}

import scala.build.Logger
import scala.util.control.NonFatal
import scala.util.{Properties, Try}

object ProcUtil {

  def maybeUpdatePreamble(file: os.Path): Boolean = {
    val header = os.read.bytes(file, offset = 0, count = "#!/usr/bin/env sh".length).toSeq
    val hasBinEnvShHeader =
      header.startsWith("#!/usr/bin/env sh".getBytes(StandardCharsets.UTF_8))
    val hasBinShHeader =
      header.startsWith("#!/bin/sh".getBytes(StandardCharsets.UTF_8))
    val usesSh = hasBinEnvShHeader || hasBinShHeader

    if (usesSh) {
      val content = os.read.bytes(file)
      val updatedContent =
        if (hasBinEnvShHeader)
          "#!/usr/bin/env bash".getBytes(StandardCharsets.UTF_8) ++
            content.drop("#!/usr/bin/env sh".length)
        else if (hasBinShHeader)
          "#!/bin/bash".getBytes(StandardCharsets.UTF_8) ++
            content.drop("#!/bin/sh".length)
        else
          sys.error("Can't happen")
      os.write.over(file, updatedContent, createFolders = true)
    }

    usesSh
  }

  def forceKillProcess(process: Process, logger: Logger): Unit = {
    if (process.isAlive) {
      process.destroyForcibly()
      logger.debug(s"Killing user process ${process.pid()}")
    }
  }

  def interruptProcess(process: Process, logger: Logger): Unit = {
    val pid = process.pid()
    try
      if (process.isAlive) {
        logger.debug("Interrupting running process")
        if (Properties.isWin) {
          os.proc("taskkill", "/PID", pid).call()
          logger.debug(s"Run following command to interrupt process: 'taskkill /PID $pid'")
        }
        else {
          os.proc("kill", "-2", pid).call()
          logger.debug(s"Run following command to interrupt process: 'kill -2 $pid'")
        }
      }
    catch { // ignore the failure if the process isn't running, might mean it exited between the first check and the call of the command to kill it
      case NonFatal(e) =>
        logger.debug(s"Ignoring error during interrupt process: $e")
    }
  }

  def waitForProcess(process: Process, onExit: CompletableFuture[_]): Unit = {
    process.waitFor()
    try onExit.join()
    catch {
      case _: CancellationException | _: CompletionException => // ignored
    }
  }

  def findApplicationPathsOnPATH(appName: String): List[String] = {
    import java.io.File.pathSeparator, java.io.File.pathSeparatorChar

    var path = System.getenv("PATH")
    val pwd  = os.pwd.toString

    // on unix & macs, an empty PATH counts as ".", the working directory
    if (path.length == 0)
      path = pwd
    else {
      // scala 'split' doesn't handle leading or trailing pathSeparators
      // correctly so expand them now.
      if (path.head == pathSeparatorChar) path = pwd + path
      if (path.last == pathSeparatorChar) path = path + pwd
      // on unix and macs, an empty PATH item is like "." (current dir).
      path = s"$pathSeparator$pathSeparator".r
        .replaceAllIn(path, pathSeparator + pwd + pathSeparator)
    }

    val appPaths = path
      .split(pathSeparator)
      .map(d => if (d == ".") pwd else d) // on unix a bare "." counts as the current dir
      .map(_ + s"/$appName")
      .filter(f => os.isFile(os.Path(f, os.pwd)))
      .toSet

    appPaths.toList
  }

  // Copied from https://github.com/scalacenter/bloop/blob/a249e0a710ce169ca05d0606778f96f44a398680/shared/src/main/scala/bloop/io/Environment.scala
  private lazy val shebangCapableShells = Seq(
    "/bin/sh",
    "/bin/ash",
    "/bin/bash",
    "/bin/dash",
    "/bin/mksh",
    "/bin/pdksh",
    "/bin/posh",
    "/bin/tcsh",
    "/bin/zsh",
    "/bin/fish"
  )

  def isShebangCapableShell = Option(System.getenv("SHELL")) match
    case Some(currentShell) if shebangCapableShells.exists(sh => currentShell.contains(sh)) => true
    case _                                                                                  => false
}
