package scala.build.preprocessing.directives

object ClasspathUtils {
  extension (classpathItem: os.Path) {
    def hasSourceJarSuffix: Boolean = classpathItem.last.endsWith("-sources.jar")
  }
}
