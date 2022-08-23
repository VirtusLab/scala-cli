"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[3813],{9705:function(e,t,a){a.d(t,{m:function(){return s},v:function(){return l}});var n=a(7294),i=a(2004);function l(e){var t=e.children;return n.createElement("div",{className:"runnable-command"},t)}function s(e){var t=e.url;return n.createElement(i.Z,{playing:!0,loop:!0,muted:!0,controls:!0,width:"100%",height:"",url:t})}},1889:function(e,t,a){a.r(t),a.d(t,{assets:function(){return m},contentTitle:function(){return c},default:function(){return u},frontMatter:function(){return r},metadata:function(){return p},toc:function(){return d}});var n=a(3117),i=a(102),l=(a(7294),a(3905)),s=a(9705),o=["components"],r={title:"Format",sidebar_position:15},c=void 0,p={unversionedId:"commands/fmt",id:"commands/fmt",title:"Format",description:"Scala CLI supports formatting your code using Scalafmt:",source:"@site/docs/commands/fmt.md",sourceDirName:"commands",slug:"/commands/fmt",permalink:"/docs/commands/fmt",draft:!1,editUrl:"https://github.com/Virtuslab/scala-cli/edit/main/website/docs/commands/fmt.md",tags:[],version:"current",sidebarPosition:15,frontMatter:{title:"Format",sidebar_position:15},sidebar:"tutorialSidebar",previous:{title:"IDE Setup",permalink:"/docs/commands/setup-ide"},next:{title:"Clean",permalink:"/docs/commands/clean"}},m={},d=[{value:"Scalafmt version and dialect",id:"scalafmt-version-and-dialect",level:3},{value:"Example 1",id:"example-1",level:4},{value:"Example 2",id:"example-2",level:4},{value:"Scalafmt options",id:"scalafmt-options",level:3},{value:"Excluding sources",id:"excluding-sources",level:3},{value:"How <code>.scalafmt.conf</code> file is generated",id:"how-scalafmtconf-file-is-generated",level:3}],f={toc:d};function u(e){var t=e.components,a=(0,i.Z)(e,o);return(0,l.kt)("wrapper",(0,n.Z)({},f,a,{components:t,mdxType:"MDXLayout"}),(0,l.kt)("p",null,"Scala CLI supports formatting your code using ",(0,l.kt)("a",{parentName:"p",href:"https://scalameta.org/scalafmt/"},"Scalafmt"),":"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli fmt\n")),(0,l.kt)("p",null,"Under the hood, ",(0,l.kt)("inlineCode",{parentName:"p"},"scala-cli")," downloads and runs Scalafmt on your code."),(0,l.kt)("p",null,"If you\u2019re setting up a continuous integration (CI) server, ",(0,l.kt)("inlineCode",{parentName:"p"},"scala-cli")," also has you covered.\nYou can check formatting correctness using a ",(0,l.kt)("inlineCode",{parentName:"p"},"--check")," flag:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli fmt --check\n")),(0,l.kt)("h3",{id:"scalafmt-version-and-dialect"},"Scalafmt version and dialect"),(0,l.kt)("p",null,"Scala CLI ",(0,l.kt)("inlineCode",{parentName:"p"},"fmt")," command supports passing the ",(0,l.kt)("inlineCode",{parentName:"p"},"scalafmt")," ",(0,l.kt)("strong",{parentName:"p"},"version")," and ",(0,l.kt)("strong",{parentName:"p"},"dialect")," directly from the command line, using the ",(0,l.kt)("inlineCode",{parentName:"p"},"--scalafmt-dialect")," and ",(0,l.kt)("inlineCode",{parentName:"p"},"--scalafmt-version")," options respectively:"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre"},"scala-cli fmt --scalafmt-dialect scala3 --scalafmt-version 3.5.8\n")),(0,l.kt)("p",null,"You can skip passing either of those, which will make Scala CLI infer a default value:"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"If a ",(0,l.kt)("inlineCode",{parentName:"li"},".scalafmt.conf")," file is present in the workspace and it has the field defined, the value will be read from there, unless explicitly specified with Scala CLI options."),(0,l.kt)("li",{parentName:"ul"},"Otherwise, the default ",(0,l.kt)("inlineCode",{parentName:"li"},"scalafmt")," ",(0,l.kt)("strong",{parentName:"li"},"version")," will be the latest one used by your Scala CLI version (so it is subject to change when updating Scala CLI). The default ",(0,l.kt)("strong",{parentName:"li"},"dialect")," will be inferred based on the Scala version (defined explicitly by ",(0,l.kt)("inlineCode",{parentName:"li"},"-S")," option, or default version if the option is not passed).")),(0,l.kt)("p",null,"It is possible to pass the configuration as a string directly from the command line, using ",(0,l.kt)("inlineCode",{parentName:"p"},"--scalafmt-conf-str")," option. If the configuration is passed this way, Scala CLI will behave exactly the same as if it found the specified configuration in the ",(0,l.kt)("inlineCode",{parentName:"p"},".scalafmt.conf")," file in the workspace."),(0,l.kt)("h4",{id:"example-1"},"Example 1"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-text",metastring:"title=.scalafmt.conf",title:".scalafmt.conf"},'version = "3.5.8"\nrunner.dialect = scala212\n')),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli fmt --scalafmt-dialect scala213\n")),(0,l.kt)("p",null,"For the setup above, ",(0,l.kt)("inlineCode",{parentName:"p"},"fmt")," will use:"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},'version="3.5.8"')," from the file"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"dialect=scala213"),", because passed ",(0,l.kt)("inlineCode",{parentName:"li"},"--scalafmt-dialect")," option overrides dialect found in the file")),(0,l.kt)("h4",{id:"example-2"},"Example 2"),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-text",metastring:"title=.scalafmt.conf",title:".scalafmt.conf"},'version = "2.7.5"\n')),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli fmt --scalafmt-version 3.5.8\n")),(0,l.kt)("p",null,"For the setup above, ",(0,l.kt)("inlineCode",{parentName:"p"},"fmt")," will use:"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},'version="3.5.8"'),", because passed ",(0,l.kt)("inlineCode",{parentName:"li"},"--scalafmt-version")," option overrides version from the file"),(0,l.kt)("li",{parentName:"ul"},(0,l.kt)("inlineCode",{parentName:"li"},"dialect=scala3"),", because dialect is neither passed as an option nor is it present in the configuration file, so it is inferred based on the Scala version; the Scala version wasn't explicitly specified in the command either, so it falls back to the default Scala version - the latest one, thus the resulting dialect is ",(0,l.kt)("inlineCode",{parentName:"li"},"scala3"),". ")),(0,l.kt)("h3",{id:"scalafmt-options"},"Scalafmt options"),(0,l.kt)("p",null,"It is possible to pass native ",(0,l.kt)("inlineCode",{parentName:"p"},"scalafmt")," options with the ",(0,l.kt)("inlineCode",{parentName:"p"},"-F")," (short for ",(0,l.kt)("inlineCode",{parentName:"p"},"--scalafmt-arg"),"), for example:"),(0,l.kt)(s.v,{mdxType:"ChainedSnippets"},(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli fmt -F --version\n")),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-text"},"scalafmt 3.5.8\n"))),(0,l.kt)("p",null,"For the available options please refer to ",(0,l.kt)("inlineCode",{parentName:"p"},"scalafmt")," help, which can be viewed with the ",(0,l.kt)("inlineCode",{parentName:"p"},"--scalafmt-help")," option (which\nis just an alias for ",(0,l.kt)("inlineCode",{parentName:"p"},"-F --help"),"):"),(0,l.kt)(s.v,{mdxType:"ChainedSnippets"},(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli fmt --scalafmt-help\n")),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-text"},"scalafmt 3.5.8\nUsage: scalafmt [options] [<file>...]\n\n  -h, --help               prints this usage text\n  -v, --version            print version \n(...)\n"))),(0,l.kt)("h3",{id:"excluding-sources"},"Excluding sources"),(0,l.kt)("p",null,"Because of the way Scala CLI invokes ",(0,l.kt)("inlineCode",{parentName:"p"},"scalafmt")," under the hood, sources are always being passed to it explicitly. This\nin turn means that regardless of how the sources were passed, ",(0,l.kt)("inlineCode",{parentName:"p"},"scalafmt")," exclusion paths (the ",(0,l.kt)("inlineCode",{parentName:"p"},"project.excludePaths"),")\nwould be ignored. In order to prevent that from happening, the ",(0,l.kt)("inlineCode",{parentName:"p"},"--respect-project-filters")," option is set to ",(0,l.kt)("inlineCode",{parentName:"p"},"true")," by\ndefault."),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-text",metastring:"title=.scalafmt.conf",title:".scalafmt.conf"},'version = "3.5.8"\nrunner.dialect = scala3\nproject {\n  includePaths = [\n    "glob:**.scala",\n    "regex:.*\\\\.sc"\n  ]\n  excludePaths = [\n    "glob:**/should/not/format/**.scala"\n  ]\n}\n')),(0,l.kt)(s.v,{mdxType:"ChainedSnippets"},(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli fmt . --check\n")),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-text"},"All files are formatted with scalafmt :)\n"))),(0,l.kt)("p",null,"You can explicitly set it to false if you want to disregard any filters configured in the ",(0,l.kt)("inlineCode",{parentName:"p"},"project.excludePaths")," setting\nin your ",(0,l.kt)("inlineCode",{parentName:"p"},".scalafmt.conf")," for any reason."),(0,l.kt)(s.v,{mdxType:"ChainedSnippets"},(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli fmt . --check --respect-project-filters=false\n")),(0,l.kt)("pre",null,(0,l.kt)("code",{parentName:"pre",className:"language-text"},"--- a/.../should/not/format/ShouldNotFormat.scala\n+++ b/.../should/not/format/ShouldNotFormat.scala\n@@ -1,3 +1,3 @@\n class ShouldNotFormat {\n-                       println()\n+  println()\n }\n"))),(0,l.kt)("h3",{id:"how-scalafmtconf-file-is-generated"},"How ",(0,l.kt)("inlineCode",{parentName:"h3"},".scalafmt.conf")," file is generated"),(0,l.kt)("p",null,"The Scala CLI ",(0,l.kt)("inlineCode",{parentName:"p"},"fmt")," command runs ",(0,l.kt)("inlineCode",{parentName:"p"},"scalafmt")," under the hood, which ",(0,l.kt)("em",{parentName:"p"},"normally")," requires ",(0,l.kt)("inlineCode",{parentName:"p"},".scalafmt.conf")," configuration file with explicitly specified ",(0,l.kt)("strong",{parentName:"p"},"version")," and ",(0,l.kt)("strong",{parentName:"p"},"dialect")," fields. The way it is handled by Scala CLI is as follows:"),(0,l.kt)("p",null,"At the beginning ",(0,l.kt)("inlineCode",{parentName:"p"},"fmt")," looks for the configuration inside the file specified in the ",(0,l.kt)("inlineCode",{parentName:"p"},"--scalafmt-conf")," option. If the option is not passed or the file doesn't exist, ",(0,l.kt)("inlineCode",{parentName:"p"},"fmt")," looks for the existing configuration file inside ",(0,l.kt)("strong",{parentName:"p"},"current workspace")," directory. If the file is still not found, ",(0,l.kt)("inlineCode",{parentName:"p"},"fmt")," looks for it inside ",(0,l.kt)("strong",{parentName:"p"},"git root")," directory. There are 3 possible cases:"),(0,l.kt)("ol",null,(0,l.kt)("li",{parentName:"ol"},"Configuration file with the specified version and dialect is found."),(0,l.kt)("li",{parentName:"ol"},"Configuration file is found, but it doesn't have specified version or dialect."),(0,l.kt)("li",{parentName:"ol"},"Configuration file is not found.")),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"In the ",(0,l.kt)("strong",{parentName:"li"},"first")," case ",(0,l.kt)("inlineCode",{parentName:"li"},"fmt")," uses the found ",(0,l.kt)("inlineCode",{parentName:"li"},".scalafmt.conf")," file to run ",(0,l.kt)("inlineCode",{parentName:"li"},"scalafmt"),"."),(0,l.kt)("li",{parentName:"ul"},"In the ",(0,l.kt)("strong",{parentName:"li"},"second")," case ",(0,l.kt)("inlineCode",{parentName:"li"},"fmt")," creates a ",(0,l.kt)("inlineCode",{parentName:"li"},".scalafmt.conf")," file inside the ",(0,l.kt)("inlineCode",{parentName:"li"},".scala-build")," directory. Content of the previously found file is copied into the newly created file, missing parameters are ",(0,l.kt)("a",{parentName:"li",href:"/docs/commands/fmt#scalafmt-version-and-dialect"},"inferred")," and written into the same file. Created file is used to run ",(0,l.kt)("inlineCode",{parentName:"li"},"scalafmt"),". "),(0,l.kt)("li",{parentName:"ul"},"In the ",(0,l.kt)("strong",{parentName:"li"},"third")," case ",(0,l.kt)("inlineCode",{parentName:"li"},"fmt")," creates a ",(0,l.kt)("inlineCode",{parentName:"li"},".scalafmt.conf")," file inside the ",(0,l.kt)("inlineCode",{parentName:"li"},".scala-build")," directory, writes ",(0,l.kt)("a",{parentName:"li",href:"/docs/commands/fmt#scalafmt-version-and-dialect"},"inferred")," version and dialect into it and uses it to run ",(0,l.kt)("inlineCode",{parentName:"li"},"scalafmt"),".")),(0,l.kt)("p",null,"If the ",(0,l.kt)("inlineCode",{parentName:"p"},"--save-scalafmt-conf")," option is passed, then ",(0,l.kt)("inlineCode",{parentName:"p"},"fmt")," command behaves as follows:"),(0,l.kt)("ul",null,(0,l.kt)("li",{parentName:"ul"},"In the ",(0,l.kt)("strong",{parentName:"li"},"first")," case ",(0,l.kt)("inlineCode",{parentName:"li"},"fmt")," uses the found ",(0,l.kt)("inlineCode",{parentName:"li"},".scalafmt.conf")," file to run ",(0,l.kt)("inlineCode",{parentName:"li"},"scalafmt"),"."),(0,l.kt)("li",{parentName:"ul"},"In the ",(0,l.kt)("strong",{parentName:"li"},"second")," case ",(0,l.kt)("inlineCode",{parentName:"li"},"fmt")," ",(0,l.kt)("a",{parentName:"li",href:"/docs/commands/fmt#scalafmt-version-and-dialect"},"infers")," missing parameters, writes them directly into the previously found file and then uses this file to run ",(0,l.kt)("inlineCode",{parentName:"li"},"scalafmt"),"."),(0,l.kt)("li",{parentName:"ul"},"In the ",(0,l.kt)("strong",{parentName:"li"},"third")," case ",(0,l.kt)("inlineCode",{parentName:"li"},"fmt")," creates a ",(0,l.kt)("inlineCode",{parentName:"li"},".scalafmt.conf")," file in the current workspace directory, writes ",(0,l.kt)("a",{parentName:"li",href:"/docs/commands/fmt#scalafmt-version-and-dialect"},"inferred")," version and dialect into it and uses it to run ",(0,l.kt)("inlineCode",{parentName:"li"},"scalafmt"),".")),(0,l.kt)("admonition",{type:"note"},(0,l.kt)("p",{parentName:"admonition"},"If the configuration is passed in the ",(0,l.kt)("inlineCode",{parentName:"p"},"--scalafmt-conf-str")," option, Scala CLI will behave exactly the same as if it found the specified configuration in a ",(0,l.kt)("inlineCode",{parentName:"p"},".scalafmt.conf")," file in the workspace.")))}u.isMDXComponent=!0}}]);