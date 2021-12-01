# Tooling to generate nice gifs used in documentation

Recordings are possible using https://github.com/paxtonhare/demo-magic licensed under MIT Licence

## How does it work?

Our animated svgs are compose of scenarios built using [demo-magic](https://github.com/paxtonhare/demo-magic) and then recorded using [asciinema](https://asciinema.org/) to .json files to be finally rendered to animated svg files using [svg_rendrer_cli](https://github.com/marionebl/svg-term-cli) 

## How to (re)create new gif

1. Copy example.sh into scenarios directory (e.g. `better-error.sh`)
2. Edit its content based on included tips.
3. Run `generate_gifs.sh better-error` to to (re)render svgs based on `better-error.sh` scenario

Gifs will be saved in `website/static/img/gifs` and `website/static/img/dark/gifs` directories based on name of the scenario (so `foo.sh` becomes `foo.svg`)