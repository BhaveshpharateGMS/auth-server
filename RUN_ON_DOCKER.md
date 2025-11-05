# 🚀 Run Auth Server on Docker - Simple Steps

## ✅ Prerequisites
- [x] Docker installed (you have Docker 28.4.0 ✓)
- [x] Docker Compose installed (you have v2.39.2 ✓)
- [ ] Environment variables ready

---

## 🎯 3 SIMPLE STEPS TO RUN

### STEP 1: Setup Environment Variables

You have two options:

#### Option A: Use Basic Configuration (Quick Start)
I've created `.env.docker` file with basic settings. Just rename it:

```powershell
Rename-Item .env.docker .env
```

#### Option B: Full Configuration
Edit the `.env` file and add all your persona variables:

```powershell
notepad .env
```

Add your VENDOR, CONSUMER, AFFILIATE, and GMS configurations from `application.properties`.

---

### STEP 2: Start the Service

Run this single command:

```powershell
docker-compose up -d
```

**What this does:**
- 🔨 Builds your Auth Server Docker image
- 🗄️ Starts Redis container
- 🚀 Starts Auth Server container
- ✓ Runs everything in the background (-d flag)

**First time?** Building may take 2-3 minutes to download dependencies.

---

### STEP 3: Verify It's Running

Check if your service is healthy:

```powershell
# Check running containers
docker-compose ps

# Check health endpoint
curl http://localhost:8080/actuator/health

# Or open in browser
start http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

---

## 📊 Monitoring Your Service

### View Live Logs
```powershell
# All services
docker-compose logs -f

# Only Auth Server
docker-compose logs -f auth-server

# Only Redis
docker-compose logs -f redis
```

### Check Container Status
```powershell
docker-compose ps
```

### View Resource Usage
```powershell
docker-compose stats
```

---

## 🔧 Common Operations

### Restart the Service
```powershell
docker-compose restart auth-server
```

### Stop Everything
```powershell
docker-compose down
```

### Rebuild After Code Changes
```powershell
docker-compose up -d --build
```

### Clean Restart (removes volumes)
```powershell
docker-compose down -v
docker-compose up -d
```

---

## 🌐 Access Your Services

Once running, you can access:

| Service | URL | Description |
|---------|-----|-------------|
| Auth API | http://localhost:8080 | Main API endpoints |
| Health Check | http://localhost:8080/actuator/health | Service health status |
| Liveness | http://localhost:8080/actuator/health/liveness | Liveness probe |
| Readiness | http://localhost:8080/actuator/health/readiness | Readiness probe |
| Redis | localhost:6379 | Redis cache (internal) |

---

## 🐛 Troubleshooting

### Service won't start?

1. **Check logs:**
```powershell
docker-compose logs auth-server
```

2. **Common issues:**
   - Missing environment variables in `.env`
   - Port 8080 already in use
   - Redis connection failed
   - Invalid Zitadel credentials

### Port already in use?

Edit `docker-compose.yml` and change the port:
```yaml
ports:
  - "8081:8080"  # Use 8081 instead of 8080
```

### Redis connection issues?

Check if Redis is running:
```powershell
docker-compose exec redis redis-cli ping
```
Should return: `PONG`

### Out of memory?

Check available memory:
```powershell
docker-compose stats
```

Increase memory limits in `docker-compose.yml`:
```yaml
deploy:
  resources:
    limits:
      memory: 2G
```

### See all running containers:
```powershell
docker ps
```

### Stop specific container:
```powershell
docker stop auth-server
```

---

## 📝 Quick Reference Card

```powershell
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f auth-server

# Restart
docker-compose restart auth-server

# Rebuild
docker-compose up -d --build

# Check status
docker-compose ps

# Check health
curl http://localhost:8080/actuator/health
```

---

## 🔄 Development Workflow

1. **Make code changes** in your IDE
2. **Rebuild and restart:**
   ```powershell
   docker-compose up -d --build
   ```
3. **Check logs:**
   ```powershell
   docker-compose logs -f auth-server
   ```
4. **Test your changes:**
   ```powershell
   curl http://localhost:8080/actuator/health
   ```

---

## 🛑 Stopping Your Service

```powershell
# Stop containers (keeps data)
docker-compose down

# Stop and remove volumes (fresh start next time)
docker-compose down -v
```

---

## 🎉 That's It!

Your Auth Server is now running on Docker!

**Next steps:**
- Test your authentication endpoints
- Monitor logs for any errors
- Configure all persona variables in `.env`
- Review `DOCKER_DEPLOYMENT.md` for production setup

**Need help?** Check `DOCKER_DEPLOYMENT.md` for detailed troubleshooting.

---

## 📞 Quick Commands Summary

| Task | Command |
|------|---------|
| Start | `docker-compose up -d` |
| Stop | `docker-compose down` |
| Logs | `docker-compose logs -f auth-server` |
| Restart | `docker-compose restart auth-server` |
| Status | `docker-compose ps` |
| Health | `curl http://localhost:8080/actuator/health` |
| Rebuild | `docker-compose up -d --build` |

---

**Happy Deploying! 🚀**

