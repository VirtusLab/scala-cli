package scala.build.errors

import scala.build.Position

final class JvmDownloadError(jvmId: String, cause: Throwable)
    extends BuildException(
      s"Cannot download JVM: $jvmId",
      cause = cause
    )
