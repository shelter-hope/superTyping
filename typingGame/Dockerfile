FROM eclipse-temurin:22-jre-alpine

LABEL maintainer="typing-game"
LABEL version="0.0.1-SNAPSHOT"

WORKDIR /app

RUN mkdir -p /app/uploads /app/logs

COPY target/typingGame-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["java", "-jar", "app.jar"]