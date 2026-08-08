# Docker Local Infrastructure Setup

This document provides complete instructions for setting up and running the VibeCheck movie booking platform using Docker Compose.

## Overview

The Docker infrastructure includes:

- **MySQL 8.0** - Primary database for all microservices
- **Apache Kafka 7.5.0** - Event streaming platform with Zookeeper
- **Redis 7** - Caching layer
- **8 Microservices** - Gateway, Auth, Movie, Theatre, Show, Booking, Payment, Notification

## Prerequisites

- Docker Desktop 4.0+ (or Docker Engine 20.10+)
- Docker Compose 2.0+
- At least 8GB RAM available for Docker
- 20GB free disk space

## Quick Start

### 1. Clone and Navigate

```bash
cd booking-system
```

### 2. Build Application JARs

```bash
cd booking-system
./mvnw clean package -DskipTests
cd ..
```

### 3. Configure Environment Variables

```bash
cp booking-system/.env.example .env
```

Edit `.env` file with your specific configurations (optional for local development with defaults).

### 4. Start All Services

```bash
docker-compose up -d
```

### 5. Verify Services are Running

```bash
docker-compose ps
```

All services should show "Up" status.

### 6. Check Health Status

```bash
# Check all service health
docker-compose ps

# Check specific service health
curl http://localhost:8079/actuator/health  # Gateway
curl http://localhost:8080/actuator/health  # Auth
curl http://localhost:8081/actuator/health  # Movie
curl http://localhost:8082/actuator/health  # Theatre
curl http://localhost:8083/actuator/health  # Show
curl http://localhost:8084/actuator/health  # Booking
curl http://localhost:8085/actuator/health  # Payment
curl http://localhost:8086/actuator/health  # Notification
```

## Service Ports

| Service | Internal Port | External Port |
|---------|---------------|---------------|
| PostgreSQL | 5432 | 5432 |
| Zookeeper | 2181 | 2181 |
| Kafka | 29092, 9092 | 29092, 9092 |
| Redis | 6379 | 6379 |
| Gateway Service | 8079 | 8079 |
| Auth Service | 8080 | 8080 |
| Movie Service | 8081 | 8081 |
| Theatre Service | 8082 | 8082 |
| Show Service | 8083 | 8083 |
| Booking Service | 8084 | 8084 |
| Payment Service | 8085 | 8085 |
| Notification Service | 8086 | 8086 |

## Architecture

### Startup Order

Services start in the following order with health check dependencies:

1. **Infrastructure Layer**
   - MySQL (with health check)
   - Zookeeper (with health check)
   - Kafka (depends on Zookeeper health)
   - Redis (with health check)

2. **Application Layer** (all depend on infrastructure health)
   - Gateway Service
   - Auth Service
   - Movie Service
   - Theatre Service
   - Show Service
   - Booking Service
   - Payment Service
   - Notification Service

### Network Isolation

All services run on a dedicated Docker bridge network `vibecheck-network` (subnet: 172.28.0.0/16) for secure inter-service communication.

### Volume Persistence

Data persistence is configured for:
- `mysql_data` - MySQL data
- `zookeeper_data` - Zookeeper data
- `zookeeper_logs` - Zookeeper logs
- `kafka_data` - Kafka data
- `redis_data` - Redis data with AOF enabled

## Database Setup

The Docker infrastructure uses an external MySQL instance with pre-created databases. Before starting the services, ensure the following databases exist:

```sql
CREATE DATABASE vibecheck_auth;
CREATE DATABASE vibecheck_booking;
CREATE DATABASE vibecheck_gateway;
CREATE DATABASE vibecheck_movie;
CREATE DATABASE vibecheck_notification;
CREATE DATABASE vibecheck_payment;
CREATE DATABASE vibecheck_show;
CREATE DATABASE vibecheck_theatre;
```

**MySQL Credentials**:
- Username: `root`
- Password: `Krish@123`
- Port: `3306`

The MySQL container in docker-compose.yml is configured with these credentials and will connect to your existing MySQL instance or run a new one with the specified databases.

### docker-compose.yml

Main orchestration file defining all services, networks, volumes, and health checks.

### .env.example

Template for environment variables. Copy to `.env` and customize:

```bash
# Database Configuration
MYSQL_ROOT_PASSWORD=Krish@123

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379

# Payment Gateway (optional)
PAYMENT_GATEWAY_PROVIDER=MOCK

# Email Configuration (optional)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### application-local.yml

Each service has a local profile configuration that:
- Uses Docker service names for inter-service communication
- Connects to existing MySQL databases (pre-created)
- Uses root credentials for database access
- Uses environment variables for sensitive data
- Configures health endpoints for Docker health checks

**Note**: The infrastructure assumes MySQL databases are pre-created with the following names:
- vibecheck_auth
- vibecheck_booking
- vibecheck_gateway
- vibecheck_movie
- vibecheck_notification
- vibecheck_payment
- vibecheck_show
- vibecheck_theatre

## Common Operations

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f gateway-service
docker-compose logs -f postgres
```

### Stop Services

```bash
docker-compose stop
```

### Stop and Remove Containers

```bash
docker-compose down
```

### Stop and Remove Containers + Volumes

```bash
docker-compose down -v
```

