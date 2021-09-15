package scala.build.errors

final class CompositeBuildException private (
  val mainException: BuildException,
  val others: Seq[BuildException]
) extends BuildException(
      s"${others.length + 1} exceptions, first one: ${mainException.getMessage}",
      mainException
    )

object CompositeBuildException {
  private def flatten(list: ::[BuildException]): ::[BuildException] = {
    val list0 = list.flatMap {
      case c: CompositeBuildException => c.mainException :: c.others.toList
      case e                          => e :: Nil
    }
    list0 match {
      case Nil    => sys.error("Can't happen")
      case h :: t => ::(h, t)
    }
  }
  def apply(exceptions: ::[BuildException]): BuildException =
    flatten(exceptions) match {
      case h :: Nil => h
      case h :: t   => new CompositeBuildException(h, t)
    }
}
