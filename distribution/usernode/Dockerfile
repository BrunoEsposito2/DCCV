FROM ubuntu:latest

USER root

RUN apt-get update && apt-get install -y \
    curl \
    gnupg \
    wget \
    unzip \
    sudo \
    software-properties-common \
    apt-transport-https \
    ca-certificates

# Installazione del JDK (OpenJDK 17)
RUN apt-get install -y openjdk-17-jdk

RUN apt-get install -y \
    lsb-release && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce docker-ce-cli containerd.io

# Installazione di Gradle
ARG GRADLE_VERSION=8.7
RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
    && unzip gradle-${GRADLE_VERSION}-bin.zip -d /opt \
    && rm gradle-${GRADLE_VERSION}-bin.zip

# Imposta la variabile d'ambiente per Gradle
ENV GRADLE_HOME=/opt/gradle-${GRADLE_VERSION}
ENV PATH=$PATH:$GRADLE_HOME/bin

# Impostare la directory di lavoro
WORKDIR /DCCV

COPY . .

EXPOSE 2551

# CMD ["/bin/bash", "/DCCV/distribution/usernode/entrypoint.sh"]