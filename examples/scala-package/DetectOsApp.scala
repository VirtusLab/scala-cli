// File was generated from based on docs/cookbooks/scala-package.mdx, do not edit manually!

object DetectOSApp extends App  {
    def getOperatingSystem(): String = {
        val os: String = System.getProperty("os.name")
        os
    }
    println(s"os: ${getOperatingSystem()}")
}