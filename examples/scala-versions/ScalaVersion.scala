// File was generated from based on docs/cookbooks/scala-versions.md, do not edit manually!

object ScalaVersion extends App {
    val props = new java.util.Properties
    props.load(getClass.getResourceAsStream("/library.properties"))
    val line = props.getProperty("version.number")
    val Version = """(\d\.\d+\.\d+).*""".r
    val Version(versionStr) = line
    println(s"Using Scala version: $versionStr")
}