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

# Imposta le variabili d'ambiente per Java
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

RUN mkdir /workspace

WORKDIR /workspace

COPY . .

EXPOSE 5555

VOLUME [ "/workspace/" ]

# run this command in case you have mp4 video files
# RUN ffmpeg -i video/video.mp4 -vcodec mjpeg -q:v 2 video/output.avi