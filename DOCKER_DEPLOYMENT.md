# 🐳 Docker Deployment Guide - Auth Server

This guide provides complete instructions for deploying the Auth Server using Docker.

## 📋 Prerequisites

- Docker Engine 20.10+ 
- Docker Compose 2.0+ (optional, for docker-compose deployment)
- At least 2GB RAM available for the container
- Access to Redis (external or using the provided docker-compose)

## 🚀 Quick Start

### Option 1: Using Docker Compose (Recommended)

1. **Create a `.env` file** with your configuration:

```bash
# Copy the example and fill in your values
cat > .env << 'EOF'
# Redis Configuration
REDIS_URL=redis://redis:6379

# Zitadel Configuration
ZITADEL_INSTANCE_URL=https://your-instance.zitadel.cloud
ZITADEL_ACCESS_TOKEN=your-access-token

# Cookie Security (false for HTTP, true for HTTPS)
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

# Vendor Configuration
VENDOR_ISSUER=your-vendor-issuer
VENDOR_ORGANIZATION_ID=your-org-id
VENDOR_CLIENT_ID=your-client-id
VENDOR_CLIENT_SECRET=your-client-secret
# ... add all other vendor variables

# Consumer Configuration
CONSUMER_ISSUER=your-consumer-issuer
# ... add all consumer variables

# Affiliate Configuration
AFFILIATE_ISSUER=your-affiliate-issuer
# ... add all affiliate variables

# GMS Configuration
GMS_ISSUER=your-gms-issuer
# ... add all GMS variables
EOF
```

2. **Start the services**:

```bash
docker-compose up -d
```

3. **Check logs**:

```bash
docker-compose logs -f auth-server
```

4. **Access the application**:
   - API: http://localhost:8080
   - Health Check: http://localhost:8080/actuator/health

5. **Stop the services**:

```bash
docker-compose down
```

### Option 2: Using Docker Only

1. **Build the image**:

```bash
docker build -t auth-server:latest .
```

2. **Run the container** (with external Redis):

```bash
docker run -d \
  --name auth-server \
  -p 8080:8080 \
  -e REDIS_URL=redis://your-redis-host:6379 \
  -e ZITADEL_INSTANCE_URL=https://your-instance.zitadel.cloud \
  -e ZITADEL_ACCESS_TOKEN=your-access-token \
  -e VENDOR_ISSUER=your-vendor-issuer \
  -e VENDOR_CLIENT_ID=your-client-id \
  -e VENDOR_CLIENT_SECRET=your-client-secret \
  # ... add all other environment variables
  auth-server:latest
```

3. **Check container logs**:

```bash
docker logs -f auth-server
```

## 🔧 Configuration

### Environment Variables

All configuration is done through environment variables. See `application.properties` for default values.

#### Required Variables:
- `REDIS_URL` - Redis connection URL
- `ZITADEL_INSTANCE_URL` - Your Zitadel instance URL
- `ZITADEL_ACCESS_TOKEN` - Zitadel access token

#### Persona-Specific Variables:
Each persona (VENDOR, CONSUMER, AFFILIATE, GMS) requires:
- `{PERSONA}_ISSUER`
- `{PERSONA}_ORGANIZATION_ID`
- `{PERSONA}_CLIENT_ID`
- `{PERSONA}_CLIENT_SECRET`
- `{PERSONA}_REDIRECT_URI`
- `{PERSONA}_LOGOUT_REDIRECT_URI`
- `{PERSONA}_PROJECT_ID`
- `{PERSONA}_MANAGEMENT_TOKEN`
- `{PERSONA}_SESSION_ID_NAME`
- `{PERSONA}_AFTER_LOGIN_REDIRECT_URI`

### JVM Options

The default JVM options are optimized for containers:

```bash
-XX:+UseContainerSupport 
-XX:MaxRAMPercentage=75.0 
-XX:InitialRAMPercentage=50.0
```

You can override them using the `JAVA_OPTS` environment variable:

```bash
docker run -e JAVA_OPTS="-Xmx512m -Xms256m" auth-server:latest
```

## 🏥 Health Checks

The application includes health check endpoints via Spring Boot Actuator:

- **Liveness**: `GET /actuator/health/liveness`
- **Readiness**: `GET /actuator/health/readiness`
- **Full Health**: `GET /actuator/health`

```bash
# Check health
curl http://localhost:8080/actuator/health
```

## 📊 Monitoring

### View Logs

```bash
# Docker Compose
docker-compose logs -f auth-server

# Docker
docker logs -f auth-server
```

### Container Stats

```bash
# Docker Compose
docker-compose stats

# Docker
docker stats auth-server
```

## 🔒 Production Deployment

### Security Checklist

1. **Use HTTPS**: Set `COOKIE_SECURE=true`
2. **Secure Redis**: Use password-protected Redis with TLS
3. **Secrets Management**: Use Docker secrets or environment injection
4. **Non-root User**: Already configured in Dockerfile
5. **Network Isolation**: Use Docker networks
6. **Resource Limits**: Set CPU/memory limits

### Resource Limits Example

```yaml
# docker-compose.yml
services:
  auth-server:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### Using External Redis

If you're using an external Redis (like Redis Cloud), modify the `REDIS_URL`:

```bash
REDIS_URL=redis://default:password@host:port
# or with TLS
REDIS_URL=rediss://default:password@host:port
```

Then remove the Redis service from `docker-compose.yml` or don't use docker-compose.

## 🐛 Troubleshooting

### Application won't start

1. **Check logs**:
   ```bash
   docker-compose logs auth-server
   ```

2. **Verify environment variables**:
   ```bash
   docker-compose config
   ```

3. **Check Redis connection**:
   ```bash
   docker-compose exec redis redis-cli ping
   ```

### Connection refused errors

- Ensure Redis is running and accessible
- Check network connectivity between containers
- Verify `REDIS_URL` is correct

### Out of Memory

- Increase container memory limits
- Adjust `JAVA_OPTS` heap settings
- Check for memory leaks in logs

### Port already in use

```bash
# Change the host port in docker-compose.yml
ports:
  - "8081:8080"  # Use 8081 instead of 8080
```

## 🔄 Updates and Maintenance

### Update the application

```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose up -d --build
```

### Clean up old images

```bash
# Remove dangling images
docker image prune

# Remove specific old images
docker rmi auth-server:old-tag
```

### Backup Redis data

```bash
# If using docker-compose with local Redis
docker-compose exec redis redis-cli BGSAVE
docker cp auth-redis:/data/dump.rdb ./backup/
```

## 📚 Additional Resources

- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Redis Docker Documentation](https://hub.docker.com/_/redis)

## 🆘 Support

For issues related to:
- **Application**: Check application logs and configuration
- **Docker**: Verify Docker installation and permissions
- **Redis**: Check Redis connectivity and configuration
- **Zitadel**: Verify API tokens and instance URL

---

**Happy Deploying! 🚀**

