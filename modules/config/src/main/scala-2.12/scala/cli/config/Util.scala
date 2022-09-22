package scala.cli.config

private[config] object Util {

  implicit class ConfigStringOps(private val s: String) extends AnyVal {
    def toBooleanOption: Option[Boolean] =
      try Some(s.toBoolean)
      catch {
        case e: IllegalArgumentException =>
          None
      }
  }

}
