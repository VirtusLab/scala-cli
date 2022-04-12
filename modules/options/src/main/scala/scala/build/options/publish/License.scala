package scala.build.options.publish

import scala.build.Positioned
import scala.build.errors.{BuildException, MalformedInputError}
import scala.build.internal.Licenses

final case class License(name: String, url: String)

object License {

  def parse(input: Positioned[String]): Either[BuildException, Positioned[License]] =
    input.value.split(":", 2) match {
      case Array(name) =>
        Licenses.map.get(name) match {
          case None =>
            Left(new MalformedInputError(
              "license",
              input.value,
              "license-id|license-id:url",
              input.positions
            ))
          case Some(license) =>
            Right(input.map(_ => License(name, license.url)))
        }
      case Array(name, url) =>
        Right(input.map(_ => License(name, url)))
    }

}
