package scala.build.options

final case class JavaOptions(
  javaHomeOpt: Option[os.Path] = None,
  jvmIdOpt: Option[String] = None,
  jvmIndexOpt: Option[String] = None,
  javaOpts: Seq[String] = Nil
) {
  def orElse(other: JavaOptions): JavaOptions =
    JavaOptions(
      javaHomeOpt = javaHomeOpt.orElse(other.javaHomeOpt),
      jvmIdOpt = jvmIdOpt.orElse(other.jvmIdOpt),
      jvmIndexOpt = jvmIndexOpt.orElse(other.jvmIndexOpt),
      javaOpts = javaOpts ++ other.javaOpts
    )

  def addHashData(update: String => Unit): Unit = {
    for (home <- javaHomeOpt)
      update("javaHome=" + home + "\n")
    for (id <- jvmIdOpt)
      update("jvmId=" + id + "\n")
    for (index <- jvmIndexOpt)
      update("jvmIndex=" + index + "\n")
    for (opt <- javaOpts)
      update("javaOpts+=" + opt + "\n")
  }
}
