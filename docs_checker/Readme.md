# Docs checker - simple tool to verify scala-cli cookbooks

Docs checker tests documentation is using simple regexes to run following actions:
 - extract/override code snippet to file in workspace for snippets starting with ````scala title=<file-name>` for example:

````
```bash
scala-cli ScalaVersion.scala
```
````
    
 - run scala-cli commands (or any commands) for snippets starting with ````bash` for example:

````
```scala title=ScalaVersion.scala
object ScalaVersion extends App 
```
````

 - check the output latest run for html comments starting with: 
    - `<!-- Expected-regex:` for regex pattern to match at least single line from last output
    - `<!-- Expected` for pattern that needs to exists in at least one line from last output
Followed by lines containing patterns/regexes, for example:

```
<!-- Expected-regex:
Using Scala version: 2.*
With care\.
-->
```

Docs checker simply parses .md file from top to bottom and applies command in order. 
Output is not perfect however we give hints where is the problematic snippet defined in markdown file. 
For compilation errors, we are defining scala files in a way so the lines matches lines in .md file.

Docs checker, process directories recursively, ignoring directories and files starting with `.`.