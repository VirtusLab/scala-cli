./mill -i show cli.nativeImage  

export PATH=out/cli/base-image/nativeImage/dest:$PATH

./mill -i scala docs/cookbooks/check.scala -- docs/cookbooks