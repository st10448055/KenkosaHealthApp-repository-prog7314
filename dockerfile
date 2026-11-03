FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
# gradlew was committed with Windows line endings; the backend image has no Android SDK
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
 && sed -i '/include(":app")/d' settings.gradle.kts \
 && ./gradlew :backend:shadowJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/backend/build/libs/*-all.jar /app/server.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-jar", "/app/server.jar"]