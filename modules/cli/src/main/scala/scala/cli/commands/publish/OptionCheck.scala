package scala.cli.commands.publish

import scala.build.errors.BuildException
import scala.build.options.{PublishOptions => BPublishOptions}

/** A check for missing options in [[PublishOptions]]
  */
trait OptionCheck {

  /** The "group" of check this check belongs to, so that users can filter them */
  def kind: OptionCheck.Kind

  /** Name of the option checked, for display / reporting purposes */
  def fieldName: String

  /** Directive name of the option checked by this check */
  def directivePath: String

  /** Checks whether the option value is missing */
  def check(options: BPublishOptions): Boolean

  /** Provides a way to compute a default value for this option, along with extra directives and
    * GitHub secrets to be set
    */
  def defaultValue(): Either[BuildException, OptionCheck.DefaultValue]
}

object OptionCheck {

  /** Computes a default value for a directive
    *
    * @param getValue
    *   computes a default value
    * @param extraDirectives
    *   extra using directives to be set
    * @param ghSecrets
    *   GitHub secrets to be set
    */
  final case class DefaultValue(
    getValue: () => Either[BuildException, Option[String]],
    extraDirectives: Seq[(String, String)],
    ghSecrets: Seq[SetSecret]
  )

  object DefaultValue {
    def simple(
      value: String,
      extraDirectives: Seq[(String, String)],
      ghSecrets: Seq[SetSecret]
    ): DefaultValue =
      DefaultValue(
        () => Right(Some(value)),
        extraDirectives,
        ghSecrets
      )
    def empty: DefaultValue =
      DefaultValue(
        () => Right(None),
        Nil,
        Nil
      )
  }

  sealed abstract class Kind extends Product with Serializable

  object Kind {
    case object Core       extends Kind
    case object Extra      extends Kind
    case object Repository extends Kind
    case object Signing    extends Kind

    val all = Seq(Core, Extra, Repository, Signing)

    def parse(input: String): Option[Kind] =
      input match {
        case "core"                => Some(Core)
        case "extra"               => Some(Extra)
        case "repo" | "repository" => Some(Repository)
        case "signing"             => Some(Signing)
        case _                     => None
      }
    def parseList(input: String): Either[Seq[String], Seq[Kind]] = {
      val results = input.split(",").map(v => (v, parse(v))).toSeq
      val unrecognized = results.collect {
        case (v, None) => v
      }
      if (unrecognized.isEmpty)
        Right {
          results.collect {
            case (_, Some(kind)) => kind
          }
        }
      else
        Left(unrecognized)
    }
  }
}
