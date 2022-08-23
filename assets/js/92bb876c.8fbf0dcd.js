"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[6347],{9705:function(e,t,a){a.d(t,{m:function(){return o},v:function(){return s}});var n=a(7294),i=a(2004);function s(e){var t=e.children;return n.createElement("div",{className:"runnable-command"},t)}function o(e){var t=e.url;return n.createElement(i.Z,{playing:!0,loop:!0,muted:!0,controls:!0,width:"100%",height:"",url:t})}},8230:function(e,t,a){a.r(t),a.d(t,{assets:function(){return d},contentTitle:function(){return c},default:function(){return h},frontMatter:function(){return r},metadata:function(){return p},toc:function(){return m}});var n=a(3117),i=a(102),s=(a(7294),a(3905)),o=a(9705),l=["components"],r={title:"Getting started",sidebar_position:2},c=void 0,p={unversionedId:"getting_started",id:"getting_started",title:"Getting started",description:"This article requires knowledge of the Scala language (how to define a class or method) as well as Scala tooling (the REPL, and basics of dependency management and unit tests).",source:"@site/docs/getting_started.md",sourceDirName:".",slug:"/getting_started",permalink:"/docs/getting_started",draft:!1,editUrl:"https://github.com/Virtuslab/scala-cli/edit/main/website/docs/getting_started.md",tags:[],version:"current",sidebarPosition:2,frontMatter:{title:"Getting started",sidebar_position:2},sidebar:"tutorialSidebar",previous:{title:"Overview",permalink:"/docs/overview"},next:{title:"Basics",permalink:"/docs/commands/basics"}},d={},m=[{value:"Scripting",id:"scripting",level:2},{value:"Dependencies",id:"dependencies",level:2},{value:"A project",id:"a-project",level:2},{value:"IDE support",id:"ide-support",level:2},{value:"Tests",id:"tests",level:2},{value:"A project, vol 2",id:"a-project-vol-2",level:2},{value:"Packaging",id:"packaging",level:2},{value:"Summary",id:"summary",level:2}],u={toc:m};function h(e){var t=e.components,a=(0,i.Z)(e,l);return(0,s.kt)("wrapper",(0,n.Z)({},u,a,{components:t,mdxType:"MDXLayout"}),(0,s.kt)("admonition",{type:"info"},(0,s.kt)("p",{parentName:"admonition"},"This article requires knowledge of the Scala language (how to define a class or method) as well as Scala tooling (the REPL, and basics of dependency management and unit tests).")),(0,s.kt)("p",null,"In this article we show how to use Scala CLI to create a basic script, followed by small project with features like dependencies, tests, and IDE support. We aim to provide you with a knowledge of how to create and develop your first projects using Scala CLI."),(0,s.kt)("p",null,"First, let's verify if Scala CLI is properly ",(0,s.kt)("a",{parentName:"p",href:"/install"},"installed"),' with a simple "hello world" test:'),(0,s.kt)(o.v,{mdxType:"ChainedSnippets"},(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash"},"echo 'println(\"Hello\")' | scala-cli -\n")),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre"},"Hello\n"))),(0,s.kt)("p",null,"Running this command the first time may take a bit longer then usual and print a fair number of logging output because Scala CLI needs to download all the artifacts it needs to compile and run the code."),(0,s.kt)("h2",{id:"scripting"},"Scripting"),(0,s.kt)("p",null,"In that example we actually just created a Scala Script. To demonstrate this more fully, let's create a script in a ",(0,s.kt)("inlineCode",{parentName:"p"},"hello.sc")," file that greets more properly:"),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=hello.sc",title:"hello.sc"},'def helloMessage(names: Seq[String]) = names match\n  case Nil =>\n    "Hello!"\n  case names =>\n    names.mkString("Hello: ", ", ", "!")\n\nprintln(helloMessage(args.toSeq))\n')),(0,s.kt)("p",null,"When that script is given no names, it prints ",(0,s.kt)("inlineCode",{parentName:"p"},'"Hello!"'),", and when it\u2019s given one or more names it prints the string that's created in the second ",(0,s.kt)("inlineCode",{parentName:"p"},"case")," statement. With Scala CLI we run the script like this:"),(0,s.kt)(o.v,{mdxType:"ChainedSnippets"},(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli hello.sc\n")),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre"},"Hello\n"))),(0,s.kt)("p",null,"To provide arguments to the script we add them after ",(0,s.kt)("inlineCode",{parentName:"p"},"--"),":"),(0,s.kt)(o.v,{mdxType:"ChainedSnippets"},(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli hello.sc -- Jenny Jake\n")),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre"},"Hello Jenny, Jake!\n"))),(0,s.kt)("p",null,"You may wonder what kind of Scala version was used under the hood. The answer is the latest stable version which was tested in Scala CLI. If you want to specify the Scala version you can use ",(0,s.kt)("inlineCode",{parentName:"p"},"-S")," or ",(0,s.kt)("inlineCode",{parentName:"p"},"--scala")," option. More about setting Scala version in the dedicated ",(0,s.kt)("a",{parentName:"p",href:"/docs/cookbooks/scala-versions"},"cookbook"),"."),(0,s.kt)("p",null,"Scala CLI offers many more features dedicated for scripting, as described in the ",(0,s.kt)("a",{parentName:"p",href:"/docs/guides/scripts"},"dedicated guide"),"."),(0,s.kt)("h2",{id:"dependencies"},"Dependencies"),(0,s.kt)("p",null,"Now let's build something more serious. For this example, it's best to start with some prototyping inside the REPL. We can start a REPL session by running ",(0,s.kt)("inlineCode",{parentName:"p"},"scala-cli repl"),". (If desired, you can also set the Scala version with ",(0,s.kt)("inlineCode",{parentName:"p"},"-S")," or ",(0,s.kt)("inlineCode",{parentName:"p"},"--scala"),".)"),(0,s.kt)("admonition",{type:"note"},(0,s.kt)("p",{parentName:"admonition"},"Scala CLI reuses most of its options across all of its commands.")),(0,s.kt)("p",null,"One of the main strengths of Scala is its ecosystem. Scala CLI is designed in a way to expose the Scala ecosystem to all usages of Scala, and running the REPL is no exception."),(0,s.kt)("p",null,"To demonstrate this, let's start prototyping with ",(0,s.kt)("a",{parentName:"p",href:"https://github.com/com-lihaoyi/os-lib"},"os-lib")," \u2014 a Scala interface to common OS filesystem and subprocess methods. To experiment with ",(0,s.kt)("inlineCode",{parentName:"p"},"os-lib")," in the REPL, we simply need to add the parameter ",(0,s.kt)("inlineCode",{parentName:"p"},"--dep com.lihaoyi::os-lib:0.7.8"),", as shown here:"),(0,s.kt)(o.v,{mdxType:"ChainedSnippets"},(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash",metastring:"ignore",ignore:!0},"scala-cli repl --dep com.lihaoyi::os-lib:0.7.8\n")),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-scala",metastring:"ignore",ignore:!0},"scala> os.pwd\nval res0: os.Path = ...\n\nscala> os.walk(os.pwd)\nval res1: IndexedSeq[os.Path] = ArraySeq(...)\n"))),(0,s.kt)("h2",{id:"a-project"},"A project"),(0,s.kt)("p",null,"Now it's time to write some logic, based on the prototyping we just did. We'll create a filter function to display all files with the given filename extension in the current directory."),(0,s.kt)("p",null,"For the consistency of our results, let's create a new directory and ",(0,s.kt)("inlineCode",{parentName:"p"},"cd")," to it:"),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash"},"mkdir scala-cli-getting-started\ncd scala-cli-getting-started\n")),(0,s.kt)("p",null,"Now we can write our logic in a file named ",(0,s.kt)("inlineCode",{parentName:"p"},"files.scala"),":"),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=files.scala",title:"files.scala"},'//> using lib "com.lihaoyi::os-lib:0.7.8"\n\ndef filesByExtension(\n  extension: String,\n  dir: os.Path = os.pwd): Seq[os.Path] =\n    os.walk(dir).filter { f =>\n      f.last.endsWith(s".$extension") && os.isFile(f)\n    }\n')),(0,s.kt)("p",null,"As you may have noticed, we specified a dependency within ",(0,s.kt)("inlineCode",{parentName:"p"},"files.scala")," using the ",(0,s.kt)("inlineCode",{parentName:"p"},"//> using lib com.lihaoyi::os-lib:0.7.8")," syntax. With Scala CLI, you can provide configuration information with ",(0,s.kt)("inlineCode",{parentName:"p"},"using")," directives \u2014 a dedicated syntax that can be embedded in any ",(0,s.kt)("inlineCode",{parentName:"p"},".scala")," file. For more details, see our dedicated ",(0,s.kt)("a",{parentName:"p",href:"/docs/guides/using-directives"},"guide for ",(0,s.kt)("inlineCode",{parentName:"a"},"using")," directives"),"."),(0,s.kt)("p",null,"Now let's check if our code compiles. We do that by running:"),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli compile .\n")),(0,s.kt)("p",null,"Notice that this time we didn\u2019t provide a path to single files, but rather used a directory; in this case, the current directory. For project-like use cases, we recommend providing directories rather than individual files. For most cases, specifying the current directory (",(0,s.kt)("inlineCode",{parentName:"p"},"."),") is a best choice."),(0,s.kt)("h2",{id:"ide-support"},"IDE support"),(0,s.kt)("p",null,"Some people are fine using the command line only, but most Scala developers use an IDE. To demonstrate this, let's open Metals with your favorite editor inside ",(0,s.kt)("inlineCode",{parentName:"p"},"scala-cli-getting-started")," directory:"),(0,s.kt)(o.m,{url:"/img/scala-cli-getting-started-1.mp4",mdxType:"GiflikeVideo"}),(0,s.kt)("p",null,"At the present moment, support for IntelliJ is often problematic. But know that we are working on making it as rock-solid as Metals."),(0,s.kt)("p",null,"Actually, in this case, we cheated a bit by running the compilation first. In order for Metals or IntelliJ to pick up a Scala CLI project, we need to generate a BSP connection detail file. Scala CLI generates these details by default every time ",(0,s.kt)("inlineCode",{parentName:"p"},"compile"),", ",(0,s.kt)("inlineCode",{parentName:"p"},"run"),", or ",(0,s.kt)("inlineCode",{parentName:"p"},"test")," are run. We also expose a ",(0,s.kt)("inlineCode",{parentName:"p"},"setup-ide")," command to manually control creation of the connection details file. For more information on this, see our ",(0,s.kt)("a",{parentName:"p",href:"/docs/guides/ide"},"IDE guide"),"."),(0,s.kt)("h2",{id:"tests"},"Tests"),(0,s.kt)("p",null,"With our IDE in place, how can we test if our code works correctly? The best way is to create a unit test. The simplest way to add a test using scala-cli is by creating a file whose name ends with ",(0,s.kt)("inlineCode",{parentName:"p"},".test.scala"),", such as ",(0,s.kt)("inlineCode",{parentName:"p"},"files.test.scala"),". (There are also other ways to mark source code files as containing a test, as described in ",(0,s.kt)("a",{parentName:"p",href:"/docs/commands/test#test-sources"},"tests guide"),".)"),(0,s.kt)("p",null,"We also need to add a test framework. Scala CLI support most popular test frameworks, and for this guide we will stick with ",(0,s.kt)("a",{parentName:"p",href:"https://scalameta.org/munit/"},"munit"),". To add a test framework, we just need an ordinary dependency, and once again we'll add that with the ",(0,s.kt)("inlineCode",{parentName:"p"},"using")," directive:"),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=files.test.scala",title:"files.test.scala"},'//> using lib "org.scalameta::munit:1.0.0-M1"\n\nclass TestSuite extends munit.FunSuite {\n  test("hello") {\n    val expected = Seq("files.scala", "files.test.scala")\n    val obtained = filesByExtension("scala").map(_.last)\n    assertEquals(obtained, expected)\n  }\n}\n')),(0,s.kt)("p",null,"Now we can run our tests at the command line:"),(0,s.kt)(o.v,{mdxType:"ChainedSnippets"},(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli test .\n")),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre"},"Compiling project (test, Scala 3.0.2, JVM)\nCompiled project (test, Scala 3.0.2, JVM)\nTestSuite:\n  + hello 0.058s\n"))),(0,s.kt)("p",null,"or directly within Metals:"),(0,s.kt)(o.m,{url:"/img/scala-cli-getting-started-2.mp4",mdxType:"GiflikeVideo"}),(0,s.kt)("h2",{id:"a-project-vol-2"},"A project, vol 2"),(0,s.kt)("p",null,"With our code ready and tested, now it's time to turn it into a command-line tool that counts files by their extension. For this we can write a simple script. A great feature of Scala CLI is that scripts and Scala sources can be mixed:"),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=countByExtension.sc",title:"countByExtension.sc"},'val (ext, directory) = args.toSeq match\n  case Seq(ext) => (ext, os.pwd)\n  case Seq(ext, directory) => (ext, os.Path(directory))\n  case other =>\n    println(s"Expected: `extension [directory]` but got: `${other.mkString(" ")}`")\n    sys.exit(1)\n\nval files = filesByExtension(ext, directory)\nfiles.map(_.relativeTo(directory)).foreach(println)\n')),(0,s.kt)("p",null,"As you probably noticed, we are using ",(0,s.kt)("inlineCode",{parentName:"p"},"os-lib")," in our script without any ",(0,s.kt)("inlineCode",{parentName:"p"},"using")," directive, How is that possible? The way it works is that configuration details provided by ",(0,s.kt)("inlineCode",{parentName:"p"},"using")," directives are global, and apply to all files. Since ",(0,s.kt)("inlineCode",{parentName:"p"},"files.scala")," and ",(0,s.kt)("inlineCode",{parentName:"p"},"countByExtension.sc")," are compiled together, the ",(0,s.kt)("inlineCode",{parentName:"p"},"using")," directives in ",(0,s.kt)("inlineCode",{parentName:"p"},"files.scala")," are used when compiling both files. (Note that defining a library dependency in more than one file is an anti-pattern.)"),(0,s.kt)("p",null,"Now let's run our code, looking for all files that end with the ",(0,s.kt)("inlineCode",{parentName:"p"},".scala")," extension:"),(0,s.kt)(o.v,{mdxType:"ChainedSnippets"},(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli . -- scala\n")),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre"},"files.scala\n.scala-build/project_940fb43dce/src_generated/main/countByExtension.scala\nfiles.test.scala\n"))),(0,s.kt)("p",null,"Seeing that output, you may wonder, why do we have an additional ",(0,s.kt)("inlineCode",{parentName:"p"},".scala")," file under the ",(0,s.kt)("inlineCode",{parentName:"p"},".scala-build")," dir? The way this works is that under the hood, Scala CLI sometimes needs to preprocess source code files \u2014 such as scripts. So these preprocessed files are created under the ",(0,s.kt)("inlineCode",{parentName:"p"},".scala-build")," directory, and then compiled from there."),(0,s.kt)("h2",{id:"packaging"},"Packaging"),(0,s.kt)("p",null,"We could stop here and call ",(0,s.kt)("inlineCode",{parentName:"p"},"scala-cli")," on our set of sources every time. Scala CLI uses caches aggressively, so rollup runs are reasonably fast \u2014 less than 1,500 milliseconds on tested machine \u2014 but sometimes this isn't fast enough, or shipping sources and compiling them may be not convenient."),(0,s.kt)("p",null,"For these use cases, Scala CLI offers means to package your project. For example, we can run this command to generate a thin, executable jar file, with the compiled code inside:"),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli package . -o countByExtension\n")),(0,s.kt)("p",null,"The default binary name is ",(0,s.kt)("inlineCode",{parentName:"p"},"app"),", so in this example we provide the ",(0,s.kt)("inlineCode",{parentName:"p"},"-o")," flag to make the name of the binary ",(0,s.kt)("inlineCode",{parentName:"p"},"countByExtension"),". Now we can run our project like this:"),(0,s.kt)("pre",null,(0,s.kt)("code",{parentName:"pre",className:"language-bash"},"./countByExtension scala\n")),(0,s.kt)("p",null,"This time it only took 350 milliseconds, so this is a big improvement. When you create a binary file (a runnable jar) like this, it's self-contained, and can be shipped to your colleagues or deployed."),(0,s.kt)("p",null,"We can reduce the startup time even further using ",(0,s.kt)("a",{parentName:"p",href:"/docs/guides/scala-native"},"Scala Native"),", or by packaging our application to other formats like ",(0,s.kt)("a",{parentName:"p",href:"/docs/commands/package#docker-container"},"Docker container"),", ",(0,s.kt)("a",{parentName:"p",href:"/docs/commands/package#assemblies"},"assembly"),", or even ",(0,s.kt)("a",{parentName:"p",href:"/docs/commands/package#os-specific-packages"},"OS-specific packages")," (.dep, .pkg, etc.). See those resources for more information."),(0,s.kt)("h2",{id:"summary"},"Summary"),(0,s.kt)("p",null,"With this guide we've only scratched the surface of what Scala CLI can do. For many more details, we've prepared a set of ",(0,s.kt)("a",{parentName:"p",href:"/docs/cookbooks/intro"},"cookbooks")," that showcase solutions to common problems, as well as a detailed set of ",(0,s.kt)("a",{parentName:"p",href:"/docs/guides/intro"},"guides")," for our ",(0,s.kt)("a",{parentName:"p",href:"/docs/commands/basics"},"commands"),"."),(0,s.kt)("p",null,"We also have a dedicated ",(0,s.kt)("a",{parentName:"p",href:"https://discord.gg/KzQdYkZZza"},"room on Scala discord")," where you can ask for help or discuss anything that's related to Scala CLI. For more in-depth discussions, we're using ",(0,s.kt)("a",{parentName:"p",href:"https://github.com/VirtusLab/scala-cli/discussions"},"Github discussions in our repo"),"; this is the best place to suggest a new feature or any improvements."))}h.isMDXComponent=!0}}]);