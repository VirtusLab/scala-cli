package scala.build.preprocessing.directives

case class UsingDirectiveValueNumberBounds(
  lower: Int = 1,
  upper: Int = Int.MaxValue
) {
  require(lower > -1)

  override def toString: String = s"at least at $lower and at most $upper num of values "
}
