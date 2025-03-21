package scala.build.errors

import scala.build.Position

final class UnnamedKeyError(val key: String)
    extends BuildException(s"Expected key $key to be named")
