FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew build -x test

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache fontconfig font-noto-cjk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]