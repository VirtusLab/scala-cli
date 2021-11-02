# Testing cat command

This is test document for Sclicheck

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