### Rebuild Specific Service

```bash
docker-compose up -d --build gateway-service
```

### Rebuild All Services

```bash
docker-compose up -d --build
```

### Scale Services (if needed)

```bash
docker-compose up -d --scale gateway-service=2
```

## Troubleshooting

### Services Not Starting

1. Check if ports are already in use:
```bash
netstat -ano | findstr :8079
```

2. Check Docker logs:
```bash
docker-compose logs [service-name]
```

3. Verify health checks:
```bash
docker inspect --format='{{.State.Health.Status}}' [container-name]
```

### Database Connection Issues

1. Verify MySQL is healthy:
```bash
docker-compose exec mysql mysqladmin ping -h localhost -uroot -pKrish@123
```

2. Check databases exist:
```bash
docker-compose exec mysql mysql -uroot -pKrish@123 -e "SHOW DATABASES;"
```

Expected databases:
- vibecheck_auth
- vibecheck_booking
- vibecheck_gateway
- vibecheck_movie
- vibecheck_notification
- vibecheck_payment
- vibecheck_show
- vibecheck_theatre

3. View MySQL logs:
```bash
docker-compose logs mysql
```

### Kafka Connection Issues

1. Verify Kafka is healthy:
```bash
docker-compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

2. Check Zookeeper connection:
```bash
docker-compose exec zookeeper nc -z localhost 2181
```

### Redis Connection Issues

1. Verify Redis is healthy:
```bash
docker-compose exec redis redis-cli ping
```

2. Check Redis logs:
```bash
docker-compose logs redis
```

### Memory Issues

If you encounter memory issues:

1. Increase Docker memory allocation in Docker Desktop settings
2. Reduce JVM heap size in Dockerfile if needed:
```dockerfile
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
```

### Rebuild After Code Changes

1. Rebuild JARs:
```bash
cd booking-system
./mvnw clean package -DskipTests
cd ..
```

2. Rebuild Docker containers:
```bash
docker-compose up -d --build
```

## Development Workflow

### Running Individual Services

To run a single service with dependencies:

```bash
# Start infrastructure only
docker-compose up -d postgres zookeeper kafka redis

# Start specific service
docker-compose up -d gateway-service
```

### Hot Reloading (Not Recommended in Docker)

For development with hot reloading, consider running services locally and only using Docker for infrastructure:

```bash
# Start only infrastructure
docker-compose up -d postgres zookeeper kafka redis

# Run services locally with local profile
cd booking-system/gateway-service
../mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Health Check Details

All services implement health checks via Spring Boot Actuator:

- **Endpoint**: `/actuator/health`
- **Interval**: 30 seconds
- **Timeout**: 10 seconds
- **Retries**: 5
- **Start Period**: 60 seconds

Infrastructure services have custom health checks:
- **PostgreSQL**: `pg_isready` command
- **Kafka**: `kafka-broker-api-versions` command
- **Redis**: `redis-cli ping` command
- **Zookeeper**: `nc -z` port check

## Monitoring

### Service Status

```bash
docker-compose ps
```

### Resource Usage

```bash
docker stats
```

### Service Metrics

Access Spring Boot Actuator endpoints:
- Health: `http://localhost:[port]/actuator/health`
- Metrics: `http://localhost:[port]/actuator/metrics`
- Prometheus: `http://localhost:[port]/actuator/prometheus`

### API Documentation

Access Swagger UI for each service:
- Gateway: `http://localhost:8079/swagger-ui.html`
- Auth: `http://localhost:8080/swagger-ui.html`
- Movie: `http://localhost:8081/swagger-ui.html`
- Theatre: `http://localhost:8082/swagger-ui.html`
- Show: `http://localhost:8083/swagger-ui.html`
- Booking: `http://localhost:8084/swagger-ui.html`
- Payment: `http://localhost:8085/swagger-ui.html`
- Notification: `http://localhost:8086/swagger-ui.html`

## Production Considerations

This setup is for local development. For production:

1. **Security**
   - Change default passwords
   - Use secrets management (e.g., HashiCorp Vault)
   - Enable TLS/SSL for all communications
   - Restrict network access

2. **Performance**
   - Adjust resource limits in docker-compose.yml
   - Enable connection pooling
   - Configure appropriate JVM heap sizes
   - Use external managed services (RDS/Aurora, ElastiCache, MSK)

3. **Monitoring**
   - Add centralized logging (ELK, Loki)
   - Implement distributed tracing (Jaeger, Zipkin)
   - Set up alerting (Prometheus Alertmanager)
   - Use APM tools (New Relic, Datadog)

4. **High Availability**
   - Configure service replicas
   - Use load balancers
   - Implement backup strategies
   - Multi-region deployment

## Cleanup

### Remove All Containers and Networks

```bash
docker-compose down
```

### Remove All Containers, Networks, and Volumes

```bash
docker-compose down -v
```

### Remove Docker Images

```bash
docker rmi $(docker images -q vibecheck-*)
```

### Complete Reset

```bash
docker-compose down -v
docker system prune -a
```

## Support

For issues or questions:
1. Check service logs: `docker-compose logs [service]`
2. Verify health status: `docker-compose ps`
3. Review this troubleshooting section
4. Check individual service documentation in `booking-system/docs/`
