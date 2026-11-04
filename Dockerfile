# syntax=docker/dockerfile:1
# 1단계: 빌드 (CI나 개발 PC에서만 실행. 1GB 개발 서버에서는 빌드하지 않고 이미지만 받는다)
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies --quiet
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 2단계: 실행
FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER app
EXPOSE 8080
# 작은 서버(1GB)용 기본값. 서버에서 JAVA_OPTS로 덮어쓸 수 있다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Duser.timezone=UTC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
