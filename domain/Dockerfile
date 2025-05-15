# Usa un'immagine di base di Linux
FROM brunoesposito2/ubuntu_opencv_build

RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
    # Install the opencv library of the detectors
    opencv-data \
    # Install the boost library
    libboost-all-dev \
    # Install the websocket library
    libwebsocketpp-dev \
    openjdk-17-jdk \
    ffmpeg

RUN apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce docker-ce-cli containerd.io

RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    openjdk-17-jdk
    
# Imposta le variabili d'ambiente per Java
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

# Installazione di Gradle
ARG GRADLE_VERSION=8.7
RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
    && unzip gradle-${GRADLE_VERSION}-bin.zip -d /opt \
    && rm gradle-${GRADLE_VERSION}-bin.zip

# Imposta la variabile d'ambiente per Gradle
ENV GRADLE_HOME=/opt/gradle-${GRADLE_VERSION}
ENV PATH=$PATH:$GRADLE_HOME/bin

RUN mkdir /DCCV

WORKDIR /DCCV

COPY . .

EXPOSE 5555

VOLUME [ "/DCCV/" ]

# run this command in case you have mp4 video files
# RUN ffmpeg -i video/video.mp4 -vcodec mjpeg -q:v 2 video/output.avi