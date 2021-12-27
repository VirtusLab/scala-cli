package scala.build.errors

final class NoResourcePathFoundError(path: os.Path) extends BuildException(
      s"Provided resource directory path doesn't exist: $path"
    )
