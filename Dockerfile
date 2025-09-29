# Build Stage
FROM gradle:jdk17-jammy AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

# Runtime Stage
FROM eclipse-temurin:17-jdk-jammy
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]