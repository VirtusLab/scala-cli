package scala.build.input

import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import scala.annotation.tailrec
import scala.build.Directories
import scala.build.errors.{BuildException, InputsException}
import scala.build.input.ElementsUtils.*
import scala.build.internal.Constants
import scala.build.internal.zip.WrappedZipInputStream
import scala.build.options.Scope
import scala.build.preprocessing.ScopePath
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
    forcedWorkspace: Option[os.Path],
    enableMarkdown: Boolean,
    allowRestrictedFeatures: Boolean,
    extraClasspathWasPassed: Boolean
  ): Inputs = {
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
            s"Warning: more than one project.scala file has been found. Setting ${s.base} as the project root directory for this run."
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
      }.getOrElse((os.pwd, true, WorkspaceOrigin.Forced))
    }

    val (workspace, needsHash, workspaceOrigin0) = forcedWorkspace match {
      case None => (inferredWorkspace, inferredNeedsHash, workspaceOrigin)
      case Some(forcedWorkspace0) =>
        val needsHash0 = forcedWorkspace0 != inferredWorkspace || inferredNeedsHash
        (forcedWorkspace0, needsHash0, WorkspaceOrigin.Forced)
    }
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
      workspaceOrigin = Some(workspaceOrigin0),
      enableMarkdown = enableMarkdown,
      allowRestrictedFeatures = allowRestrictedFeatures
    )
  }

  private val githubGistsArchiveRegex: Regex =
    s"""://gist\\.github\\.com/[^/]*?/[^/]*$$""".r

  private def resolve(path: String, content: Array[Byte]): Element =
    if (path.endsWith(".scala")) VirtualScalaFile(content, path)
    else if (path.endsWith(".java")) VirtualJavaFile(content, path)
    else if (path.endsWith(".sc")) {
      val wrapperPath = os.sub / path.split("/").last
      VirtualScript(content, path, wrapperPath)
    }
    else if (path.endsWith(".md")) {
      val wrapperPath = os.sub / path.split("/").last
      VirtualMarkdownFile(content, path, wrapperPath)
    }
    else VirtualData(content, path)

  private def resolveZipArchive(content: Array[Byte], enableMarkdown: Boolean): Seq[Element] = {
    val zipInputStream = WrappedZipInputStream.create(new ByteArrayInputStream(content))
    zipInputStream.entries().foldLeft(List.empty[Element]) {
      (acc, ent) =>
        if (ent.isDirectory) acc
        else {
          val content = zipInputStream.readAllBytes()
          (resolve(ent.getName, content) match {
            case _: VirtualMarkdownFile if !enableMarkdown => None
            case e: Element                                => Some(e)
          }) map { element => element :: acc } getOrElse acc
        }
    }
  }

  def validateSnippets(
    scriptSnippetList: List[String] = List.empty,
    scalaSnippetList: List[String] = List.empty,
    javaSnippetList: List[String] = List.empty
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
  ): Seq[Either[String, Seq[Element]]] = args.zipWithIndex.map {
    case (arg, idx) =>
      lazy val path      = os.Path(arg, cwd)
      lazy val dir       = path / os.up
      lazy val subPath   = path.subRelativeTo(dir)
      lazy val stdinOpt0 = stdinOpt
      val isStdin = (arg == "-.scala" || arg == "_" || arg == "_.scala") &&
        stdinOpt0.nonEmpty
      if (isStdin) Right(Seq(VirtualScalaFile(stdinOpt0.get, "<stdin>-scala-file")))
      else if ((arg == "-.java" || arg == "_.java") && stdinOpt0.nonEmpty)
        Right(Seq(VirtualJavaFile(stdinOpt0.get, "<stdin>-java-file")))
      else if ((arg == "-" || arg == "-.sc" || arg == "_.sc") && stdinOpt0.nonEmpty)
        Right(Seq(VirtualScript(stdinOpt0.get, "stdin", os.sub / "stdin.sc")))
      else if (arg.endsWith(".zip") && os.exists(os.Path(arg, cwd))) {
        val content = os.read.bytes(os.Path(arg, cwd))
        Right(resolveZipArchive(content, enableMarkdown))
      }
      else if (arg.contains("://")) {
        val url =
          if githubGistsArchiveRegex.findFirstMatchIn(arg).nonEmpty then s"$arg/download" else arg
        download(url).map { content =>
          if (githubGistsArchiveRegex.findFirstMatchIn(arg).nonEmpty)
            resolveZipArchive(content, enableMarkdown)
          else
            List(resolve(url, content))
        }
      }
      else if (path.last == "project.scala") Right(Seq(ProjectScalaFile(dir, subPath)))
      else if (arg.endsWith(".sc")) Right(Seq(Script(dir, subPath)))
      else if (arg.endsWith(".scala")) Right(Seq(SourceScalaFile(dir, subPath)))
      else if (arg.endsWith(".java")) Right(Seq(JavaFile(dir, subPath)))
      else if (arg.endsWith(".c") || arg.endsWith(".h")) Right(Seq(CFile(dir, subPath)))
      else if (arg.endsWith(".md")) Right(Seq(MarkdownFile(dir, subPath)))
      else if (os.isDir(path)) Right(Seq(Directory(path)))
      else if (acceptFds && arg.startsWith("/dev/fd/")) {
        val content = os.read.bytes(os.Path(arg, cwd))
        Right(Seq(VirtualScript(content, arg, os.sub / s"input-${idx + 1}.sc")))
      }
      else {
        val msg =
          if (os.exists(path))
            s"$arg: unrecognized source type (expected .scala or .sc extension, or a directory)"
          else s"$arg: not found"
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
    acceptFds: Boolean,
    forcedWorkspace: Option[os.Path],
    enableMarkdown: Boolean,
    allowRestrictedFeatures: Boolean,
    extraClasspathWasPassed: Boolean
  ): Either[BuildException, Inputs] = {
    val validatedArgs: Seq[Either[String, Seq[Element]]] =
      validateArgs(args, cwd, download, stdinOpt, acceptFds, enableMarkdown)
    val validatedSnippets: Seq[Either[String, Seq[Element]]] =
      validateSnippets(scriptSnippetList, scalaSnippetList, javaSnippetList)
    val validatedArgsAndSnippets = validatedArgs ++ validatedSnippets
    val invalid = validatedArgsAndSnippets.collect {
      case Left(msg) => msg
    }
    if (invalid.isEmpty) {
      val validElems = validatedArgsAndSnippets.collect {
        case Right(elem) => elem
      }.flatten
      assert(extraClasspathWasPassed || validElems.nonEmpty)

      Right(forValidatedElems(
        validElems,
        baseProjectName,
        forcedWorkspace,
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
    acceptFds: Boolean = false,
    forcedWorkspace: Option[os.Path] = None,
    enableMarkdown: Boolean = false,
    allowRestrictedFeatures: Boolean,
    extraClasspathWasPassed: Boolean
  ): Either[BuildException, Inputs] =
    if (
      args.isEmpty && scriptSnippetList.isEmpty && scalaSnippetList.isEmpty && javaSnippetList.isEmpty && !extraClasspathWasPassed
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
