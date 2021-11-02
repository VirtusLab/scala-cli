FROM asciinema/asciinema

RUN apt-get install -y pv curl

RUN mkdir /data
WORKDIR /data

# Preload scala-cli
RUN cd /data
RUN echo 'def a = 123' > a.scala
RUN scala-cli compile a.scala || echo "Problems with bloop"

RUN apt-get update
RUN apt-get install -y clang

RUN curl -fsSL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get install -y nodejs

RUN apt-get install -y rubygems
RUN gem install rouge

# Update path to point to newer version
RUN curl -fLo scala-cli.deb https://github.com/VirtusLab/scala-cli/releases/download/v0.0.7/scala-cli-x86_64-pc-linux.deb
RUN dpkg -i scala-cli.deb

COPY *.sh /data/
COPY scenarios /data/scenarios

ENTRYPOINT ./run_scenario.sh "$1"