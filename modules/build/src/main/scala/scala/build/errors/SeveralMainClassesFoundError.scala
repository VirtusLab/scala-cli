package scala.build.errors

final class SeveralMainClassesFoundError(mainClasses: ::[String])
    extends MainClassError(s"Found several main classes: ${mainClasses.mkString(", ")}")
