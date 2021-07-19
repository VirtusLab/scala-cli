package scala.build.options

import dependency.AnyDependency

import scala.build.internal.CodeWrapper

trait HashedType[T] {
  def hashedValue(t: T): String
}

object HashedType {
  def apply[T](implicit instance: HashedType[T]): HashedType[T] = instance

  implicit val string: HashedType[String] = {
    str => str
  }

  implicit val path: HashedType[os.Path] = {
    path => path.toString
  }

  implicit val boolean: HashedType[Boolean] = {
    bool => bool.toString
  }

  implicit val anyDependency: HashedType[AnyDependency] = {
    dep => dep.render
  }

  implicit val packageType: HashedType[PackageType] = {
    tpe => tpe.toString
  }

  implicit val codeWrapper: HashedType[CodeWrapper] = {
    wrapper => wrapper.toString
  }

}
