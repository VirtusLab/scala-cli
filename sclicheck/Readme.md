# Sclicheck - simple tool to verify scala-cli cookbooks

Sclicheck `[sklicheck]` is a simple command line tool to verify documentation.

It uses regexes under the hood so in some cases we may not parse the file properly.

Sclicheck extracts commands from `.md` file and then run this commands as defined within a file using a single workspace. Commands are not run in isolation by design. The whole point of the tool is to maintain a state of the current example and modify / test it using commands.

Currently following commands are supported:
 - [Write code](#write-code) - stores snippet into a file
 - [Run code](#run-code) - runs provided commands as a bash script
 - [Check output](#check-output) - check if lates optput contains provided patterns
 - [Clear](#clear) - clears all file in workspace

Usually, within a doc one want to first define a code, then run some commands (e.g. compilation) to test it and then check the output from last command (e.g. compilation).

Sclicheck accepts following arguments: `[--dest <dest>] [--step] [--stopAtFailure] <files>` where:
 - `<files>` - list of `.md` files or directories to check. In case of directories Sclicheck will recusievly check all `.md` files
 - `--dest <dest>`- Sclicheck after checking given file (`<name.md>`) store all of the generated sources as well as `<name.md>` (as `Readme.md`) file in `<dest>/<name>`. This is usefull to generate examples directly from documents
 - `--step` - stops after each command. Useful for debugging.
 - `--stopAtFailure` - stops after each failed command. Useful for debugging.

## Example

Let consider this simple document we want to check:

````md
# Testing cat command

Cat command can print a content of a file. Let's start with simple file

```md title=a.txt
A text
```

Let's read it using `cat`:

```bash
cat a.txt
```

<!-- Expected:
A text
-->

`cat` fails if file does not exists:

```bash fail
cat no_a_file
```
<!-- Expected:
no_a_file
-->
````

For the example above Sclicheck will:
 - write a file `a.txt` with `A text` as content
 - runs `cat a.txt` and store output (`A text`)
 - check if a patten `A text` exisits in output from last command (`A text`)
 - runs `cat no_a_file` (expecting that command will fail)
 - check if a patten `no_a_file` exisits in output from last command (`ls: cannot access 'no_a_file': No such file or directory`)

## Actions

### Write code

It extracts code to file in workspace for all code snippets marked with ` ```<language> title=<file-name> `

for example:

````
```scala title=A.scala
def a = 123
```
````

Will create (or override if exists) file `A.scala` with provided context. We support writing into subdirectories as well using `title=dir/file.ext`.

Sclicheck generates the sources for `.scala` and `.java` files in such a way that the lines with actual code matches the lines in provided .md files to make debugging easier.

**Important!**

Code block is ignored if any additional properties are passed to first line of a snippet.

To add a named code snippet that should be ignore provide any additional option like `ignore` for example:

````
```scala title=A.scala ignore
def a = 123
```
````

### Running bash scripts

It will run code snippets starting with ` ```bash ` for example:

```bash
scala-cli clean
scala-cli compile .
```

The output from last command is stored for following [check output](#check-output) commands.

We turn such snippet into a bash script so example below becomes:

```bash
#!/usr/bin/env bash

set -e

scala-cli clean
scala-cli compile .
```

Sclicheck expect that script return 0 but when `fail` is provided it expects failure (return non-zero exit code). Example:

````
```bash fail
ls non_exisiting_dir
```
````

**Important** Code block is ignored if any additional properties are passed to first line of a snippet.

To add a `bash` code snippet that should be ignored any additional option like `ignore` for example:

````
```bash ignore
ls non_exisiting_dir
```
````

# Check output

Sclicheck can check the output latest run command

For that we use html comments starting with:
    - `<!-- Expected-regex:` for regex pattern to match at least single line from last output
    - `<!-- Expected` for pattern that needs to exists in at least one line from last output

Followed by lines containing patterns/regexes, for example:

```
<!-- Expected-regex:
Using Scala version: 2.*
With care\.
-->
```

Sclicheck, for each provided pattern check if there is at least a one line in the output that contains the pattern (for non-regex) or matches the provided regex.

## Clear

In some cases we want to start with fresh context. For such cases Sclicheck provides a Clear command. It is defined as single line html comment containing single word `clear`:

`<!-- clear -->`