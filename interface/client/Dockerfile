FROM docker:dind

# Switch to root user for installation permissions
USER root

# Install required system packages
RUN apk update && apk add --no-cache \
    wget \
    unzip \
    nodejs \
    npm \
    gnupg \
    sudo \
    curl \
    ca-certificates \
    openjdk17 \
    docker-cli

# Download and install Gradle
RUN wget -c https://services.gradle.org/distributions/gradle-8.8-bin.zip -O /tmp/gradle-8.8-bin.zip && \
    unzip -d /opt/gradle /tmp/gradle-8.8-bin.zip && \
    ln -s /opt/gradle/gradle-8.8 /opt/gradle/latest

# Set Gradle environment variables
ENV GRADLE_HOME=/opt/gradle/latest
ENV PATH=${GRADLE_HOME}/bin:${PATH}

# Create and set up application directory
RUN mkdir -p /app
WORKDIR /app

# Copy application files
COPY . /app/

# Install Node.js dependencies
RUN npm install

# Expose application port
EXPOSE 3000

# Set the new entrypoint
CMD ["/bin/sh", "entrypoint.sh"]