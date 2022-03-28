package scala.cli.internal

import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.{CancellationException, CompletableFuture, CompletionException}

import scala.build.Logger
import scala.util.Properties
import scala.util.control.NonFatal

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
      os.write.over(file, updatedContent)
    }

    usesSh
  }

  def downloadFile(url: String): String = {
    var inputStream: InputStream = null
    val data =
      try {
        inputStream = new URL(url).openStream()
        inputStream.readAllBytes()
      }
      finally if (inputStream != null)
          inputStream.close()
    new String(data, StandardCharsets.UTF_8)
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

}
