FROM eclipse-temurin:17-jammy

WORKDIR /app

COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

RUN ./mvnw -q exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install --with-deps chromium"

ENV AIF_HEADLESS=true
ENV AIF_BROWSER_CHANNEL=
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

EXPOSE 10000

CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
