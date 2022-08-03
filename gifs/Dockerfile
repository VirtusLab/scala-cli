FROM ubuntu:20.04

RUN curl -fsSL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get update &&\
    DEBIAN_FRONTEND=noninteractive apt-get install -y pv curl clang rubygems nodejs python3-pip &&\
    rm -rf /var/lib/apt/lists/* &&\
    gem install rouge &&\
    pip3 install asciinema==2.1.0

RUN mkdir /data
WORKDIR /data

# Preload scala-cli
RUN cd /data
RUN echo 'def a = 123' > a.scala
RUN scala-cli compile a.scala || echo "Problems with bloop"

# Update path to point to newer version
RUN curl -fLo scala-cli.deb https://github.com/VirtusLab/scala-cli/releases/download/v0.1.11/scala-cli-x86_64-pc-linux.deb
RUN dpkg -i scala-cli.deb

COPY *.sh /data/
COPY scenarios /data/scenarios

ENTRYPOINT ./run_scenario.sh "$1"