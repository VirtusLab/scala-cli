package scala.cli.commands

import caseapp.annotation.Tag
import os.copy

sealed trait SpecificationLevel extends Product with Serializable {
  def md = toString() + " have"
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
    * This also means that that thing should be stable and we need to support it.
    */
  case object MUST extends SpecificationLevel

  /** Marks option, directive or command that SHOULD be a part of any Scala Runner Specification (in
    * RFC meaning). Annotated thing will be included in a new `scala` command.
    *
    * This also means that that thing should be stable and we need to support it.
    */
  case object SHOULD extends SpecificationLevel

  /** Marks option, directive or command that is an implementation details of Scala CLI and will not
    * be a part of any Scala Runner Specification. Annotated thing will be included in a new `scala`
    * command.
    *
    * This also means that that thing should be stable and we need to support it.
    */
  case object IMPLEMENTATION extends SpecificationLevel {
    override def md = toString() + " specific"
  }

  /** Annotated option, directive or command will not be a part of the Scala Runner Specification
    * and will not be avialiable in the new `scala` command.
    *
    * This also means that that thing should be sable and we need to support it.
    */
  case object RESTRICTED extends SpecificationLevel {
    override def md = "Scala CLI specific"
  }

  /** Marks option, directive or command that is an internal implementation of Scala CLI and will
    * not be a part of any Scala Runner Specification. Annotated thing will be included in a new
    * `scala` command.
    *
    * Such options are not intended to be used by users and in most cases will be hidden from help
    * and not used in tutorials etc.
    *
    * This does not guarantee that given option will be stable or supported in upcoming releases and
    * we do not recommend using it in scripts.
    *
    * Those options are mainly provided to debug or workaround problems or to offer more control
    * over 3-party tools like bloop or coursier.
    *
    * We recommend if anyone needs more permanent use of any of those options then it should be
    * reported as an issue so we can consider more peremenent solution.
    */
  case object INTERNAL extends SpecificationLevel {
    override def md = toString() + " internal"
  }

  /** Annotated option, directive or command will not be a part of the Scala Runner Specification
    * and will not be avialiable in the new `scala` command.
    *
    * Experimental option are not guarantee to be supported in upcoming versions of Scala CLI and
    * all new options should be experimental.
    */
  case object EXPERIMENTAL extends SpecificationLevel {
    override def md: String = toString()
  }

  val inSpecification = Seq(MUST, SHOULD)
}

object tags {
  val experimental   = SpecificationLevel.EXPERIMENTAL.toString()
  val restricted     = SpecificationLevel.RESTRICTED.toString()
  val implementation = SpecificationLevel.IMPLEMENTATION.toString()
  val internal       = SpecificationLevel.INTERNAL.toString()
  val must           = SpecificationLevel.MUST.toString()
  val should         = SpecificationLevel.SHOULD.toString()

  def levelFor(name: String) = name match {
    case `experimental`   => Some(SpecificationLevel.EXPERIMENTAL)
    case `restricted`     => Some(SpecificationLevel.RESTRICTED)
    case `implementation` => Some(SpecificationLevel.IMPLEMENTATION)
    case `must`           => Some(SpecificationLevel.MUST)
    case `should`         => Some(SpecificationLevel.SHOULD)
    case `internal`       => Some(SpecificationLevel.INTERNAL)
    case _                => None
  }
}
