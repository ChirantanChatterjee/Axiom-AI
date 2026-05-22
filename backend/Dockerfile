FROM eclipse-temurin:17-jammy

WORKDIR /app

COPY backend/ .

RUN chmod +x ./mvnw

RUN ./mvnw clean package -DskipTests

RUN ./mvnw -q exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install --with-deps chromium"

ENV AIF_HEADLESS=true
ENV PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright
ENV NO_AT_BRIDGE=1

EXPOSE 10000

CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]