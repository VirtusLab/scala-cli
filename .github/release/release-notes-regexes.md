## PR link
Find: `in https\:\/\/github\.com\/VirtusLab\/scala\-cli\/pull\/(.*?)$` </br>
Replace: `in [#$1](https://github.com/VirtusLab/scala-cli/pull/$1)`

## Contributor link
Find: `by @(.*?) in` </br>
Replace: `by [@$1](https://github.com/$1) in`

## New contributor link
Find: `@(.*?) made` </br>
Replace: `[@$1](https://github.com/$1) made`

## No GH contributor link
Find: `by \[@(.*?).\(.*\) in` </br>
Replace: `by @$1 in`