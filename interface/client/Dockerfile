FROM docker:dind AS builder

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
    docker-cli \
    bash

# Download and install Gradle
RUN wget -c https://services.gradle.org/distributions/gradle-8.8-bin.zip -O /tmp/gradle-8.8-bin.zip && \
    unzip -d /opt/gradle /tmp/gradle-8.8-bin.zip && \
    ln -s /opt/gradle/gradle-8.8 /opt/gradle/latest && \
    rm /tmp/gradle-8.8-bin.zip

# Set Gradle environment variables
ENV GRADLE_HOME=/opt/gradle/latest
ENV PATH=${GRADLE_HOME}/bin:${PATH}
ENV NODE_ENV=production

ENV REACT_APP_API_BASE_URL=http://guibackend:4000
ENV REACT_APP_VIDEO_WS_URL=ws://cameranode:5555

# Create and set up application directory
WORKDIR /app

# Copy application source code
COPY . .

# Use a faster npm registry
RUN npm config set fetch-retry-mintimeout 20000 && \
    npm config set fetch-retry-maxtimeout 120000 && \
    npm config set fetch-timeout 600000 && \
    npm install --verbose

# Build the React application (production build)
RUN npm run build


FROM docker:dind AS production

USER root

# Install only essential runtime packages
RUN apk update && apk add --no-cache \
    nodejs \
    npm \
    bash \
    curl \
    ca-certificates \
    docker-cli

# Install serve globally to serve the React application
RUN npm install -g serve

# Create application directory
WORKDIR /app

# Copy built application from builder stage
COPY --from=builder /app/build ./build
COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/entrypoint.sh ./entrypoint.sh

# Make entrypoint executable
RUN chmod +x ./entrypoint.sh

# Create a start script for the application
RUN echo '#!/bin/bash' > /app/start.sh && \
    echo 'echo "Starting DCCV Frontend..."' >> /app/start.sh && \
    echo 'cd /app/build' >> /app/start.sh && \
    echo 'serve -s . -l 3000 -L' >> /app/start.sh && \
    chmod +x /app/start.sh

# Expose application port
EXPOSE 3000

# Set the new entrypoint
CMD ["/bin/sh", "entrypoint.sh"]
