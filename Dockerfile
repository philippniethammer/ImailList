#
# Build stage
#
FROM maven:3-eclipse-temurin-11-alpine AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

#
# Package stage
#
FROM eclipse-temurin:11-jre-ubi9-minimal
COPY --from=build /home/app/target/imailList.jar /usr/local/lib/imailList.jar
ENTRYPOINT ["java","-jar","/usr/local/lib/imailList.jar"]
