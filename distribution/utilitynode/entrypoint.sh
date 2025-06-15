#!/bin/bash

# Controlla se esiste il file PID di Docker
 if [ -f /var/run/docker.pid ]; then
     PID=$(cat /var/run/docker.pid)
     echo "Trovato Docker PID: $PID, tentativo di terminazione..."

     # Verifica se il processo esiste
     if ps -p $PID > /dev/null; then
         # Tenta di terminare il processo in modo pulito
         kill $PID
         sleep 2

         # Se il processo esiste ancora, forzane la terminazione
         if ps -p $PID > /dev/null; then
             echo "Il processo non si è terminato, uso kill -9..."
             kill -9 $PID
         fi
     else
         echo "Il processo $PID non esiste più, rimuovo solo il file PID"
     fi

     # Rimuovi il file PID
     rm -f /var/run/docker.pid
     echo "File PID rimosso"
 else
     echo "Nessun file PID Docker trovato"
 fi

# Function to check whether the Docker daemon is running
check_docker() {
    docker info >/dev/null 2>&1
    return $?
}

# Start the Docker daemon in the background
if ! check_docker; then
    echo "Starting Docker daemon with DinD configuration..."
    
    # Start dockerd with specific flags for containers
    dockerd \
        --host=unix:///var/run/docker.sock \
        --storage-driver=vfs \
        --iptables=false \
        --ip-forward=false \
        --bridge=none \
        --exec-opt native.cgroupdriver=cgroupfs \
        --log-level=error \
        >/dev/null 2>&1 &

    echo "Waiting for Docker daemon initialization..."
    WAIT_SECONDS=0
    while ! check_docker; do
        sleep 2
        WAIT_SECONDS=$((WAIT_SECONDS + 2))
        echo "Waiting for Docker daemon... ($WAIT_SECONDS seconds)"

        if [ $WAIT_SECONDS -ge 10 ]; then
            echo "Timeout: Docker daemon has not started within 10 seconds."
            echo "Trying alternative approach..."
            break
        fi
    done
else
    echo "Docker daemon is already running!"
fi
 
 echo "The Docker daemon is ready!"

 # Check swarm status
 status=$(docker info --format '{{.Swarm.LocalNodeState}}')

 # If the node already belongs to a swarm, then it'll be reset
 if [ "$status" = "active" ]; then
     echo "Swarm is already active. Ongoing leave process..."
     docker swarm leave --force

     # Wait for 4 seconds to ensure clean state
     echo "Waiting for swarm to clean up..."
     sleep 4
 fi

 # Run Docker commands
 docker swarm join --token "$JOIN_TOKEN" "$MANAGER_IP:2377"
 sleep 10
gradle --project-cache-dir=/DCCV/.gradle-utility launchUtilityNode