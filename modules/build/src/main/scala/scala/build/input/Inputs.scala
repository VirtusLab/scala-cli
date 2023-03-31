package scala.build.input

import java.io.{ByteArrayInputStream, File}
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.annotation.tailrec
import scala.build.Directories
import scala.build.errors.{BuildException, InputsException, WorkspaceError}
import scala.build.input.ElementsUtils.*
import scala.build.internal.Constants
import scala.build.internal.zip.WrappedZipInputStream
import scala.build.options.Scope
import scala.build.preprocessing.ScopePath
import scala.build.preprocessing.SheBang.isShebangScript
import scala.util.Properties
import scala.util.matching.Regex

final case class Inputs(
  elements: Seq[Element],
  defaultMainClassElement: Option[Script],
  workspace: os.Path,
  baseProjectName: String,
  mayAppendHash: Boolean,
  workspaceOrigin: Option[WorkspaceOrigin],
  enableMarkdown: Boolean,
  allowRestrictedFeatures: Boolean
) {

  def isEmpty: Boolean = elements.isEmpty

  def singleFiles(): Seq[SingleFile] =
    elements.flatMap {
      case f: SingleFile        => Seq(f)
      case d: Directory         => d.singleFilesFromDirectory(enableMarkdown)
      case _: ResourceDirectory => Nil
      case _: Virtual           => Nil
    }

  def sourceFiles(): Seq[SourceFile] =
    singleFiles().collect {
      case f: SourceFile => f
    }

  def flattened(): Seq[SingleElement] =
    elements.flatMap {
      case f: SingleFile        => Seq(f)
      case d: Directory         => d.singleFilesFromDirectory(enableMarkdown)
      case _: ResourceDirectory => Nil
      case v: Virtual           => Seq(v)
    }

  private lazy val inputsHash: String = elements.inputsHash
  lazy val projectName: String = {
    val needsSuffix = mayAppendHash && (elements match {
      case Seq(d: Directory) => d.path != workspace
      case _                 => true
    })
    if needsSuffix then s"$baseProjectName-$inputsHash" else baseProjectName
  }

  def scopeProjectName(scope: Scope): String =
    if scope == Scope.Main then projectName else s"$projectName-${scope.name}"

  def add(extraElements: Seq[Element]): Inputs =
    if elements.isEmpty then this else copy(elements = (elements ++ extraElements).distinct)

  def generatedSrcRoot(scope: Scope): os.Path =
    workspace / Constants.workspaceDirName / projectName / "src_generated" / scope.name

  private def inHomeDir(directories: Directories): Inputs =
    copy(
      workspace = elements.homeWorkspace(directories),
      mayAppendHash = false,
      workspaceOrigin = Some(WorkspaceOrigin.HomeDir)
    )
  def avoid(forbidden: Seq[os.Path], directories: Directories): Inputs =
    if forbidden.exists(workspace.startsWith) then inHomeDir(directories) else this
  def checkAttributes(directories: Directories): Inputs = {
    @tailrec
    def existingParent(p: os.Path): Option[os.Path] =
      if (os.exists(p)) Some(p)
      else if (p.segmentCount <= 0) None
      else existingParent(p / os.up)
    def reallyOwnedByUser(p: os.Path): Boolean =
      if (Properties.isWin)
        p.toIO.canWrite // Wondering if there's a better way to do that…
      else
        os.owner(p) == os.owner(os.home) &&
        p.toIO.canWrite
    val canWrite = existingParent(workspace).exists(reallyOwnedByUser)
    if canWrite then this else inHomeDir(directories)
  }
  def sourceHash(): String = {
    def bytes(s: String): Array[Byte] = s.getBytes(StandardCharsets.UTF_8)
    val it = elements.iterator.flatMap {
      case elem: OnDisk =>
        val content = elem match {
          case dirInput: Directory =>
            Seq("dir:") ++ dirInput.singleFilesFromDirectory(enableMarkdown)
              .map(file => s"${file.path}:" + os.read(file.path))
          case _ => Seq(os.read(elem.path))
        }
        (Iterator(elem.path.toString) ++ content.iterator ++ Iterator("\n")).map(bytes)
      case v: Virtual =>
        Iterator(v.content, bytes("\n"))
    }
    val md = MessageDigest.getInstance("SHA-1")
    it.foreach(md.update)
    val digest        = md.digest()
    val calculatedSum = new BigInteger(1, digest)
    String.format(s"%040x", calculatedSum)
  }

  def nativeWorkDir: os.Path =
    workspace / Constants.workspaceDirName / projectName / "native"
  def nativeImageWorkDir: os.Path =
    workspace / Constants.workspaceDirName / projectName / "native-image"
  def docJarWorkDir: os.Path =
    workspace / Constants.workspaceDirName / projectName / "doc"
}

