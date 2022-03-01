package scala.cli.internal

import java.io.InputStream
import java.net.URL
import java.nio.charset.StandardCharsets

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

}
