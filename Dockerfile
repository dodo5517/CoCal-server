# Build Stage
FROM gradle:jdk17-jammy AS build
WORKDIR /home/gradle/src
COPY . /home/gradle/src
RUN gradle clean bootJar -x test --no-daemon

# Runtime Stage
FROM eclipse-temurin:17-jdk-jammy
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]