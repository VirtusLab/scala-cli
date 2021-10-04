# Tooling to generate nice gifs used in documentation

Recordings are possible using https://github.com/paxtonhare/demo-magic licensed under MIT Licence

## How does it work?

Our gifs are compose of scenaries built using [demo-magic](https://github.com/paxtonhare/demo-magic) and then recorded using ascicinema to .json files to be finally rendered using [asciicast2gif](https://github.com/asciinema/asciicast2gif) docker image

## How to create new gif

1. Copy example.sh into scenarios (e.g. `better-error.sh`)
2. Edit its content based on included tips.
3. Run `render.sc` to render all files or `render.sc better-error` to (re)render single gif

Gifs will be saved in `website/static/img` directry based on name of the scenario (so `foo.sh` becomes `foo.gif`)