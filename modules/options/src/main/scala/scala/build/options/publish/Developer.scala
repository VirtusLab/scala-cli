package scala.build.options.publish

import scala.build.Positioned
import scala.build.errors.{BuildException, MalformedInputError}

final case class Developer(
  id: String,
  name: String,
  url: String,
  mail: Option[String] = None
)

object Developer {

  def parse(input: Positioned[String]): Either[BuildException, Developer] =
    input.value.split("|", 4) match {
      case Array(id, name, url) =>
        Right(Developer(id, name, url))
      case Array(id, name, url, mail) =>
        Right(Developer(id, name, url, Some(mail).map(_.trim).filter(_.nonEmpty)))
      case _ =>
        Left(
          new MalformedInputError("developer", input.value, "id|name|URL", input.positions)
        )
    }

}
