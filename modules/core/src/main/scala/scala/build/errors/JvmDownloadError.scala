package scala.build.errors

final class JvmDownloadError(jvmId: String, cause: Throwable)
    extends BuildException(
      s"Cannot download JVM: $jvmId",
      cause = cause
    )
