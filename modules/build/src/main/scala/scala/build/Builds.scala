package scala.build

import scala.build.options.Scope

final case class Builds(
  builds: Seq[Build],
  crossBuilds: Seq[Seq[Build]],
  docBuilds: Seq[Build],
  docCrossBuilds: Seq[Seq[Build]]
) {
  def main: Build =
    get(Scope.Main).getOrElse {
      sys.error("No main build found")
    }
  def get(scope: Scope): Option[Build] =
    builds.find(_.scope == scope)
  def anyFailed: Boolean = !all.forall(_.success)

  def all: Seq[Build] =
    builds ++ crossBuilds.flatten
  lazy val map: Map[CrossKey, Build.Successful] =
    all
      .collect {
        case s: Build.Successful => s
      }
      .map(b => b.crossKey -> b)
      .toMap

  def allDoc: Seq[Build] =
    docBuilds ++ docCrossBuilds.flatten
}
