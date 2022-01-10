#!/usr/bin/env bash
set -e

dest="$(pwd)/out/sclicheck/bin"

./mill copyTo cli.launcher "$dest/scala-cli.sh"

# work around sh issues (sh ignores PATH entries with '+' or '%', and the cs-provided Java 17 entry has one)
# so we try to run the scala-cli launcher with bash instead
cat > "$dest/scala-cli" << EOF
#!/usr/bin/env bash
exec bash "$dest/scala-cli.sh"
EOF
chmod +x "$dest/scala-cli"

echo "Adding $dest to PATH"
export PATH="$dest:$PATH"
ls "$dest"

if [ $# -eq 0 ]
  then
    toCheck=("website/docs/cookbooks" "website/docs/commands")
  else
    toCheck=("$@")
fi

# adding --resource-dirs is a hack to get file watching for free on .md files
scala-cli sclicheck/sclicheck.scala docs -- "${toCheck[@]}"