object Inputs {
  private def forValidatedElems(
    validElems: Seq[Element],
    baseProjectName: String,
    workspace: os.Path,
    needsHash: Boolean,
    workspaceOrigin: WorkspaceOrigin,
    enableMarkdown: Boolean,
    allowRestrictedFeatures: Boolean,
    extraClasspathWasPassed: Boolean
  ): Inputs = {
    assert(extraClasspathWasPassed || validElems.nonEmpty)
    val allDirs = validElems.collect { case d: Directory => d.path }
    val updatedElems = validElems.filter {
      case f: SourceFile =>
        val isInDir = allDirs.exists(f.path.relativeTo(_).ups == 0)
        !isInDir
      case _: Directory         => true
      case _: ResourceDirectory => true
      case _: Virtual           => true
    }
    // only on-disk scripts need a main class override
    val defaultMainClassElemOpt = validElems.collectFirst { case script: Script => script }
    Inputs(
      updatedElems,
      defaultMainClassElemOpt,
      workspace,
      baseProjectName,
      mayAppendHash = needsHash,
      workspaceOrigin = Some(workspaceOrigin),
      enableMarkdown = enableMarkdown,
      allowRestrictedFeatures = allowRestrictedFeatures
    )
  }

  private val githubGistsArchiveRegex: Regex =
    s"""://gist\\.github\\.com/[^/]*?/[^/]*$$""".r

  private def resolveZipArchive(content: Array[Byte], enableMarkdown: Boolean): Seq[Element] = {
    val zipInputStream = WrappedZipInputStream.create(new ByteArrayInputStream(content))
    zipInputStream.entries().foldLeft(List.empty[Element]) {
      (acc, ent) =>
        if (ent.isDirectory) acc
        else {
          val content = zipInputStream.readAllBytes()
          (Virtual(ent.getName, content) match {
            case _: AnyMarkdownFile if !enableMarkdown => None
            case e: Element                            => Some(e)
          }) map { element => element :: acc } getOrElse acc
        }
    }
  }

  def validateSnippets(
    scriptSnippetList: List[String] = List.empty,
    scalaSnippetList: List[String] = List.empty,
    javaSnippetList: List[String] = List.empty,
    markdownSnippetList: List[String] = List.empty
  ): Seq[Either[String, Seq[Element]]] = {
    def validateSnippet(
      snippetList: List[String],
      f: (Array[Byte], String) => Element
    ): Seq[Either[String, Seq[Element]]] =
      snippetList.zipWithIndex.map { case (snippet, index) =>
        val snippetName: String = if (index > 0) s"snippet$index" else "snippet"
        if (snippet.nonEmpty) Right(Seq(f(snippet.getBytes(StandardCharsets.UTF_8), snippetName)))
        else Left(s"Empty snippet was passed: $snippetName")
      }

    Seq(
      validateSnippet(
        scriptSnippetList,
        (content, snippetName) => VirtualScript(content, snippetName, os.sub / s"$snippetName.sc")
      ),
      validateSnippet(
        scalaSnippetList,
        (content, snippetNameSuffix) =>
          VirtualScalaFile(content, s"<snippet>-scala-$snippetNameSuffix")
      ),
      validateSnippet(
        javaSnippetList,
        (content, snippetNameSuffix) =>
          VirtualJavaFile(content, s"<snippet>-java-$snippetNameSuffix")
      ),
      validateSnippet(
        markdownSnippetList,
        (content, snippetNameSuffix) =>
          VirtualMarkdownFile(
            content,
            s"<snippet>-markdown-$snippetNameSuffix",
            os.sub / s"$snippetNameSuffix.md"
          )
      )
    ).flatten
  }

