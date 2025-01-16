# Použij oficiální JDK image
FROM openjdk:17-jdk-slim

# Nastav pracovní adresář
WORKDIR /app

# Zkopíruj Gradle Wrapper a ostatní build soubory
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle ./
COPY settings.gradle ./

# Nainstaluj závislosti
RUN ./gradlew dependencies --no-daemon

# Zkopíruj celý projekt
COPY . .

# Sestav projekt
RUN ./gradlew build --no-daemon

# Spustitelný JAR soubor
CMD ["java", "-jar", "build/libs/your-project-name.jar"]
