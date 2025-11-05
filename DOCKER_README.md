# 🐳 Docker Files for Auth Server

Complete Docker setup for deploying the Authentication & Authorization Server.

## 📦 What's Included

| File | Description |
|------|-------------|
| `Dockerfile` | Multi-stage build configuration (Java 21 + Maven) |
| `docker-compose.yml` | Complete stack with Redis |
| `.dockerignore` | Optimizes build by excluding unnecessary files |
| `docker-start.sh` | Quick start script for Linux/Mac |
| `docker-start.bat` | Quick start script for Windows |
| `DOCKER_DEPLOYMENT.md` | Comprehensive deployment guide |

## 🚀 Quick Start

### Prerequisites
- Docker Engine 20.10+
- Docker Compose 2.0+
- 2GB+ RAM available

### Linux / Mac

```bash
# Make the script executable
chmod +x docker-start.sh

# Run the deployment
./docker-start.sh
```

### Windows

```cmd
# Double-click docker-start.bat or run:
docker-start.bat
```

### Manual Start

```bash
# Build and start services
docker-compose up -d

# View logs
docker-compose logs -f auth-server

# Stop services
docker-compose down
```

## 🔧 Configuration

Create a `.env` file with your configuration:

```bash
# Minimal required configuration
REDIS_URL=redis://redis:6379
ZITADEL_INSTANCE_URL=https://your-instance.zitadel.cloud
ZITADEL_ACCESS_TOKEN=your-access-token

# Add all persona configurations (VENDOR, CONSUMER, AFFILIATE, GMS)
# See application.properties for all required variables
```

## 📚 Documentation

- **Full Deployment Guide**: See [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)
- **Application Config**: See [application.properties](src/main/resources/application.properties)
- **Project Info**: See [README.md](README.md) (if available)

## 🏥 Health Check

Once deployed, verify the service is running:

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

## 🔑 Key Features

✅ **Multi-stage Build** - Optimized image size (~300MB)  
✅ **Non-root User** - Enhanced security  
✅ **Health Checks** - Built-in monitoring  
✅ **Auto-restart** - Resilient deployment  
✅ **JVM Optimization** - Container-aware settings  
✅ **Redis Included** - Complete stack in one command  

## 📊 Architecture

```
┌─────────────────┐      ┌─────────────────┐
│   Auth Server   │─────▶│     Redis       │
│   (Port 8080)   │      │   (Port 6379)   │
└─────────────────┘      └─────────────────┘
         │
         ▼
   ┌──────────────┐
   │   Zitadel    │
   │   (External) │
   └──────────────┘
```

## 🛠️ Troubleshooting

### Container won't start
```bash
docker-compose logs auth-server
```

### Port already in use
Edit `docker-compose.yml` and change the port:
```yaml
ports:
  - "8081:8080"  # Use 8081 instead
```

### Redis connection issues
```bash
docker-compose exec redis redis-cli ping
# Should return: PONG
```

## 🔄 Common Commands

```bash
# View all logs
docker-compose logs -f

# Restart only auth-server
docker-compose restart auth-server

# Rebuild after code changes
docker-compose up -d --build

# Stop and remove everything
docker-compose down -v

# Check container stats
docker-compose stats
```

## 🌐 Production Deployment

For production, consider:

1. **Use external Redis** (Redis Cloud, AWS ElastiCache, etc.)
2. **Set `COOKIE_SECURE=true`** (requires HTTPS)
3. **Add resource limits** in docker-compose.yml
4. **Use secrets management** (Docker secrets, Kubernetes secrets)
5. **Enable proper logging** (centralized logging solution)
6. **Set up monitoring** (Prometheus, Grafana, etc.)

See [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) for detailed production guidelines.

## 📞 Support

- **Application Issues**: Check logs with `docker-compose logs`
- **Docker Issues**: Verify Docker installation and permissions
- **Configuration**: Review `.env` file and `application.properties`

---

**Ready to deploy? Run `./docker-start.sh` (Linux/Mac) or `docker-start.bat` (Windows)!** 🚀

