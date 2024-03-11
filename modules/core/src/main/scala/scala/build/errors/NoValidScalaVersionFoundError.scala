package scala.build.errors

final class NoValidScalaVersionFoundError(val versionString: String = "")
    extends ScalaVersionError({
      val suffix = if versionString.nonEmpty then s" for $versionString" else ""
      s"Cannot find a valid matching Scala version$suffix."
    })
