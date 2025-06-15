---
title: "DCCV - Distributed Cluster for Computer Vision"
layout: default
description: "University project implementing a distributed computer vision system using Akka actors and Docker Swarm"
---

# DCCV - Distributed Cluster for Computer Vision

A distributed computer vision system built with Akka actors and Docker Swarm for real-time computer vision algorithms.

## Quick Start

```bash
curl -sSL https://github.com/brunoesposito2/DCCV/releases/latest/download/deploy-dccv.sh -o deploy-dccv.sh
chmod +x deploy-dccv.sh
./deploy-dccv.sh
```

## Architecture

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Camera Node** | C++/OpenCV | Computer vision processing |
| **GUI Frontend** | React | Web interface |
| **GUI Backend** | Scala/Akka/Vertx | REST API & coordination |
| **Utility Node** | Scala/Akka | Cluster supervision |
| **Storage** | MongoDB | Data persistence |

## Features

- **Real-time computer vision** with face/body detection
- **Distributed actor system** using Akka clustering
- **Live video streaming** via WebSocket
- **Docker Swarm orchestration** for scalability
- **Modern web interface** with React
- **Automated CI/CD** with semantic versioning

## Documentation

- [**API Documentation**](./api/) - Akka actors main protocols, REST and Docker APIs
- [**Latest Release**](https://github.com/brunoesposito2/DCCV/releases/latest) - Deployment files
- [**Source Code**](https://github.com/brunoesposito2/DCCV) - GitHub repository
- [**Docker Images**](https://hub.docker.com/u/brunoesposito2) - Container registry

## Technology Stack

**Backend:** Scala, Akka, C++, OpenCV, Vertx, MongoDB  
**Frontend:** React, WebSocket  
**DevOps:** Docker Swarm, GitHub Actions, Gradle
