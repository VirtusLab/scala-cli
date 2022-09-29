import java.util.Locale

lazy val isArmArchitecture: Boolean =
  sys.props.getOrElse("os.arch", "").toLowerCase(Locale.ROOT) == "aarch64"
