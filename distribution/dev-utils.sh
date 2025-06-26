#!/bin/bash

# Script di utilità migliorato per lo sviluppo DCCV con supporto per Docker Stack

# Colori per l'output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configurazione
PROJECT_ROOT=$(pwd)

function show_help {
    echo "Utilizzo: $0 [comando]"
    echo "Comandi disponibili:"
    echo "  start-dev         Avvia i nodi tramite docker in modalità sviluppo"
    echo "  start-dev-swarm   Avvia lo swarm in modalità sviluppo"
    echo "  sync-camera       Sincronizza e riavvia CameraNode"
    echo "  sync-user         Sincronizza e riavvia UserNode"
    echo "  sync-utility      Sincronizza e riavvia UtilityNode"
    echo "  sync-all          Sincronizza e riavvia tutti i nodi"
    echo "  logs [nodo]       Mostra i log di un nodo specifico (camera, user, utility, frontend)"
    echo "  status            Mostra lo stato dello swarm"
    echo "  stop              Arresta tutti i container Docker Compose"
    echo "  clean             Arresta e rimuove i container, le reti e i volumi"
}

function init_swarm {
    # Verifica se lo swarm è già inizializzato
    swarm_status=$(docker info --format '{{.Swarm.LocalNodeState}}')
    if [ "$swarm_status" != "active" ]; then
        echo -e "${YELLOW}Inizializzazione Docker Swarm...${NC}"
        docker swarm init || { echo -e "${RED}Errore durante l'inizializzazione dello swarm${NC}"; return 1; }
    else
        echo -e "${GREEN}Docker Swarm è già attivo.${NC}"
    fi

    # Crea rete overlay se non esiste
    if ! docker network ls | grep -q "swarm-network"; then
        echo -e "${YELLOW}Creazione rete overlay 'swarm-network'...${NC}"
        docker network create --driver overlay --attachable swarm-network || { echo -e "${RED}Errore durante la creazione della rete${NC}"; return 1; }
    else
        echo -e "${GREEN}Rete 'swarm-network' già presente.${NC}"
    fi

    # Imposta variabili di ambiente globali
    export JOIN_TOKEN=$(docker swarm join-token -q worker)
    export MANAGER_IP=$(docker node inspect self --format '{{.Status.Addr}}')

    return 0
}

# funzione per pulire i lock file Gradle
function clean_gradle_locks {
    echo "Pulizia lock file Gradle..."
    find . -name "*.lock" -path "*/.gradle*/*" -delete
    echo "Lock file rimossi."
}

function start_dev {
    echo -e "${YELLOW}Avvio dei nodi in modalità sviluppo...${NC}"

    # Pulizia dei lock file prima dell'avvio
    clean_gradle_locks

    init_swarm

    echo -e "${YELLOW}Avvio CameraNode...${NC}"
    cd distribution/cameranode && docker-compose up -d

    echo -e "${YELLOW}Avvio UserNode...${NC}"
    cd ../usernode && docker-compose up -d

    echo -e "${YELLOW}Avvio UtilityNode...${NC}"
    cd ../utilitynode && docker-compose up -d

    cd ../../

    DEV_MODE="true"

    echo -e "${GREEN}Tutti i nodi sono stati avviati in modalità sviluppo!${NC}"

    echo -e "Premi un tasto qualsiasi per continuare..."
    read -n 1 -s
}

function start_dev_swarm {
    echo -e "${YELLOW}Avvio dello swarm in modalità sviluppo...${NC}"

    init_swarm || return 1

    # Pulizia dei lock file prima dell'avvio
    clean_gradle_locks

    # Avvia container in modalità sviluppo su swarm
    echo -e "${YELLOW}Avvio dei nodi su docker swarm...${NC}"
    docker compose up --build -d

    DEV_MODE="true"

    echo -e "${GREEN}Tutti i nodi swarm sono stati avviati in modalità sviluppo!${NC}"

    echo -e "Premi un tasto qualsiasi per continuare..."
    read -n 1 -s
}

function sync_camera {
    echo -e "${YELLOW}Sincronizzazione CameraNode...${NC}"

    # Compila i jar necessari
    cd "$PROJECT_ROOT"
    ./gradlew application:jar domain:buildCMake distribution:cameranode:jar

    echo -e "${YELLOW}Modalità sviluppo: riavvio soft dell'applicazione...${NC}"
    # In modalità dev il container ha già i file montati, quindi basta riavviare l'applicazione
    # Se l'applicazione ha un meccanismo per ricaricare i file modificati, potrebbe non essere necessario
    docker exec -it cameranode pkill -f "java" || true
    # Aspetta che il processo termini
    sleep 2
    # Riavvia l'applicazione tramite il task Gradle
    docker exec -d cameranode bash -c "cd /DCCV && gradle --project-cache-dir=/DCCV/.gradle-camera launchCameraNode"
    docker restart cameranode

    echo -e "${GREEN}CameraNode sincronizzato!${NC}"
}

function sync_user {
    echo -e "${YELLOW}Sincronizzazione UserNode...${NC}"

    # Compila i jar necessari
    cd "$PROJECT_ROOT"
    ./gradlew application:jar presentation:jar interface:server:jar distribution:usernode:jar

    echo -e "${YELLOW}Modalità sviluppo: riavvio soft dell'applicazione...${NC}"
    # Riavvio soft solo del backend
    docker exec -it guibackend pkill -f "java" || true
    # Aspetta che il processo termini
    sleep 2
    # Riavvia l'applicazione tramite il task Gradle
    docker exec -d guibackend bash -c "cd /DCCV && gradle --project-cache-dir=/DCCV/.gradle-backend launchServer"

    docker restart guibackend

    echo -e "${GREEN}UserNode backend sincronizzato!${NC}"
    # Nota: per il frontend, se si usa React con hot-reload non è necessario alcun riavvio.
    # In caso contrario, decommentare la seguente riga
    # docker restart guifrontend
}

