FROM ubuntu:20.04

RUN curl -fsSL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get update
RUN apt-get install openjdk-17-jdk openjdk-17-jre -y
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y pv curl clang rubygems nodejs python3-pip &&\
    rm -rf /var/lib/apt/lists/* &&\
    gem install rouge &&\
    pip3 install asciinema==2.1.0

RUN mkdir /data
WORKDIR /data

COPY scala-cli-jvm /usr/bin/scala-cli
# Preload scala-cli
RUN cd /data
RUN echo 'def a = 123' > a.scala
RUN scala-cli compile a.scala || echo "Problems with bloop"

COPY *.sh /data/
COPY scenarios /data/scenarios

ENTRYPOINT ./run_scenario.sh "$1"