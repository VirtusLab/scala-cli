package scala.cli.commands.publish

import scala.cli.signing.shared.Secret

final case class SetSecret(
  name: String,
  value: Secret[String],
  force: Boolean
)
