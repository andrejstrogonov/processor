FROM ubuntu:latest
LABEL authors="strog"
WORKDIR .
COPY greatEngine-assembly-0.1.jar /
CMD java -jar greatEngine-assembly-0.1.jar
ENTRYPOINT ["top", "-b"]