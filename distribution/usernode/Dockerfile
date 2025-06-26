# Multi-stage build per ottimizzare la dimensione dell'immagine finale
FROM ubuntu:latest AS builder

USER root

RUN apt-get update && apt-get install -y \
    curl \
    gnupg \
    wget \
    unzip \
    openjdk-17-jdk \
    && rm -rf /var/lib/apt/lists/*

# Installazione di Gradle
ARG GRADLE_VERSION=8.7
RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
    && unzip gradle-${GRADLE_VERSION}-bin.zip -d /opt \
    && rm gradle-${GRADLE_VERSION}-bin.zip

ENV GRADLE_HOME=/opt/gradle-${GRADLE_VERSION}
ENV PATH=$PATH:$GRADLE_HOME/bin

# Copia il codice sorgente
WORKDIR /build
COPY . .

# Build del progetto e creazione del JAR
RUN gradle clean shadowJar 

# Stage finale - runtime
FROM ubuntu:latest

USER root

RUN apt-get update && apt-get install -y \
    curl \
    gnupg \
    wget \
    sudo \
    software-properties-common \
    apt-transport-https \
    ca-certificates \
    lsb-release \
    openjdk-17-jre \
    && rm -rf /var/lib/apt/lists/*

# Installazione Docker
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce docker-ce-cli containerd.io && \
    rm -rf /var/lib/apt/lists/*

# Imposta la directory di lavoro
WORKDIR /DCCV

# Copia solo il JAR necessario dal builder stage
COPY --from=builder /build/distribution/usernode/build/libs/usernode-*-fat.jar /DCCV/usernode-app.jar

# Copia lo script di entrypoint
COPY distribution/usernode/entrypoint.sh /DCCV/entrypoint.sh
RUN chmod +x /DCCV/entrypoint.sh

EXPOSE 2551