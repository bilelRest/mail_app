FROM openjdk:17-jdk-alpine

# Installer maven et les polices nécessaires
RUN apk add --no-cache maven ttf-freefont fontconfig

# Ajouter les fichiers nécessaires
COPY pom.xml .
COPY src ./src
COPY target/mail-0.0.1-SNAPSHOT.jar /app/mail-0.0.1-SNAPSHOT.jar

# Générer le cache des polices
RUN fc-cache -fv

# Exposer le port
EXPOSE 8087

# Commande de lancement de l'application
CMD ["java", "-jar", "/app/mail-0.0.1-SNAPSHOT.jar"]
