package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

final case class PackageOptions(
  @Recurse
    shared: SharedOptions,
  @Name("o")
    output: Option[String] = None,
  @Name("f")
    force: Boolean = false,
  includeDependencies: Boolean = true,
  library: Boolean = false,
  @Name("M")
    mainClass: Option[String] = None
) {
  import PackageOptions.PackageType
  def packageType: PackageType =
    if (library) PackageType.LibraryJar
    else if (shared.js) PackageType.Js
    else if (shared.native) PackageType.Native
    else PackageType.Bootstrap
}

object PackageOptions {

  sealed abstract class PackageType extends Product with Serializable
  object PackageType {
    case object Bootstrap extends PackageType
    case object LibraryJar extends PackageType
    case object Js extends PackageType
    case object Native extends PackageType
  }

  implicit val parser = Parser[PackageOptions]
  implicit val help = Help[PackageOptions]
}
