# Multi-stage build per ottimizzare la dimensione dell'immagine finale
FROM brunoesposito2/ubuntu_opencv_build AS builder

USER root

RUN apt-get update && apt-get install -y \
    curl \
    gnupg \
    wget \
    unzip \
    openjdk-17-jdk \
    sudo \
    software-properties-common \
    apt-transport-https \
    ca-certificates \
    lsb-release \
    ffmpeg \
    # Install the opencv library of the detectors
    opencv-data \
    # Install the boost library
    libboost-all-dev \
    # Install the websocket library
    libwebsocketpp-dev \
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

# Build del progetto e creazione del JAR (assumendo che il cameranode sia in una directory specifica)
RUN gradle clean shadowJar 

# Build del programma C++ per la creazione dell'eseguibile
RUN gradle domain:build -x test -x spotlessCheck -x spotlessApply

# Stage finale - runtime basato sull'immagine OpenCV che già usi
FROM brunoesposito2/ubuntu_opencv_build

USER root

RUN apt-get update && apt-get install -y \
    curl \
    gnupg \
    wget \
    sudo \
    lsb-release \
    openjdk-17-jre 

# Installazione Docker
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce docker-ce-cli containerd.io && \
    rm -rf /var/lib/apt/lists/*

# Imposta le variabili d'ambiente per Java
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

# Imposta la directory di lavoro
WORKDIR /DCCV

# Copia solo il JAR necessario dal builder stage
COPY --from=builder /build/distribution/cameranode/build/libs/cameranode-*-fat.jar /DCCV/cameranode-app.jar

# Copia tutto il path domain dal builder stage
COPY --from=builder /build/domain /DCCV/domain

# Copia lo script di entrypoint
COPY distribution/cameranode/entrypoint.sh /DCCV/entrypoint.sh
RUN chmod +x /DCCV/entrypoint.sh

EXPOSE 2553
EXPOSE 5555
EXPOSE 8080

# run this command in case you have mp4 video files
# RUN ffmpeg -i video/video.mp4 -vcodec mjpeg -q:v 2 video/output.avi
