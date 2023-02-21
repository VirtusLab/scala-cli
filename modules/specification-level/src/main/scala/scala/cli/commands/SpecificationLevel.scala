package scala.cli.commands

sealed trait SpecificationLevel extends Product with Serializable {
  def md: String = toString + " have"
}

/** Specification levels in the context of Scala CLI runner specification. For more refer to
  * [SIP-46](https://github.com/scala/improvement-proposals/pull/46)
  *
  * Levels are also used to mark if given option, directive or command is part of stable API.
  */
object SpecificationLevel {

  /** Marks option, directive or command that MUST be a part of any Scala Runner Specification (in
    * RFC meaning). Annotated thing will be included in a new `scala` command.
    *
    * This also means that that thing should be sable and we need to support it.
    */
  case object MUST extends SpecificationLevel

  /** Marks option, directive or command that SHOULD be a part of any Scala Runner Specification (in
    * RFC meaning). Annotated thing will be included in a new `scala` command.
    *
    * This also means that that thing should be sable and we need to support it.
    */
  case object SHOULD extends SpecificationLevel

  /** Marks option, directive or command that is an implementation details of Scala CLI and will not
    * be a part of any Scala Runner Specification. Annotated thing will be included in a new `scala`
    * command.
    *
    * This also means that that thing should be sable and we need to support it.
    */
  case object IMPLEMENTATION extends SpecificationLevel {
    override def md: String = toString + " specific"
  }

  /** Annotated option, directive or command will not be a part of the Scala Runner Specification
    * and will not be avialiable in the new `scala` command.
    *
    * This also means that that thing should be sable and we need to support it.
    */
  case object RESTRICTED extends SpecificationLevel {
    override def md = "Scala CLI specific"
  }

  /** Annotated option, directive or command will not be a part of the Scala Runner Specification
    * and will not be avialiable in the new `scala` command.
    *
    * Experimental option are not guarantee to be supported in upcoming versions of Scala CLI and
    * all new options should be experimental.
    */
  case object EXPERIMENTAL extends SpecificationLevel {
    override def md: String = toString
  }

  val inSpecification = Seq(MUST, SHOULD)
}

object tags {
  // specification level tags
  val experimental: String   = SpecificationLevel.EXPERIMENTAL.toString
  val restricted: String     = SpecificationLevel.RESTRICTED.toString
  val implementation: String = SpecificationLevel.IMPLEMENTATION.toString
  val must: String           = SpecificationLevel.MUST.toString // included in --help by default
  val should: String         = SpecificationLevel.SHOULD.toString

  // other tags
  // the `inShortHelp` tag whitelists options to be included in --help
  // this is in contrast to blacklisting options in --help with the @Hidden annotation
  val inShortHelp: String = "inShortHelp" // included in --help by default

  def levelFor(name: String): Option[SpecificationLevel] = name match {
    case `experimental`   => Some(SpecificationLevel.EXPERIMENTAL)
    case `restricted`     => Some(SpecificationLevel.RESTRICTED)
    case `implementation` => Some(SpecificationLevel.IMPLEMENTATION)
    case `must`           => Some(SpecificationLevel.MUST)
    case `should`         => Some(SpecificationLevel.SHOULD)
    case _                => None
  }
}
