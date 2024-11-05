FROM ubuntu:latest
LABEL authors="strog"
WORKDIR .
COPY greatEngine-assembly-1.2.0.jar /
CMD java -jar greatEngine-assembly-1.2.0.jar
ENTRYPOINT ["top", "-b"]