  def validateArgs(
    args: Seq[String],
    cwd: os.Path,
    download: String => Either[String, Array[Byte]],
    stdinOpt: => Option[Array[Byte]],
    acceptFds: Boolean,
    enableMarkdown: Boolean
  )(using programInvokeData: ScalaCliInvokeData): Seq[Either[String, Seq[Element]]] =
    args.zipWithIndex.map {
      case (arg, idx) =>
        lazy val path      = os.Path(arg, cwd)
        lazy val dir       = path / os.up
        lazy val subPath   = path.subRelativeTo(dir)
        lazy val stdinOpt0 = stdinOpt
        lazy val content   = os.read.bytes(path)
        lazy val fullProgramCall = programInvokeData.progName +
          s"${
              if programInvokeData.subCommand == SubCommand.Default then ""
              else s" ${programInvokeData.subCommandName}"
            }"
        val unrecognizedSourceErrorMsg =
          s"$arg: unrecognized source type (expected .scala or .sc extension, or a directory)."
        val missingShebangHeaderErrorMsg =
          s"""$unrecognizedSourceErrorMsg
             |If $arg is meant to be treated as a script, add a shebang header in its top line.
             |  ${Console.BOLD}#!/usr/bin/env -S ${programInvokeData.progName} shebang${Console.RESET}
             |When a shebang header is provided, the script can then be run with the 'shebang' sub-command, even if no file extension is present.
             |  ${Console.BOLD}${programInvokeData.progName} shebang $arg${Console.RESET}""".stripMargin

