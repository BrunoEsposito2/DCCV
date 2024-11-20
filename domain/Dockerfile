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
    ffmpeg

# Add Gradle to PATH
ENV PATH=$PATH:/opt/gradle/gradle-8.10.2/bin

RUN mkdir /workspace

WORKDIR /workspace

COPY . .

EXPOSE 5555

VOLUME [ "/workspace/" ]

# run this command in case you have mp4 video files
# RUN ffmpeg -i video/video.mp4 -vcodec mjpeg -q:v 2 video/output.avi