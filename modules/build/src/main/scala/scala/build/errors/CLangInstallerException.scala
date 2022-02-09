package scala.build.errors

final class CLangInstallException(message: String)
    extends BuildException(s"Failed to Install CLang: $message")