        if (arg == "-.scala" || arg == "_" || arg == "_.scala") && stdinOpt0.nonEmpty then
          Right(Seq(VirtualScalaFile(stdinOpt0.get, "<stdin>-scala-file")))
        else if (arg == "-.java" || arg == "_.java") && stdinOpt0.nonEmpty then
          Right(Seq(VirtualJavaFile(stdinOpt0.get, "<stdin>-java-file")))
        else if (arg == "-" || arg == "-.sc" || arg == "_.sc") && stdinOpt0.nonEmpty then
          Right(Seq(VirtualScript(stdinOpt0.get, "stdin", os.sub / "stdin.sc")))
        else if (arg == "-.md" || arg == "_.md") && stdinOpt0.nonEmpty then
          Right(Seq(VirtualMarkdownFile(
            stdinOpt0.get,
            "<stdin>-markdown-file",
            os.sub / "stdin.md"
          )))
        else if arg.endsWith(".zip") && os.exists(path) then
          Right(resolveZipArchive(content, enableMarkdown))
        else if arg.contains("://") then {
          val isGithubGist = githubGistsArchiveRegex.findFirstMatchIn(arg).nonEmpty
          val url          = if isGithubGist then s"$arg/download" else arg
          download(url).map { urlContent =>
            if isGithubGist then resolveZipArchive(urlContent, enableMarkdown)
            else List(Virtual(url, urlContent))
          }
        }
        else if path.last == Constants.projectFileName then
          Right(Seq(ProjectScalaFile(dir, subPath)))
        else if arg.endsWith(".sc") then Right(Seq(Script(dir, subPath)))
        else if arg.endsWith(".scala") then Right(Seq(SourceScalaFile(dir, subPath)))
        else if arg.endsWith(".java") then Right(Seq(JavaFile(dir, subPath)))
        else if arg.endsWith(".jar") then Right(Seq(JarFile(dir, subPath)))
        else if arg.endsWith(".c") || arg.endsWith(".h") then Right(Seq(CFile(dir, subPath)))
        else if arg.endsWith(".md") then Right(Seq(MarkdownFile(dir, subPath)))
        else if os.isDir(path) then Right(Seq(Directory(path)))
        else if acceptFds && arg.startsWith("/dev/fd/") then
          Right(Seq(VirtualScript(content, arg, os.sub / s"input-${idx + 1}.sc")))
        else if programInvokeData.subCommand == SubCommand.Shebang && os.exists(path) then
          if isShebangScript(String(content)) then Right(Seq(Script(dir, subPath)))
          else
            Left(
              if programInvokeData.isShebangCapableShell then missingShebangHeaderErrorMsg
              else unrecognizedSourceErrorMsg
            )
        else {
          val msg =
            if os.exists(path) then
              programInvokeData match {
                case ScalaCliInvokeData(progName, _, _, true)
                    if isShebangScript(String(content)) =>
                  s"""$arg: scripts with no file extension should be run with the 'shebang' sub-command.
                     |  ${Console.BOLD}$progName shebang $arg${Console.RESET}""".stripMargin
                case ScalaCliInvokeData(_, _, _, true) => missingShebangHeaderErrorMsg
                case _                                 => unrecognizedSourceErrorMsg
              }
            else if programInvokeData.subCommand == SubCommand.Default && idx == 0 &&
              arg.forall(_.isLetterOrDigit)
            then
              s"""$arg is not a ${programInvokeData.progName} sub-command and it is not a valid path to an input file or directory.
                 |Try viewing the relevant help to see the list of available sub-commands and options.
                 |  ${Console.BOLD}${programInvokeData.progName} --help${Console.RESET}""".stripMargin
            else
              s"""$arg: input file not found
                 |Try viewing the relevant help to see the list of available sub-commands and options.
                 |  ${Console.BOLD}$fullProgramCall --help${Console.RESET}""".stripMargin
          Left(msg)
        }
    }

  private def forNonEmptyArgs(
    args: Seq[String],
    cwd: os.Path,
    baseProjectName: String,
    download: String => Either[String, Array[Byte]],
    stdinOpt: => Option[Array[Byte]],
    scriptSnippetList: List[String],
    scalaSnippetList: List[String],
    javaSnippetList: List[String],
    markdownSnippetList: List[String],
    acceptFds: Boolean,
    forcedWorkspace: Option[os.Path],
    enableMarkdown: Boolean,
    allowRestrictedFeatures: Boolean,
    extraClasspathWasPassed: Boolean
  )(using invokeData: ScalaCliInvokeData): Either[BuildException, Inputs] = {
    val validatedArgs: Seq[Either[String, Seq[Element]]] =
      validateArgs(
        args,
        cwd,
        download,
        stdinOpt,
        acceptFds,
        enableMarkdown
      )
    val validatedSnippets: Seq[Either[String, Seq[Element]]] =
      validateSnippets(scriptSnippetList, scalaSnippetList, javaSnippetList, markdownSnippetList)
    val validatedArgsAndSnippets = validatedArgs ++ validatedSnippets
    val invalid = validatedArgsAndSnippets.collect {
      case Left(msg) => msg
    }
    if (invalid.isEmpty) {
      val validElems = validatedArgsAndSnippets.collect {
        case Right(elem) => elem
      }.flatten
      assert(extraClasspathWasPassed || validElems.nonEmpty)
      val (inferredWorkspace, inferredNeedsHash, workspaceOrigin) = {
        val settingsFiles = validElems.projectSettingsFiles
        val dirsAndFiles = validElems.collect {
          case d: Directory  => d
          case f: SourceFile => f
        }
        settingsFiles.headOption.map { s =>
          if (settingsFiles.length > 1)
            System.err.println(
              s"Warning: more than one ${Constants.projectFileName} file has been found. Setting ${s.base} as the project root directory for this run."
            )
          (s.base, true, WorkspaceOrigin.SourcePaths)
        }.orElse {
          dirsAndFiles.collectFirst {
            case d: Directory =>
              if (dirsAndFiles.length > 1)
                System.err.println(
                  s"Warning: setting ${d.path} as the project root directory for this run."
                )
              (d.path, true, WorkspaceOrigin.SourcePaths)
            case f: SourceFile =>
              if (dirsAndFiles.length > 1)
                System.err.println(
                  s"Warning: setting ${f.path / os.up} as the project root directory for this run."
                )
              (f.path / os.up, true, WorkspaceOrigin.SourcePaths)
          }
        }.orElse {
          validElems.collectFirst {
            case _: Virtual =>
              (os.temp.dir(), true, WorkspaceOrigin.VirtualForced)
          }
        }.getOrElse((os.pwd, true, WorkspaceOrigin.Forced))
      }

      val (workspace, needsHash, workspaceOrigin0) = forcedWorkspace match {
        case None => (inferredWorkspace, inferredNeedsHash, workspaceOrigin)
        case Some(forcedWorkspace0) =>
          val needsHash0 = forcedWorkspace0 != inferredWorkspace || inferredNeedsHash
          (forcedWorkspace0, needsHash0, WorkspaceOrigin.Forced)
      }

      if workspace.toString.contains(File.pathSeparator) then
        val prog       = invokeData.invocationString
        val argsString = args.mkString(" ")
        Left(new WorkspaceError(
          s"""Invalid workspace path: ${Console.BOLD}$workspace${Console.RESET}
             |Workspace path cannot contain a ${Console.BOLD}${File.pathSeparator}${Console.RESET}.
             |Consider moving your project to a different path.
             |Alternatively, you can force your workspace with the '--workspace' option:
             |    ${Console.BOLD}$prog --workspace <alternative-workspace-path> $argsString${Console.RESET}"""
            .stripMargin
        ))
      else
        Right(forValidatedElems(
          validElems,
          baseProjectName,
          workspace,
          needsHash,
          workspaceOrigin0,
          enableMarkdown,
          allowRestrictedFeatures,
          extraClasspathWasPassed
        ))
    }
    else
      Left(new InputsException(invalid.mkString(System.lineSeparator())))
  }

  def apply(
    args: Seq[String],
    cwd: os.Path,
    baseProjectName: String = "project",
    defaultInputs: () => Option[Inputs] = () => None,
    download: String => Either[String, Array[Byte]] = _ => Left("URL not supported"),
    stdinOpt: => Option[Array[Byte]] = None,
    scriptSnippetList: List[String] = List.empty,
    scalaSnippetList: List[String] = List.empty,
    javaSnippetList: List[String] = List.empty,
    markdownSnippetList: List[String] = List.empty,
    acceptFds: Boolean = false,
    forcedWorkspace: Option[os.Path] = None,
    enableMarkdown: Boolean = false,
    allowRestrictedFeatures: Boolean,
    extraClasspathWasPassed: Boolean
  )(using ScalaCliInvokeData): Either[BuildException, Inputs] =
    if (
      args.isEmpty && scriptSnippetList.isEmpty && scalaSnippetList.isEmpty && javaSnippetList.isEmpty &&
      markdownSnippetList.isEmpty && !extraClasspathWasPassed
    )
      defaultInputs().toRight(new InputsException(
        "No inputs provided (expected files with .scala, .sc, .java or .md extensions, and / or directories)."
      ))
    else
      forNonEmptyArgs(
        args,
        cwd,
        baseProjectName,
        download,
        stdinOpt,
        scriptSnippetList,
        scalaSnippetList,
        javaSnippetList,
        markdownSnippetList,
        acceptFds,
        forcedWorkspace,
        enableMarkdown,
        allowRestrictedFeatures,
        extraClasspathWasPassed
      )

  def default(): Option[Inputs] = None

  def empty(workspace: os.Path, enableMarkdown: Boolean): Inputs =
    Inputs(
      elements = Nil,
      defaultMainClassElement = None,
      workspace = workspace,
      baseProjectName = "project",
      mayAppendHash = true,
      workspaceOrigin = None,
      enableMarkdown = enableMarkdown,
      allowRestrictedFeatures = false
    )

  def empty(projectName: String): Inputs =
    Inputs(Nil, None, os.pwd, projectName, false, None, true, false)
}
