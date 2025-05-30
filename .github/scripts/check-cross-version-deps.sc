#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default

val modules =
  os.proc(os.pwd / "mill", "-i", "resolve", "__[]")
    .call(cwd = os.pwd)
    .out
    .lines()

for { module <- modules } {
  println(s"Checking for $module...")
  val depRegex            = "\\[\\d+]\\s+[│└├─\\S\\s]+\\s([\\w.-]+):([\\w.-]+):([\\w\\s\\S.-]+)".r
  val scalaDepSuffixRegex = "^(.+?)(_[23](?:\\.\\d{2})?)?$".r
  val deps = os.proc(os.pwd / "mill", "-i", s"$module.ivyDepsTree")
    .call(cwd = os.pwd)
    .out
    .lines()
    .filter(_.count(_ == ':') == 2)
    .map { case depRegex(org, name, depVersion) => (org, name, depVersion) }
  val scalaVersionsByOrgAndName = deps
    .groupBy { case (org, scalaDepSuffixRegex(nameWithoutSuffix, _), _) =>
      s"$org:$nameWithoutSuffix"
    }
    .map { case (key, entries) =>
      key -> entries.map { case (_, scalaDepSuffixRegex(_, scalaVersion), _) =>
        scalaVersion
      }.distinct
    }
    .filter { case (_, scalaVersions) => scalaVersions.head != null } // filter out non-Scala deps
  println("Checking for clashing dependency Scala versions...")
  val conflictEntries: Map[String, Vector[String]] =
    scalaVersionsByOrgAndName
      .filter { case (key, scalaVersions) =>
        if scalaVersions.length == 1 then
          println(s"[info] $key${scalaVersions.head} (OK)")
          false
        else
          println(
            s"[${Console.RED}error${Console.RESET}] $key: multiple conflicting Scala versions: ${scalaVersions.mkString(", ")}"
          )
          true
      }
  if conflictEntries.nonEmpty then
    println(s"${Console.RED}ERROR: Found ${conflictEntries.size} conflicting entries for $module:")
    conflictEntries.foreach {
      case (key, scalaVersions) =>
        println(s"  $key: multiple conflicting Scala versions: ${scalaVersions.mkString(", ")}")
    }
    println(Console.RESET)
    sys.exit(1)
  else println(s"[info] $module OK")
}

println("Checks completed for:")
modules.foreach(m => println(s"  $m"))
println("No conflicts detected.")
sys.exit(0)
