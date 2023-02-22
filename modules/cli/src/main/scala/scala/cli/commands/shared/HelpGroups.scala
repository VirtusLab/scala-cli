package scala.cli.commands.shared

enum HelpGroup:
  case Benchmarking, BSP, BuildToolExport,
    Config, Compilation, CompilationServer,
    Debian, Debug, Default, Dependency, Doc, Docker,
    Entrypoint,
    Format,
    Help,
    Install,
    Java,
    Launcher, LegacyScalaRunner, Logging,
    MacOS, Markdown,
    NativeImage,
    Package, PGP, Publishing,
    RedHat, Repl, Run, Runner,
    Scala, ScalaJs, ScalaNative, Secret, Signing,
    Test,
    Uninstall, Update,
    Watch, Windows,
    Version

  override def toString: String = this match
    case BuildToolExport   => "Build Tool export"
    case CompilationServer => "Compilation server"
    case LegacyScalaRunner => "Legacy Scala runner"
    case NativeImage       => "Native image"
    case ScalaJs           => "Scala.js"
    case ScalaNative       => "Scala Native"
    case e                 => e.productPrefix

enum HelpCommandGroup:
  case Main, Miscellaneous, Undefined
  override def toString: String = this match
    case Undefined => ""
    case e         => e.productPrefix