function sync_utility {
    echo -e "${YELLOW}Sincronizzazione UtilityNode...${NC}"

    # Compila i jar necessari
    cd "$PROJECT_ROOT"
    ./gradlew application:jar presentation:jar storage:jar distribution:utilitynode:jar

    echo -e "${YELLOW}Modalità sviluppo: riavvio soft dell'applicazione...${NC}"
    # Riavvio soft dell'applicazione
    docker exec -it utilitynode pkill -f "java" || true
    # Aspetta che il processo termini
    sleep 2
    # Riavvia l'applicazione tramite il task Gradle
    docker exec -d utilitynode bash -c "cd /DCCV && gradle --project-cache-dir=/DCCV/.gradle-utility launchUtilityNode"

    docker restart utilitynode

    echo -e "${GREEN}UtilityNode sincronizzato!${NC}"
}

function sync_all {
    echo -e "${YELLOW}Sincronizzazione di tutti i nodi...${NC}"
    sync_camera
    sync_user
    sync_utility
    echo -e "${GREEN}Tutti i nodi sincronizzati!${NC}"
}

function show_logs {
    if [ -z "$1" ]; then
        echo -e "${RED}Specificare il nodo (camera, user, utility, frontend)${NC}"
        return 1
    fi

    case $1 in
        camera)
            docker logs -f cameranode
            ;;
        user)
            docker logs -f guibackend
            ;;
        frontend)
            docker logs -f guifrontend
            ;;
        utility)
            docker logs -f utilitynode
            ;;
        *)
            echo -e "${RED}Nodo non riconosciuto. Usare: camera, user, frontend, utility${NC}"
            return 1
            ;;
    esac
}

function show_status {
    echo -e "${YELLOW}Stato dei nodi swarm:${NC}"
    docker node ls

    echo -e "\n${YELLOW}Container in esecuzione:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

    echo -e "\n${YELLOW}Reti Docker:${NC}"
    docker network ls

    echo -e "\n${YELLOW}Test connettività tra container:${NC}"
    echo "Da CameraNode a UserNode (porta 2551):"
    docker exec -it cameranode nc -zv guibackend 2551 || echo "Connessione fallita"
    echo "Da CameraNode a UtilityNode (porta 2552):"
    docker exec -it cameranode nc -zv utilitynode 2552 || echo "Connessione fallita"
    echo "Da UserNode a CameraNode (porta 2550):"
    docker exec -it guibackend nc -zv cameranode 2550 || echo "Connessione fallita"
}

function stop_compose {
    echo -e "${YELLOW}Arresto di tutti i container Docker Compose...${NC}"
    cd distribution/cameranode && docker-compose down
    cd ../usernode && docker-compose down
    cd ../utilitynode && docker-compose down
    cd ../..
    echo -e "${GREEN}Tutti i container Docker Compose arrestati.${NC}"
}

function clean_builds {
    echo -e "${YELLOW}Pulizia delle directory di build...${NC}"

    # Esegui pulizia Gradle
    ./gradlew clean || echo "Errore durante l'esecuzione di Gradle clean"

    # Rimuovi tutte le directory di build nel progetto
    find . -type d -name "build" -exec rm -rf {} \; 2>/dev/null || true
    find . -type d -name "out" -exec rm -rf {} \; 2>/dev/null || true

    # Rimuovi le directory .gradle
    find . -type d -name ".gradle*" -exec rm -rf {} \; 2>/dev/null || true
    rm -rf .gradle

    # Rimuovi eventuali file generati
    find . -name "*.class" -exec rm -f {} \; 2>/dev/null || true
    find . -name "*.jar" -path "*/build/*" -exec rm -f {} \; 2>/dev/null || true

    # Rimuovi le directory node_modules
    find . -type d -name "node_modules" -exec rm -rf {} \; 2>/dev/null || true
    rm -rf node_modules

    # Pulisci i lock file di Gradle
    clean_gradle_locks

    echo -e "${GREEN}Directory di build pulite.${NC}"
}

function clean_all {
    echo -e "${YELLOW}Arresto e pulizia completa...${NC}"

    # Rimuovi container Docker Compose
    stop_compose

    # Rimuovi reti orfane
    echo -e "${YELLOW}Rimozione reti orfane...${NC}"
    docker network prune -f

    # Rimuovi volumi non utilizzati
    echo -e "${YELLOW}Rimozione volumi non utilizzati...${NC}"
    docker volume prune -f

    # Pulisci i lock file di Gradle
    clean_builds

    echo -e "${GREEN}Pulizia completata.${NC}"
}

# Main
case $1 in
    start-dev)
        start_dev
        ;;
    start-dev-swarm)
        start_dev_swarm
        ;;
    sync-camera)
        sync_camera
        ;;
    sync-user)
        sync_user
        ;;
    sync-utility)
        sync_utility
        ;;
    sync-all)
        sync_all
        ;;
    logs)
        show_logs $2
        ;;
    status)
        show_status
        ;;
    stop)
        stop_compose
        ;;
    clean)
        clean_all
        ;;
    *)
        show_help
        ;;
esac