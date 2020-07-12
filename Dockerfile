FROM insideo/jre8
COPY target/scala-2.13/uber-server-assembly-1.0.0.jar /etc
EXPOSE 8080
ENTRYPOINT java -jar /etc/uber-server-assembly-1.0.0.jar
