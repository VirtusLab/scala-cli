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
      os.write.over(file, updatedContent)
    }

    usesSh
  }

  // from https://github.com/coursier/coursier/blob/7b7c2c312aea26e850f0cd2cf15e688d0777f819/modules/cache/jvm/src/main/scala/coursier/cache/CacheUrl.scala#L489-L497
  private def closeConn(conn: URLConnection): Unit = {
    Try(conn.getInputStream).toOption.filter(_ != null).foreach(_.close())
    conn match {
      case conn0: HttpURLConnection =>
        Try(conn0.getErrorStream).toOption.filter(_ != null).foreach(_.close())
        conn0.disconnect()
      case _ =>
    }
  }

  def download(
    url: String,
    headers: (String, String)*
  ): Array[Byte] = {
    var conn: URLConnection = null
    val url0                = new URL(url)
    try {
      conn = url0.openConnection()
      for ((k, v) <- headers)
        conn.setRequestProperty(k, v)
      conn.getInputStream.readAllBytes()
    }
    finally
      if (conn != null)
        closeConn(conn)
  }

  def downloadFile(url: String): String = {
    val data = download(url)
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

}
