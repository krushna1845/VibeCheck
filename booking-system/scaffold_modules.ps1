$root = 'C:\Users\acer\Downloads\booking-system\booking-system'
$services = @(
    @{ Name='movie-service'; Package='movie'; Class='Movie' },
    @{ Name='theatre-service'; Package='theatre'; Class='Theatre' },
    @{ Name='show-service'; Package='show'; Class='Show' },
    @{ Name='booking-service'; Package='booking'; Class='Booking' },
    @{ Name='payment-service'; Package='payment'; Class='Payment' },
    @{ Name='notification-service'; Package='notification'; Class='Notification' }
)

$packages = @(
    'config','controller','service','service.impl','repository','entity',
    'dto.request','dto.response','mapper','exception','security',
    'kafka.producer','kafka.consumer','kafka.event','redis','scheduler','validator','util','constant','enums'
)

foreach ($svc in $services) {
    $moduleDir = Join-Path $root $svc.Name
    $javaRoot = Join-Path $moduleDir 'src/main/java/com/krushna/moviebooking/' + $svc.Package
    New-Item -ItemType Directory -Path $javaRoot -Force | Out-Null

    $appClass = $svc.Class + 'Application'
    $appContent = @"
package com.krushna.moviebooking.$($svc.Package);

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class $appClass {
    public static void main(String[] args) {
        SpringApplication.run($appClass.class, args);
    }
}
"@
    Set-Content -Path (Join-Path $javaRoot ($appClass + '.java')) -Value $appContent -Encoding UTF8

    foreach ($pkg in $packages) {
        $pkgDir = Join-Path $javaRoot $pkg
        New-Item -ItemType Directory -Path $pkgDir -Force | Out-Null
        $className = switch ($pkg) {
            'config' { $svc.Class + 'Config' }
            'controller' { $svc.Class + 'Controller' }
            'service' { $svc.Class + 'Service' }
            'service.impl' { $svc.Class + 'ServiceImpl' }
            'repository' { $svc.Class + 'Repository' }
            'entity' { $svc.Class + 'Entity' }
            'dto.request' { 'Create' + $svc.Class + 'Request' }
            'dto.response' { $svc.Class + 'Response' }
            'mapper' { $svc.Class + 'Mapper' }
            'exception' { $svc.Class + 'Exception' }
            'security' { 'SecurityConfig' }
            'kafka.producer' { $svc.Class + 'EventProducer' }
            'kafka.consumer' { $svc.Class + 'EventConsumer' }
            'kafka.event' { $svc.Class + 'Event' }
            'redis' { $svc.Class + 'RedisRepository' }
            'scheduler' { $svc.Class + 'Scheduler' }
            'validator' { $svc.Class + 'Validator' }
            'util' { $svc.Class + 'Utils' }
            'constant' { $svc.Class + 'Constants' }
            'enums' { $svc.Class + 'Status' }
            default { $svc.Class + 'Placeholder' }
        }
        $filePath = Join-Path $pkgDir ($className + '.java')
        if (-not (Test-Path $filePath)) {
            $content = @"
package com.krushna.moviebooking.$($svc.Package).$pkg;

public class $className {}
"@
            Set-Content -Path $filePath -Value $content -Encoding UTF8
        }
    }

    $resourcesDir = Join-Path $moduleDir 'src/main/resources'
    New-Item -ItemType Directory -Path $resourcesDir -Force | Out-Null
    $appYml = @"
server:
  port: 8080
spring:
  application:
    name: $($svc.Name)
  datasource:
    url: jdbc:postgresql://localhost:5432/$($svc.Name -replace '-', '_')
    username: postgres
    password: postgres
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
  redis:
    host: localhost
    port: 6379
management:
  endpoints:
    web:
      exposure:
        include: health,info
"@
    Set-Content -Path (Join-Path $resourcesDir 'application.yml') -Value $appYml -Encoding UTF8
    Set-Content -Path (Join-Path $resourcesDir 'application-dev.yml') -Value @"
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/$($svc.Name -replace '-', '_')_dev
    username: postgres
    password: postgres
  kafka:
    bootstrap-servers: localhost:9092
  redis:
    host: localhost
    port: 6379
"@ -Encoding UTF8
    Set-Content -Path (Join-Path $resourcesDir 'application-prod.yml') -Value @"
spring:
  datasource:
    url: `${DB_URL}
    username: `${DB_USERNAME}
    password: `${DB_PASSWORD}
  kafka:
    bootstrap-servers: `${KAFKA_BOOTSTRAP_SERVERS}
  redis:
    host: `${REDIS_HOST}
    port: `${REDIS_PORT:6379}
"@ -Encoding UTF8

    $migrationDir = Join-Path $resourcesDir 'db/migration'
    New-Item -ItemType Directory -Path $migrationDir -Force | Out-Null
    Set-Content -Path (Join-Path $migrationDir 'V1__init_schema.sql') -Value @"
CREATE TABLE IF NOT EXISTS $($svc.Name -replace '-', '_')_domain (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
"@ -Encoding UTF8

    Set-Content -Path (Join-Path $moduleDir 'Dockerfile') -Value @"
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/$($svc.Name)-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
"@ -Encoding UTF8
}
