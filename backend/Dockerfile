FROM eclipse-temurin:17-jammy

WORKDIR /app

COPY backend/ .

RUN chmod +x ./mvnw

RUN apt-get update && apt-get install -y \
    libxcursor1 \
    libgtk-3-0 \
    libpangocairo-1.0-0 \
    libcairo-gobject2 \
    libgdk-pixbuf-2.0-0 \
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libx11-xcb1 \
    libxrandr2 \
    libgbm1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

RUN ./mvnw clean package -DskipTests

RUN ./mvnw -B -ntp -f docker/generated-framework-warmup-pom.xml \
    dependency:go-offline test-compile

RUN ./mvnw -q exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install chromium"

ENV AIF_HEADLESS=true
ENV AIF_GENERATED_TEST_MAVEN_OPTS="-Xmx192m -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"
ENV PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
ENV NO_AT_BRIDGE=1

EXPOSE 10000

CMD ["sh", "-c", "java ${JAVA_OPTS:-} -jar target/backend-0.0.1-SNAPSHOT.jar"]
