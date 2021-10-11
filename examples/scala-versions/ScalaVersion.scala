// File was generated from based on docs/cookbooks/scala-versions.md, do not edit manually!

object ScalaVersion extends App {
  def props(url: java.net.URL): java.util.Properties = {
    val properties = new java.util.Properties()
    val is = url.openStream()
    try {
      properties.load(is)
      properties
    } finally is.close()    
  }

  def scala2Version: String = 
    props(getClass.getResource("/library.properties")).getProperty("version.number")
    
  def checkScala3(res: java.util.Enumeration[java.net.URL]): String = 
    if (!res.hasMoreElements) scala2Version else {
      val manifest = props(res.nextElement)
      manifest.getProperty("Specification-Title") match {
        case "scala3-library-bootstrapped" =>
          manifest.getProperty("Implementation-Version")
        case _ => checkScala3(res)
      }
    }
  val manifests = getClass.getClassLoader.getResources("META-INF/MANIFEST.MF")
    
  val scalaVersion = checkScala3(manifests)
  val javaVersion = System.getProperty("java.version")

  println(s"Scala: $scalaVersion")
}