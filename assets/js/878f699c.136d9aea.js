"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[5672],{3905:function(e,a,t){t.d(a,{Zo:function(){return c},kt:function(){return k}});var n=t(7294);function l(e,a,t){return a in e?Object.defineProperty(e,a,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[a]=t,e}function i(e,a){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);a&&(n=n.filter((function(a){return Object.getOwnPropertyDescriptor(e,a).enumerable}))),t.push.apply(t,n)}return t}function r(e){for(var a=1;a<arguments.length;a++){var t=null!=arguments[a]?arguments[a]:{};a%2?i(Object(t),!0).forEach((function(a){l(e,a,t[a])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):i(Object(t)).forEach((function(a){Object.defineProperty(e,a,Object.getOwnPropertyDescriptor(t,a))}))}return e}function o(e,a){if(null==e)return{};var t,n,l=function(e,a){if(null==e)return{};var t,n,l={},i=Object.keys(e);for(n=0;n<i.length;n++)t=i[n],a.indexOf(t)>=0||(l[t]=e[t]);return l}(e,a);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(n=0;n<i.length;n++)t=i[n],a.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(l[t]=e[t])}return l}var s=n.createContext({}),p=function(e){var a=n.useContext(s),t=a;return e&&(t="function"==typeof e?e(a):r(r({},a),e)),t},c=function(e){var a=p(e.components);return n.createElement(s.Provider,{value:a},e.children)},m={inlineCode:"code",wrapper:function(e){var a=e.children;return n.createElement(n.Fragment,{},a)}},u=n.forwardRef((function(e,a){var t=e.components,l=e.mdxType,i=e.originalType,s=e.parentName,c=o(e,["components","mdxType","originalType","parentName"]),u=p(t),k=l,d=u["".concat(s,".").concat(k)]||u[k]||m[k]||i;return t?n.createElement(d,r(r({ref:a},c),{},{components:t})):n.createElement(d,r({ref:a},c))}));function k(e,a){var t=arguments,l=a&&a.mdxType;if("string"==typeof e||l){var i=t.length,r=new Array(i);r[0]=u;var o={};for(var s in a)hasOwnProperty.call(a,s)&&(o[s]=a[s]);o.originalType=e,o.mdxType="string"==typeof e?e:l,r[1]=o;for(var p=2;p<i;p++)r[p]=t[p];return n.createElement.apply(null,r)}return n.createElement.apply(null,t)}u.displayName="MDXCreateElement"},4109:function(e,a,t){t.r(a),t.d(a,{assets:function(){return c},contentTitle:function(){return s},default:function(){return k},frontMatter:function(){return o},metadata:function(){return p},toc:function(){return m}});var n=t(3117),l=t(102),i=(t(7294),t(3905)),r=["components"],o={title:"Package",sidebar_position:17},s=void 0,p={unversionedId:"commands/package",id:"commands/package",title:"Package",description:"The package command can package your Scala code in various formats, such as:",source:"@site/docs/commands/package.md",sourceDirName:"commands",slug:"/commands/package",permalink:"/docs/commands/package",draft:!1,editUrl:"https://github.com/Virtuslab/scala-cli/edit/main/website/docs/commands/package.md",tags:[],version:"current",sidebarPosition:17,frontMatter:{title:"Package",sidebar_position:17},sidebar:"tutorialSidebar",previous:{title:"Clean",permalink:"/docs/commands/clean"},next:{title:"Doc",permalink:"/docs/commands/doc"}},c={},m=[{value:"Default package format",id:"default-package-format",level:2},{value:"Library JARs",id:"library-jars",level:2},{value:"Assemblies",id:"assemblies",level:2},{value:"Docker container",id:"docker-container",level:2},{value:"Building Docker container from base image",id:"building-docker-container-from-base-image",level:3},{value:"Scala.js",id:"scalajs",level:2},{value:"Native image",id:"native-image",level:2},{value:"Scala Native",id:"scala-native",level:2},{value:"OS-specific packages",id:"os-specific-packages",level:2},{value:"Debian",id:"debian",level:3},{value:"Mandatory arguments",id:"mandatory-arguments",level:4},{value:"Optional arguments",id:"optional-arguments",level:4},{value:"RedHat",id:"redhat",level:3},{value:"Mandatory arguments",id:"mandatory-arguments-1",level:4},{value:"Optional arguments",id:"optional-arguments-1",level:4},{value:"macOS (PKG)",id:"macos-pkg",level:3},{value:"Mandatory arguments",id:"mandatory-arguments-2",level:4},{value:"Optional arguments",id:"optional-arguments-2",level:4},{value:"Windows",id:"windows",level:3},{value:"Mandatory arguments",id:"mandatory-arguments-3",level:4},{value:"Optional arguments",id:"optional-arguments-3",level:4},{value:"Using directives",id:"using-directives",level:2},{value:"packaging.packageType",id:"packagingpackagetype",level:3},{value:"packaging.output",id:"packagingoutput",level:3}],u={toc:m};function k(e){var a=e.components,t=(0,l.Z)(e,r);return(0,i.kt)("wrapper",(0,n.Z)({},u,t,{components:a,mdxType:"MDXLayout"}),(0,i.kt)("p",null,"The ",(0,i.kt)("inlineCode",{parentName:"p"},"package")," command can package your Scala code in various formats, such as:"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#default-package-format"},"lightweight launcher JARs")),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#library-jars"},"standard library JARs")),(0,i.kt)("li",{parentName:"ul"},"so called ",(0,i.kt)("a",{parentName:"li",href:"#assemblies"},'"assemblies" or "fat JARs"')),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#docker-container"},"docker container")),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#scalajs"},"JavaScript files")," for Scala.js code"),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#native-image"},"GraalVM native image executables")),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#scala-native"},"native executables")," for Scala Native code"),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#os-specific-packages"},"OS-specific formats"),", such as deb or rpm (Linux), pkg (macOS), or MSI (Windows)")),(0,i.kt)("h2",{id:"default-package-format"},"Default package format"),(0,i.kt)("p",null,"The default package format writes a ",(0,i.kt)("em",{parentName:"p"},"lightweight launcher JAR"),', like the "bootstrap" JAR files ',(0,i.kt)("a",{parentName:"p",href:"https://get-coursier.io/docs/cli-bootstrap#bootstraps"},"generated by coursier"),".\nThese JARs tend to have a small size (mostly containing only the byte code from your own sources),\ncan be generated fast,\nand download their dependencies upon first launch via ",(0,i.kt)("a",{parentName:"p",href:"https://get-coursier.io"},"coursier"),"."),(0,i.kt)("p",null,"Such JARs can be copied to other machines, and will run fine there.\nTheir only requirement is that the ",(0,i.kt)("inlineCode",{parentName:"p"},"java")," command needs to be available in the ",(0,i.kt)("inlineCode",{parentName:"p"},"PATH"),":"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=Hello.scala",title:"Hello.scala"},'object Hello {\n  def main(args: Array[String]): Unit =\n    println("Hello")\n}\n')),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli package Hello.scala -o hello\n./hello\n# Hello\n")),(0,i.kt)("h2",{id:"library-jars"},"Library JARs"),(0,i.kt)("p",null,(0,i.kt)("em",{parentName:"p"},"Library JARs")," are suitable if you plan to put the resulting JAR in a class path, rather than running it as is.\nThese follow the same format as the JARs of libraries published to Maven Central:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=MyLibrary.scala",title:"MyLibrary.scala"},'package mylib\n\nclass MyLibrary {\n  def message = "Hello"\n}\n')),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash"},'scala-cli package MyLibrary.scala -o my-library.jar --library\njavap -cp my-library.jar mylib.MyLibrary\n# Compiled from "MyLibrary.scala"\n# public class mylib.MyLibrary {\n#   public java.lang.String message();\n#   public mylib.MyLibrary();\n# }\n')),(0,i.kt)("h2",{id:"assemblies"},"Assemblies"),(0,i.kt)("p",null,(0,i.kt)("em",{parentName:"p"},"Assemblies")," blend your dependencies and your sources' byte code together in a single JAR file.\nAs a result, assemblies can be run as is, just like ",(0,i.kt)("a",{parentName:"p",href:"#default-package-format"},"bootstraps"),", but don't need to download\nanything upon first launch.\nBecause of that, assemblies also tend to be bigger, and somewhat slower to generate:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=Hello.scala",title:"Hello.scala"},'object Hello {\n  def main(args: Array[String]): Unit =\n    println("Hello")\n}\n')),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli package Hello.scala -o hello --assembly\n./hello\n# Hello\n")),(0,i.kt)("h2",{id:"docker-container"},"Docker container"),(0,i.kt)("p",null,"Scala CLI can create an executable application and package it into a docker image."),(0,i.kt)("p",null,"For example, here\u2019s an application that will be executed in a docker container:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=HelloDocker.scala",title:"HelloDocker.scala"},'object HelloDocker extends App {\n  println("Hello from Docker")\n}\n')),(0,i.kt)("p",null,"Passing ",(0,i.kt)("inlineCode",{parentName:"p"},"--docker")," to the ",(0,i.kt)("inlineCode",{parentName:"p"},"package")," sub-command generates a docker image.\nThe docker image name parameter ",(0,i.kt)("inlineCode",{parentName:"p"},"--docker-image-repository")," is mandatory."),(0,i.kt)("p",null,"The following command generates a ",(0,i.kt)("inlineCode",{parentName:"p"},"hello-docker")," image with the ",(0,i.kt)("inlineCode",{parentName:"p"},"latest")," tag:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli package --docker HelloDocker.scala --docker-image-repository hello-docker\n")),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash"},"docker run hello-docker\n# Hello from Docker\n")),(0,i.kt)("p",null,"You can also create Docker images for Scala.js and Scala Native applications.\nThe following command shows how to create a Docker image (",(0,i.kt)("inlineCode",{parentName:"p"},"--docker"),") for a Scala.js (",(0,i.kt)("inlineCode",{parentName:"p"},"--js"),") application:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli package --js --docker HelloDocker.scala --docker-image-repository hello-docker\n")),(0,i.kt)("p",null,"Packaging Scala Native applications to a Docker image is only supported on Linux."),(0,i.kt)("p",null,"The following command shows how to do that:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash",metastring:"ignore",ignore:!0},"scala-cli package --native --docker HelloDocker.scala --docker-image-repository hello-docker\n")),(0,i.kt)("h3",{id:"building-docker-container-from-base-image"},"Building Docker container from base image"),(0,i.kt)("p",null,(0,i.kt)("inlineCode",{parentName:"p"},"--docker-from")," lets you specify your base docker image."),(0,i.kt)("p",null,"The following command generate a ",(0,i.kt)("inlineCode",{parentName:"p"},"hello-docker")," image using base image ",(0,i.kt)("inlineCode",{parentName:"p"},"openjdk:11")),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash",metastring:"ignore",ignore:!0},"scala-cli package --docker HelloDocker.scala --docker-from openjdk:11 --docker-image-repository hello-docker\n")),(0,i.kt)("h2",{id:"scalajs"},"Scala.js"),(0,i.kt)("p",null,"Packaging Scala.js applications results in a ",(0,i.kt)("inlineCode",{parentName:"p"},".js")," file, which can be run with ",(0,i.kt)("inlineCode",{parentName:"p"},"node"),":"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=HelloJs.scala",title:"HelloJs.scala"},'object Hello {\n  def main(args: Array[String]): Unit =\n    println("Hello")\n}\n')),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli package --js HelloJs.scala -o hello.js\nnode hello.js\n# Hello\n")),(0,i.kt)("p",null,"Note that Scala CLI doesn't offer the ability to link the resulting JavaScript with linkers, such as Webpack (yet)."),(0,i.kt)("h2",{id:"native-image"},"Native image"),(0,i.kt)("p",null,(0,i.kt)("a",{parentName:"p",href:"https://www.graalvm.org/22.0/reference-manual/native-image/"},"GraalVM native image"),"\nmakes it possible to build native executables out of JVM applications. It can\nbe used from Scala CLI to build native executables for Scala applications."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=Hello.scala",title:"Hello.scala"},'object Hello {\n  def main(args: Array[String]): Unit =\n    println("Hello")\n}\n')),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli package Hello.scala -o hello --native-image\n./hello\n# Hello\n")),(0,i.kt)("p",null,"Note that Scala CLI automatically downloads and unpacks a GraalVM distribution\nusing the ",(0,i.kt)("a",{parentName:"p",href:"https://get-coursier.io/docs/cli-java"},"JVM management capabilities of coursier"),"."),(0,i.kt)("p",null,"Several options can be passed to adjust the GraalVM version used by Scala CLI:"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("inlineCode",{parentName:"li"},"--graalvm-jvm-id")," accepts a JVM identifier, such as ",(0,i.kt)("inlineCode",{parentName:"li"},"graalvm-java17:22.0.0")," or ",(0,i.kt)("inlineCode",{parentName:"li"},"graalvm-java17:21")," (short versions accepted)."),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("inlineCode",{parentName:"li"},"--graalvm-java-version")," makes it possible to specify only a target Java version, such as ",(0,i.kt)("inlineCode",{parentName:"li"},"11")," or ",(0,i.kt)("inlineCode",{parentName:"li"},"17")," (note that only specific Java versions may be supported by the default GraalVM version that Scala CLI picks)"),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("inlineCode",{parentName:"li"},"--graalvm-version")," makes it possible to specify only a GraalVM version, such as ",(0,i.kt)("inlineCode",{parentName:"li"},"22.0.0")," or ",(0,i.kt)("inlineCode",{parentName:"li"},"21")," (short versions accepted)")),(0,i.kt)("h2",{id:"scala-native"},"Scala Native"),(0,i.kt)("p",null,"Packaging a Scala Native application results in a native executable:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala",metastring:"title=HelloNative.scala",title:"HelloNative.scala"},'object Hello {\n  def main(args: Array[String]): Unit =\n    println("Hello")\n}\n')),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash"},"scala-cli package --native HelloNative.scala -S 2.13.6 -o hello\nfile hello\n# hello: Mach-O 64-bit executable x86_64\n./hello\n# Hello\n")),(0,i.kt)("h2",{id:"os-specific-packages"},"OS-specific packages"),(0,i.kt)("p",null,"Scala CLI also lets you package Scala code as OS-specific packages.\nThis feature is somewhat experimental, and supports the following formats, provided they're compatible with the operating system you're running ",(0,i.kt)("inlineCode",{parentName:"p"},"scala-cli")," on:"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#debian"},"DEB")," (Linux)"),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#redhat"},"RPM")," (Linux)"),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#macos-pkg"},"PKG")," (macOS)"),(0,i.kt)("li",{parentName:"ul"},(0,i.kt)("a",{parentName:"li",href:"#windows"},"MSI")," (Windows)")),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-scala",metastring:"Hello.scala","Hello.scala":!0},'object Hello {\n  def main(args: Array[String]): Unit =\n    println("Hello")\n}\n')),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash",metastring:"ignore",ignore:!0},"scala-cli package --deb Hello.scala -o hello.deb\nfile hello\n# hello: Mach-O 64-bit executable x86_64\n./hello\n# Hello\n")),(0,i.kt)("h3",{id:"debian"},"Debian"),(0,i.kt)("p",null,"DEB is the package format for the Debian Linux distribution.\nTo build a Debian package, you will need to have ",(0,i.kt)("a",{parentName:"p",href:"http://manpages.ubuntu.com/manpages/trusty/pl/man1/dpkg-deb.1.html"},(0,i.kt)("inlineCode",{parentName:"a"},"dpkg-deb"))," installed."),(0,i.kt)("p",null,"Example:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash",metastring:"ignore",ignore:!0},"scala-cli package --deb --output 'path.deb' Hello.scala\n")),(0,i.kt)("h4",{id:"mandatory-arguments"},"Mandatory arguments"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"version"),(0,i.kt)("li",{parentName:"ul"},"maintainer"),(0,i.kt)("li",{parentName:"ul"},"description"),(0,i.kt)("li",{parentName:"ul"},"output-path")),(0,i.kt)("h4",{id:"optional-arguments"},"Optional arguments"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"force"),(0,i.kt)("li",{parentName:"ul"},"launcher-app"),(0,i.kt)("li",{parentName:"ul"},"debian-conflicts"),(0,i.kt)("li",{parentName:"ul"},"debian-dependencies"),(0,i.kt)("li",{parentName:"ul"},"architecture")),(0,i.kt)("h3",{id:"redhat"},"RedHat"),(0,i.kt)("p",null,"RPM is the software package format for RedHat distributions.\nTo build a RedHat Package, you will need to have ",(0,i.kt)("a",{parentName:"p",href:"https://linux.die.net/man/8/rpmbuild"},(0,i.kt)("inlineCode",{parentName:"a"},"rpmbuild"))," installed."),(0,i.kt)("p",null,"Example:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash",metastring:"ignore",ignore:!0},"scala-cli package --rpm --output 'path.rpm' Hello.scala\n")),(0,i.kt)("h4",{id:"mandatory-arguments-1"},"Mandatory arguments"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"version"),(0,i.kt)("li",{parentName:"ul"},"description"),(0,i.kt)("li",{parentName:"ul"},"license"),(0,i.kt)("li",{parentName:"ul"},"output-path")),(0,i.kt)("h4",{id:"optional-arguments-1"},"Optional arguments"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"force"),(0,i.kt)("li",{parentName:"ul"},"launcher-app"),(0,i.kt)("li",{parentName:"ul"},"release"),(0,i.kt)("li",{parentName:"ul"},"rpm-architecture")),(0,i.kt)("h3",{id:"macos-pkg"},"macOS (PKG)"),(0,i.kt)("p",null,"PKG is a software package format for macOS.\nTo build a PKG you will need to have ",(0,i.kt)("a",{parentName:"p",href:"https://www.unix.com/man-page/osx/1/pkgbuild/"},(0,i.kt)("inlineCode",{parentName:"a"},"pkgbuild"))," installed."),(0,i.kt)("p",null,"Example:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-bash",metastring:"ignore",ignore:!0},"`scala-cli package --pkg --output 'path.pkg` Hello.scala\n")),(0,i.kt)("h4",{id:"mandatory-arguments-2"},"Mandatory arguments"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"version"),(0,i.kt)("li",{parentName:"ul"},"identifier"),(0,i.kt)("li",{parentName:"ul"},"output-path")),(0,i.kt)("h4",{id:"optional-arguments-2"},"Optional arguments"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"force"),(0,i.kt)("li",{parentName:"ul"},"launcher-app")),(0,i.kt)("h3",{id:"windows"},"Windows"),(0,i.kt)("p",null,"MSI is a software package format for Windows.\nTo build an MSI installer, you will need to have ",(0,i.kt)("a",{parentName:"p",href:"https://wixtoolset.org/"},(0,i.kt)("inlineCode",{parentName:"a"},"WIX Toolset"))," installed."),(0,i.kt)("p",null,"Example:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-cmd"},"scala-cli package --msi --output path.msi Hello.scala\n")),(0,i.kt)("h4",{id:"mandatory-arguments-3"},"Mandatory arguments"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"version"),(0,i.kt)("li",{parentName:"ul"},"maintainer"),(0,i.kt)("li",{parentName:"ul"},"licence-path"),(0,i.kt)("li",{parentName:"ul"},"product-name"),(0,i.kt)("li",{parentName:"ul"},"output-path")),(0,i.kt)("h4",{id:"optional-arguments-3"},"Optional arguments"),(0,i.kt)("ul",null,(0,i.kt)("li",{parentName:"ul"},"force"),(0,i.kt)("li",{parentName:"ul"},"launcher-app"),(0,i.kt)("li",{parentName:"ul"},"exit-dialog"),(0,i.kt)("li",{parentName:"ul"},"logo-path")),(0,i.kt)("h2",{id:"using-directives"},"Using directives"),(0,i.kt)("p",null,"Instead of passing the ",(0,i.kt)("inlineCode",{parentName:"p"},"package")," options directly from bash, it is possible to pass some of them with ",(0,i.kt)("a",{parentName:"p",href:"/docs/guides/using-directives"},"using directives"),"."),(0,i.kt)("h3",{id:"packagingpackagetype"},"packaging.packageType"),(0,i.kt)("p",null,"This using directive makes it possible to define the type of the package generated by the ",(0,i.kt)("inlineCode",{parentName:"p"},"package")," command. For example:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre"},'//> using packaging.packageType "assembly"\n')),(0,i.kt)("p",null,"Available types: ",(0,i.kt)("inlineCode",{parentName:"p"},"assembly"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"raw-assembly"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"bootstrap"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"library"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"source"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"doc"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"spark"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"js"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"native"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"docker"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"graalvm"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"deb"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"dmg"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"pkg"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"rpm"),", ",(0,i.kt)("inlineCode",{parentName:"p"},"msi"),"."),(0,i.kt)("h3",{id:"packagingoutput"},"packaging.output"),(0,i.kt)("p",null,"This using directive makes it possible to define the destination path of the package generated by the ",(0,i.kt)("inlineCode",{parentName:"p"},"package")," command. For example:"),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre"},'//> using packaging.output "foo"\n')),(0,i.kt)("p",null,"The using directive above makes it possible to create a package named ",(0,i.kt)("inlineCode",{parentName:"p"},"foo")," inside the current directory."))}k.isMDXComponent=!0}}]);