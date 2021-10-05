FROM asciinema/asciinema


RUN apt-get install -y pv curl

RUN curl -fLo scala-cli.deb https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-linux.deb
RUN dpkg -i scala-cli.deb

RUN mkdir /data
WORKDIR /data

# Preload scala-cli
RUN cd /data
RUN echo 'def a = 123' > a.scala
RUN scala-cli compile a.scala || echo "Problems with bloop"

COPY *.sh /data/
COPY scenarios /data/scenarios



ENTRYPOINT asciinema rec --overwrite --command="scenarios/$1.sh -n" /out/$1.cast