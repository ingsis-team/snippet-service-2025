FROM gradle:8.10.0-jdk-21-and-22

USER root
RUN apt-get update && apt-get install -y postgresql-client
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle assemble
EXPOSE 8080
ENTRYPOINT ["java","-jar","/home/gradle/src/build/libs/snippet-service-2025-0.0.1-SNAPSHOT.jar"]
