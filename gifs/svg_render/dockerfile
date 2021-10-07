FROM node:12.18.1

RUN npm install -g svg-term-cli

RUN mkdir /profiles

# Terminal themes for light and dark mode of the site
# Based on https://iterm2colorschemes.com/
RUN curl -fLo /profiles/dark https://raw.githubusercontent.com/mbadolato/iTerm2-Color-Schemes/master/schemes/Chester.itermcolors
RUN curl -fLo /profiles/light https://raw.githubusercontent.com/mbadolato/iTerm2-Color-Schemes/master/schemes/Jackie%20Brown.itermcolors


ENTRYPOINT svg-term $*