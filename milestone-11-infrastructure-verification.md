# Milestone 11 - Infrastructure Recovery & Runtime Startup Verification

## EXECUTED COMMANDS

### Static Configuration Analysis
```powershell
# Read docker-compose.yml
# Read all 9 Dockerfiles
# Checked for JAR files in target directories
```

### Maven Build
```powershell
cd c:\Users\acer\Downloads\booking-system\booking-system
.\mvnw.cmd clean package -DskipTests
```

**Result:** BUILD SUCCESS (46.743s)
- common: SUCCESS
- gateway-service: SUCCESS
- auth-service: SUCCESS
- movie-service: SUCCESS
- theatre-service: SUCCESS
- show-service: SUCCESS
- booking-service: SUCCESS
- payment-service: SUCCESS
- notification-service: SUCCESS

### Docker Engine Check
```powershell
docker version
```

**Result:** FAILED - Docker Desktop Linux engine not running

## CONTAINER LIST (Expected)

All 13 containers defined in docker-compose.yml:
1. vibecheck-mysql (MySQL 8.0)
2. vibecheck-zookeeper (Confluent Zookeeper 7.5.0)
3. vibecheck-kafka (Confluent Kafka 7.5.0)
4. vibecheck-redis (Redis 7-alpine)
5. vibecheck-gateway (gateway-service)
6. vibecheck-auth (auth-service)
7. vibecheck-movie (movie-service)
8. vibecheck-theatre (theatre-service)
9. vibecheck-show (show-service)
10. vibecheck-booking (booking-service)
11. vibecheck-payment (payment-service)
12. vibecheck-notification (notification-service)

## HEALTH STATUS

**BLOCKER:** Cannot verify - Docker engine not running

## EXPOSED PORTS

| Service | Internal Port | External Port |
|---------|---------------|---------------|
| MySQL | 3306 | 3306 |
| Zookeeper | 2181 | 2181 |
| Kafka | 29092, 9092 | 29092, 9092 |
| Redis | 6379 | 6379 |
| Gateway | 8079 | 8079 |
| Auth | 8080 | 8080 |
| Movie | 8081 | 8081 |
| Theatre | 8082 | 8082 |
| Show | 8083 | 8083 |
| Booking | 8084 | 8084 |
| Payment | 8085 | 8085 |
| Notification | 8086 | 8086 |

## INFRASTRUCTURE CONNECTIVITY RESULTS

**BLOCKER:** Cannot verify - Docker engine not running

## STARTUP FAILURES ENCOUNTERED

**BLOCKER:** Docker Desktop Linux engine unavailable

Error message:
```
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine; 
check if the path is correct and if the daemon is running: 
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
```

## FIXES APPLIED

None - runtime verification blocked by Docker engine unavailability.

## STATIC CONFIGURATION VERIFICATION

### Services Included
- ✅ common (library, not a service)
- ✅ gateway-service
- ✅ auth-service
- ✅ movie-service
- ✅ theatre-service
- ✅ show-service
- ✅ booking-service
- ✅ payment-service
- ✅ notification-service

### Infrastructure Dependencies
- ✅ MySQL 8.0 configured with healthcheck
- ✅ Redis 7-alpine configured with healthcheck
- ✅ Zookeeper 7.5.0 configured with healthcheck
- ✅ Kafka 7.5.0 configured with healthcheck
- ✅ Kafka uses Zookeeper (not KRaft mode)

### Environment Variables
- ✅ SPRING_PROFILES_ACTIVE: local
- ✅ Database URLs: jdbc:mysql://mysql:3306/vibecheck_{service}
- ✅ Redis: SPRING_REDIS_HOST=redis, SPRING_REDIS_PORT=6379
- ✅ Kafka: SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
- ✅ Payment gateway configuration with defaults
- ✅ Notification service mail/SMS configuration with defaults

### Service Dependencies
- ✅ All application services depend on mysql (health condition)
- ✅ All application services depend on redis (health condition)
- ✅ All application services depend on kafka (health condition)
- ✅ Kafka depends on zookeeper (health condition)

### Healthchecks
- ✅ MySQL: mysqladmin ping
- ✅ Zookeeper: nc -z localhost 2181
- ✅ Kafka: kafka-broker-api-versions --bootstrap-server localhost:9092
- ✅ Redis: redis-cli ping
- ✅ All services: curl -f http://localhost:{port}/actuator/health

### Networks
- ✅ vibecheck-network: bridge driver, 172.28.0.0/16 subnet

### Volumes
- ✅ mysql_data
- ✅ zookeeper_data
- ✅ zookeeper_logs
- ✅ kafka_data
- ✅ redis_data

### Dockerfiles
- ✅ All use eclipse-temurin:21-jre
- ✅ All include curl for healthchecks
- ✅ Proper EXPOSE ports
- ✅ JAR files successfully built

## FINAL VERIFICATION EVIDENCE

**BLOCKER:** Cannot perform runtime verification without Docker engine.

Static configuration analysis shows:
- All required services are defined
- Infrastructure dependencies are properly configured
- Healthchecks are in place
- Dependencies are correctly ordered
- JAR files are built successfully
- Dockerfiles are properly configured

## FINAL PASS/FAIL DECISION

**FAIL** - Runtime verification cannot be completed due to Docker Desktop Linux engine being unavailable.

## REQUIRED ACTIONS TO COMPLETE

1. Start Docker Desktop
2. Ensure Linux engine is running
3. Execute:
   ```powershell
   docker compose down -v
   docker compose build --no-cache
   docker compose up -d
   docker compose ps
   docker compose logs --tail=200
   ```
4. Verify all containers reach healthy state
5. Verify Spring Boot services start successfully
6. Verify database, Redis, and Kafka connectivity
7. Run tests: `./mvnw clean test`
