package scala.build.errors

final class FileNotFoundException(val path: os.Path)
    extends BuildException(s"File not found: $path")
