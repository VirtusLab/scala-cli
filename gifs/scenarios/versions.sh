#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  cat <<EOF > versions.scala
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

  println(s"Scala: \$scalaVersion Java: \$javaVersion")
}
EOF

  cat <<EOF > classpath.scala
object Main extends App {
  val classpath = System.getProperty("java.class.path").split(java.io.File.pathSeparator)
  val ignoreIf = Seq("scala-cli", "scala-lang", "jline", "scala-sbt", "pretty-stacktraces", "java/dev/jna", "protobuf-java")
  println(classpath.toList
    .filter(l => !ignoreIf.exists(l.contains))
    .filter(_.endsWith(".jar"))
    .map(_.split("/").last)
    .sorted
    .mkString("Jars: ", ", ", "")
  )
}
EOF

  scala-cli versions.scala
  scala-cli --scala 2 versions.scala
  scala-cli --scala 2.12.12 versions.scala
  scala-cli --jvm 8 versions.scala
  scala-cli --jvm adopt:9 versions.scala
  scala-cli --scala 2 --dep org.typelevel::cats-core:2.3.0 classpath.scala
  scala-cli --dep org.scalameta::munit:0.7.29 classpath.scala

else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clear

  # Put your stuff here


  pe "scala-cli versions.scala"
  pe "scala-cli --scala 2 versions.scala"
  pe "scala-cli --scala 2.12.12 versions.scala"
  sleep 2
  clear
  pe "scala-cli --jvm 8 versions.scala"
  pe "scala-cli --jvm adopt:9 versions.scala"
  sleep 2
  clear
  pe "scala-cli --dep org.scalameta::munit:0.7.29 classpath.scala"
  pe "scala-cli --scala 2 --dep org.typelevel::cats-core:2.3.0 classpath.scala"

  # Wait a bit to read output of last command
  sleep 2
  echo " " && echo "ok" > status.txt
fi