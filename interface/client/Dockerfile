FROM ubuntu:latest

# Install dependencies
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    wget \
    unzip

# Download and install Gradle
RUN wget -c https://services.gradle.org/distributions/gradle-8.8-bin.zip -O /tmp/gradle-8.8-bin.zip && \
    unzip -d /opt/gradle /tmp/gradle-8.8-bin.zip && \
    ln -s /opt/gradle/gradle-8.8 /opt/gradle/latest

# Set environment variables
ENV GRADLE_HOME=/opt/gradle/latest
ENV PATH=${GRADLE_HOME}/bin:${PATH}

# Install Node.js and npm
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs \
    npm

# Create application directory
RUN mkdir -p /app

# Set working directory
WORKDIR /app

# Copy application files
COPY .. /app/

RUN cd /app/interface && npm install

# Expose application port
EXPOSE 3000

# Define the command to run the application
CMD ["gradle", "runReact"]
