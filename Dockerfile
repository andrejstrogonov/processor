FROM ubuntu:16.04

RUN apt-get update
RUN apt-get install -y openjdk-8-jdk
RUN apt-get -y install wget
RUN apt-get install gzip
ADD https://www.scala-lang.org/files/archive/scala-2.13.12.tgz /scala-2.13.12

ENV SCALA_HOME /usr/local

CMD ["scala"]