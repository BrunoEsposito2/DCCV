#!/bin/bash

# DCCV Production Deployment Script
# Distributed Cluster for Computer Vision - Docker Swarm Deployment

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

VERSION=$1
ORGANIZATION="brunoesposito2"

if [ -z "$VERSION" ]; then
    echo -e "${YELLOW}No version specified, using 'latest'${NC}"
    VERSION="latest"
fi

echo -e "${BLUE}=== DCCV Deployment Script - Version $VERSION ===${NC}"
echo -e "${BLUE}Distributed Cluster for Computer Vision${NC}"

# Check Docker installation
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker not installed. Please install Docker first.${NC}"
    exit 1
fi

# Initialize Docker Swarm if needed
swarm_status=$(docker info --format '{{.Swarm.LocalNodeState}}' 2>/dev/null)
if [ "$swarm_status" != "active" ]; then
    echo -e "${YELLOW}Initializing Docker Swarm...${NC}"
    docker swarm init || { 
        echo -e "${RED}Swarm initialization failed${NC}"
        DEFAULT_IP=$(hostname -I | awk '{print $1}')
        echo -e "${YELLOW}Trying with --advertise-addr $DEFAULT_IP${NC}"
        docker swarm init --advertise-addr $DEFAULT_IP || {
            echo -e "${RED}Failed to initialize swarm. Check network configuration.${NC}"
            exit 1
        }
    }
else
    echo -e "${GREEN}Docker Swarm already active.${NC}"
fi

# Get swarm tokens
JOIN_TOKEN=$(docker swarm join-token -q worker)
MANAGER_IP=$(docker node inspect self --format '{{.Status.Addr}}')

echo -e "${GREEN}Swarm join token: $JOIN_TOKEN${NC}"
echo -e "${GREEN}Manager IP: $MANAGER_IP${NC}"

# Create overlay network
if ! docker network ls | grep -q "swarm-network"; then
    echo -e "${YELLOW}Creating overlay network 'swarm-network'...${NC}"
    docker network create --driver overlay --attachable swarm-network || { 
        echo -e "${RED}Network creation failed${NC}"
        exit 1
    }
else
    echo -e "${GREEN}Network 'swarm-network' already exists.${NC}"
fi

# Download docker-compose file
echo -e "${YELLOW}Downloading docker-compose file...${NC}"
COMPOSE_URL="https://github.com/brunoesposito2/DCCV/releases/download/v${VERSION}/docker-compose-prod.yml"
COMPOSE_FILE="docker-compose-prod.yml"

if curl -sSL "$COMPOSE_URL" -o "$COMPOSE_FILE"; then
    echo -e "${GREEN}Docker-compose file downloaded successfully.${NC}"
else
    echo -e "${RED}Failed to download docker-compose file.${NC}"
    if [ ! -f "$COMPOSE_FILE" ]; then
        echo -e "${RED}File '$COMPOSE_FILE' not found locally.${NC}"
        exit 1
    else
        echo -e "${GREEN}Using local docker-compose file.${NC}"
    fi
fi

# Export environment variables
export JOIN_TOKEN=$JOIN_TOKEN
export MANAGER_IP=$MANAGER_IP

# Remove existing stack if present
if docker stack ls | grep -q "dccv"; then
    echo -e "${YELLOW}Removing existing DCCV stack...${NC}"
    docker stack rm dccv
    echo -e "${YELLOW}Waiting for complete stack removal...${NC}"
    sleep 15
fi

# Deploy stack
echo -e "${YELLOW}Deploying DCCV stack...${NC}"
docker stack deploy -c "$COMPOSE_FILE" dccv || {
    echo -e "${RED}Stack deployment failed.${NC}"
    exit 1
}

# Verify services
echo -e "${YELLOW}Verifying service status...${NC}"
sleep 5
docker stack services dccv

echo -e "${GREEN}=== DCCV Deployment Complete! ===${NC}"
echo -e "${BLUE}🎥 Camera Stream: http://localhost:5555${NC}"
echo -e "${BLUE}🖥️  Web Interface: http://localhost:3000${NC}"
echo -e "${BLUE}🔧 REST API: http://localhost:4000${NC}"
echo -e "${BLUE}📊 Akka Cluster Ports: 2551-2553${NC}"
echo ""
echo -e "${BLUE}📋 Management Commands:${NC}"
echo -e "${BLUE}  View logs: docker service logs -f dccv_<service_name>${NC}"
echo -e "${BLUE}  Monitor services: docker stack services dccv${NC}"
echo -e "${BLUE}  Check nodes: docker node ls${NC}"
