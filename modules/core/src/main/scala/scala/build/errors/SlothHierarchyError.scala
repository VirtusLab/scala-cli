package scala.build.errors

/** Raised when `--sloth-strict` / `//> using slothStrict` is set and Sloth cannot resolve a type
  * needed to recompute stack map frames in a dependency jar.
  */
final class SlothHierarchyError(message: String, cause: Throwable = null)
    extends BuildException(message, cause = cause)
