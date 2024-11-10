FROM ubuntu:16.04

RUN apt-get update
RUN apt-get install -y openjdk-8-jdk
RUN apt-get -y install wget
RUN apt-get install gzip
RUN apt-get -y install curl
ADD https://www.scala-lang.org/files/archive/scala-2.13.12.tgz /scala_2
ADD https://github.com/sbt/sbt/releases/tag/v1.9.9 /sbt/

ENV scala_2/scala-2.13.12/bin/scalac="scala-cli"
CMD ["scala-cli","src/main/scala/Main.scala"]

