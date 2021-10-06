package scala.build

final case class Builds(
  main: Build,
  cross: Seq[Build]
)
