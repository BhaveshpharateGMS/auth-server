# ✅ Docker Setup Complete!

Your Auth Server is now ready for Docker deployment! Here's what has been created:

## 📦 Created Files

### Core Docker Files
1. **`Dockerfile`** - Production-ready multi-stage build
   - Stage 1: Builds the application using Maven & Java 21
   - Stage 2: Optimized runtime image (~300MB)
   - Includes health checks, non-root user, and JVM optimization

2. **`docker-compose.yml`** - Complete deployment stack
   - Auth Server service with auto-restart
   - Redis service with persistent storage
   - Health checks and networking configured
   - All environment variables mapped

3. **`.dockerignore`** - Build optimization
   - Excludes unnecessary files from Docker context
   - Speeds up build process

### Quick Start Scripts
4. **`docker-start.sh`** - One-command deployment (Linux/Mac)
5. **`docker-start.bat`** - One-command deployment (Windows)

### Documentation
6. **`DOCKER_DEPLOYMENT.md`** - Comprehensive deployment guide
7. **`DOCKER_README.md`** - Quick reference guide

## 🚀 How to Deploy

### Option 1: Quick Start (Recommended)

**Windows:**
```cmd
docker-start.bat
```

**Linux/Mac:**
```bash
chmod +x docker-start.sh
./docker-start.sh
```

### Option 2: Manual Deployment

```bash
# 1. Create .env file with your configuration
# (See application.properties for required variables)

# 2. Start services
docker-compose up -d

# 3. View logs
docker-compose logs -f auth-server

# 4. Check health
curl http://localhost:8080/actuator/health
```

## 🔧 Before You Deploy

### 1. Create `.env` File

Create a `.env` file in the same directory with your configuration:

```bash
# Required Configuration
REDIS_URL=redis://redis:6379
ZITADEL_INSTANCE_URL=https://your-instance.zitadel.cloud
ZITADEL_ACCESS_TOKEN=your-access-token

# Security Settings
COOKIE_SECURE=false  # Set to true for production with HTTPS
COOKIE_SAME_SITE=Lax

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

# Persona Configurations (VENDOR, CONSUMER, AFFILIATE, GMS)
VENDOR_ISSUER=your-vendor-issuer
VENDOR_CLIENT_ID=your-client-id
VENDOR_CLIENT_SECRET=your-secret
# ... (add all required variables from application.properties)
```

### 2. Verify Docker Installation

```bash
# Check Docker
docker --version

# Check Docker Compose
docker-compose --version
```

## 📍 Service Endpoints

After deployment:

| Endpoint | URL | Purpose |
|----------|-----|---------|
| API | http://localhost:8080 | Main application |
| Health | http://localhost:8080/actuator/health | Health check |
| Liveness | http://localhost:8080/actuator/health/liveness | Kubernetes liveness |
| Readiness | http://localhost:8080/actuator/health/readiness | Kubernetes readiness |
| Redis | localhost:6379 | Redis cache |

## 📋 Common Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs (follow)
docker-compose logs -f auth-server

# View Redis logs
docker-compose logs -f redis

# Restart auth server
docker-compose restart auth-server

# Rebuild after code changes
docker-compose up -d --build

# Check running containers
docker-compose ps

# View resource usage
docker-compose stats
```

## 🏗️ Technical Details

### Image Specifications
- **Base Image**: eclipse-temurin:21-jre-alpine
- **Size**: ~300MB (optimized)
- **Java Version**: 21
- **Spring Boot**: 3.5.6
- **User**: Non-root (spring:spring)

### Security Features
- ✅ Non-root user execution
- ✅ Container-aware JVM settings
- ✅ Health check probes
- ✅ Secure cookie configuration
- ✅ Network isolation via Docker networks
- ✅ No sensitive data in image

### Performance Optimization
- ✅ Multi-stage build (smaller image)
- ✅ Layer caching for dependencies
- ✅ JVM container support enabled
- ✅ Memory limits configured
- ✅ Connection pooling for Redis

## 🌐 Production Considerations

When deploying to production:

1. **Use External Redis**
   - Redis Cloud, AWS ElastiCache, Azure Cache
   - Update `REDIS_URL` in `.env`
   - Remove Redis service from `docker-compose.yml`

2. **Enable HTTPS**
   - Set `COOKIE_SECURE=true`
   - Use reverse proxy (nginx, Traefik)
   - Configure TLS certificates

3. **Set Resource Limits**
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2'
         memory: 2G
   ```

4. **Use Secrets Management**
   - Docker secrets
   - Kubernetes secrets
   - HashiCorp Vault
   - AWS Secrets Manager

5. **Add Monitoring**
   - Prometheus metrics
   - Grafana dashboards
   - Centralized logging (ELK, Splunk)

## 🔍 Troubleshooting

### Container won't start
```bash
# Check logs
docker-compose logs auth-server

# Common issues:
# - Missing environment variables
# - Redis connection failed
# - Invalid Zitadel credentials
```

### Port already in use
Edit `docker-compose.yml`:
```yaml
ports:
  - "8081:8080"  # Change to different port
```

### Redis connection refused
```bash
# Test Redis
docker-compose exec redis redis-cli ping
# Should return: PONG

# Check Redis logs
docker-compose logs redis
```

### Out of memory
Increase memory in `docker-compose.yml`:
```yaml
environment:
  - JAVA_OPTS=-XX:MaxRAMPercentage=50.0
```

## 📚 Additional Resources

- **Full Deployment Guide**: [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)
- **Quick Reference**: [DOCKER_README.md](DOCKER_README.md)
- **Application Config**: [src/main/resources/application.properties](src/main/resources/application.properties)

## ✨ What's Next?

1. Create your `.env` file with proper credentials
2. Run `./docker-start.sh` (or `.bat` on Windows)
3. Verify health at http://localhost:8080/actuator/health
4. Test your authentication endpoints
5. Review logs for any issues

## 🎯 Deployment Checklist

- [ ] Docker & Docker Compose installed
- [ ] `.env` file created with all required variables
- [ ] Redis accessible (or using docker-compose Redis)
- [ ] Zitadel credentials verified
- [ ] Network ports available (8080, 6379)
- [ ] Sufficient resources (2GB+ RAM)
- [ ] For production: HTTPS configured
- [ ] For production: External Redis configured
- [ ] For production: Monitoring setup

---

**You're all set! 🎉**

Run your deployment script and your Auth Server will be up and running in minutes!

For questions or issues, refer to [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) for detailed troubleshooting.

