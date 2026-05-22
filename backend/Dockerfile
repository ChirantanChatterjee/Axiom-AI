FROM eclipse-temurin:17-jammy

WORKDIR /app

COPY backend/ .

RUN chmod +x mvnw

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
    libxrandr2 \
    libgbm1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

RUN ./mvnw clean package -DskipTests

RUN ./mvnw -q exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install chromium"

ENV AIF_HEADLESS=true
ENV PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright
ENV NO_AT_BRIDGE=1

EXPOSE 10000

CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]