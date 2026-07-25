from pathlib import Path
import textwrap

root = Path(r"c:\Users\acer\Downloads\booking-system\booking-system")

parent_pom = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>

    <groupId>com.krushna</groupId>
    <artifactId>movie-booking-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>movie-booking-platform</name>
    <description>Distributed movie ticket booking platform</description>

    <modules>
        <module>common</module>
        <module>gateway-service</module>
        <module>auth-service</module>
        <module>movie-service</module>
        <module>theatre-service</module>
        <module>show-service</module>
        <module>booking-service</module>
        <module>payment-service</module>
        <module>notification-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.release>21</maven.compiler.release>
        <spring-boot.version>4.1.0</spring-boot.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                    <configuration>
                        <release>${maven.compiler.release}</release>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
'''
(root / 'pom.xml').write_text(parent_pom, encoding='utf-8')

services = [
    ('common', 'common', 'common', 'common'),
    ('gateway-service', 'gateway-service', 'gateway', 'gateway-service'),
    ('auth-service', 'auth-service', 'auth', 'auth-service'),
    ('movie-service', 'movie-service', 'movie', 'movie-service'),
    ('theatre-service', 'theatre-service', 'theatre', 'theatre-service'),
    ('show-service', 'show-service', 'show', 'show-service'),
    ('booking-service', 'booking-service', 'booking', 'booking-service'),
    ('payment-service', 'payment-service', 'payment', 'payment-service'),
    ('notification-service', 'notification-service', 'notification', 'notification-service'),
]

common_packages = [
    'config', 'controller', 'service', 'service.impl', 'repository', 'entity',
    'dto.request', 'dto.response', 'mapper', 'exception', 'security',
    'kafka.producer', 'kafka.consumer', 'kafka.event', 'redis', 'scheduler',
    'validator', 'util', 'constant', 'enums'
]

for artifact_id, module_name, package_segment, display_name in services:
    module_dir = root / module_name
    module_dir.mkdir(parents=True, exist_ok=True)

    if module_name == 'common':
        pom = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.krushna</groupId>
        <artifactId>movie-booking-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>common</artifactId>
    <name>common</name>
    <description>Shared domain types and utilities</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
    </dependencies>
</project>
'''
    else:
        pom = f'''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.krushna</groupId>
        <artifactId>movie-booking-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>{artifact_id}</artifactId>
    <name>{artifact_id}</name>
    <description>{display_name} service for the movie booking platform</description>

    <dependencies>
        <dependency>
            <groupId>com.krushna</groupId>
            <artifactId>common</artifactId>
            <version>${{project.version}}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
'''
    (module_dir / 'pom.xml').write_text(pom, encoding='utf-8')

    src_main = module_dir / 'src' / 'main'
    src_main_java = src_main / 'java'
    src_main_resources = src_main / 'resources'
    src_main_resources.mkdir(parents=True, exist_ok=True)
    package_base = f'com.krushna.moviebooking.{package_segment}'
    java_root = src_main_java / Path(*package_base.split('.'))
    java_root.mkdir(parents=True, exist_ok=True)

    app_name = 'CommonModuleApplication' if module_name == 'common' else ''.join(part.capitalize() for part in package_segment.split('-')) + 'Application'
    (java_root / f'{app_name}.java').write_text(textwrap.dedent(f'''\
        package {package_base};

        import org.springframework.boot.SpringApplication;
        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class {app_name} {{
            public static void main(String[] args) {{
                SpringApplication.run({app_name}.class, args);
            }}
        }}
    '''), encoding='utf-8')

    for pkg in common_packages:
        pkg_path = java_root / Path(*pkg.split('.'))
        pkg_path.mkdir(parents=True, exist_ok=True)

        if pkg == 'service':
            class_name = f'{package_segment.capitalize()}Service'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public interface {class_name} {{}}
'''
        elif pkg == 'service.impl':
            class_name = f'{package_segment.capitalize()}ServiceImpl'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import org.springframework.stereotype.Service;

@Service
public class {class_name} implements {package_segment.capitalize()}Service {{}}
'''
        elif pkg == 'controller':
            class_name = f'{package_segment.capitalize()}Controller'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import org.springframework.web.bind.annotation.RestController;

@RestController
public class {class_name} {{}}
'''
        elif pkg == 'config':
            class_name = f'{package_segment.capitalize()}Config'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import org.springframework.context.annotation.Configuration;

@Configuration
public class {class_name} {{}}
'''
        elif pkg == 'repository':
            class_name = f'{package_segment.capitalize()}Repository'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface {class_name} extends JpaRepository<{package_segment.capitalize()}Entity, Long> {{}}
'''
        elif pkg == 'entity':
            class_name = f'{package_segment.capitalize()}Entity'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class {class_name} {{
    @Id
    private Long id;
}}
'''
        elif pkg == 'dto.request':
            class_name = f'Create{package_segment.capitalize()}Request'
            if module_name == 'common':
                content = f'''package {package_base}.{pkg.replace('.', '.')};

public record {class_name}(String requestId, String description) {{}}
'''
            else:
                content = f'''package {package_base}.{pkg.replace('.', '.')};

public record {class_name}(String requestId, String description) {{}}
'''
        elif pkg == 'dto.response':
            class_name = f'{package_segment.capitalize()}Response'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public record {class_name}(String id, String status) {{}}
'''
        elif pkg == 'mapper':
            class_name = f'{package_segment.capitalize()}Mapper'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public interface {class_name}<D, E> {{
    D toDto(E entity);
    E toEntity(D dto);
}}
'''
        elif pkg == 'exception':
            class_name = f'{package_segment.capitalize()}Exception'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public class {class_name} extends RuntimeException {{
    public {class_name}(String message) {{
        super(message);
    }}
}}
'''
        elif pkg == 'security':
            class_name = 'SecurityConfig'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import org.springframework.context.annotation.Configuration;

@Configuration
public class {class_name} {{}}
'''
        elif pkg == 'kafka.producer':
            class_name = f'{package_segment.capitalize()}EventProducer'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class {class_name} {{
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public {class_name}(KafkaTemplate<String, Object> kafkaTemplate) {{
        this.kafkaTemplate = kafkaTemplate;
    }}
}}
'''
        elif pkg == 'kafka.consumer':
            class_name = f'{package_segment.capitalize()}EventConsumer'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class {class_name} {{
    @KafkaListener(topics = "default-topic")
    public void listen(String message) {{
        // placeholder consumer
    }}
}}
'''
        elif pkg == 'kafka.event':
            class_name = f'{package_segment.capitalize()}Event'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public record {class_name}(String eventId, String source) {{}}
'''
        elif pkg == 'redis':
            class_name = f'{package_segment.capitalize()}RedisRepository'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import org.springframework.stereotype.Repository;

@Repository
public class {class_name} {{}}
'''
        elif pkg == 'scheduler':
            class_name = f'{package_segment.capitalize()}Scheduler'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class {class_name} {{
    @Scheduled(cron = "0 * * * * *")
    public void run() {{
        // placeholder scheduled job
    }}
}}
'''
        elif pkg == 'validator':
            class_name = f'{package_segment.capitalize()}Validator'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public class {class_name} {{}}
'''
        elif pkg == 'util':
            class_name = f'{package_segment.capitalize()}Utils'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public final class {class_name} {{
    private {class_name}() {{}}

    public static String normalize(String value) {{
        return value == null ? "" : value.trim();
    }}
}}
'''
        elif pkg == 'constant':
            class_name = f'{package_segment.capitalize()}Constants'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public final class {class_name} {{
    private {class_name}() {{}}
    public static final String SERVICE_NAME = "{display_name}";
}}
'''
        elif pkg == 'enums':
            class_name = f'{package_segment.capitalize()}Status'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public enum {class_name} {{
    PENDING,
    COMPLETED,
    FAILED
}}
'''
        else:
            class_name = f'{package_segment.capitalize()}Placeholder'
            content = f'''package {package_base}.{pkg.replace('.', '.')};

public class {class_name} {{}}
'''

        (pkg_path / f'{class_name}.java').write_text(content, encoding='utf-8')

    (src_main_resources / 'application.yml').write_text(textwrap.dedent(f'''\
        server:
          port: 8080
        spring:
          application:
            name: {artifact_id}
          datasource:
            url: jdbc:postgresql://localhost:5432/{artifact_id.replace('-', '_')}
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
    '''), encoding='utf-8')
    (src_main_resources / 'application-dev.yml').write_text(textwrap.dedent(f'''\
        spring:
          datasource:
            url: jdbc:postgresql://localhost:5432/{artifact_id.replace('-', '_')}_dev
            username: postgres
            password: postgres
          kafka:
            bootstrap-servers: localhost:9092
          redis:
            host: localhost
            port: 6379
    '''), encoding='utf-8')
    (src_main_resources / 'application-prod.yml').write_text(textwrap.dedent(f'''\
        spring:
          datasource:
            url: ${{DB_URL}}
            username: ${{DB_USERNAME}}
            password: ${{DB_PASSWORD}}
          kafka:
            bootstrap-servers: ${{KAFKA_BOOTSTRAP_SERVERS}}
          redis:
            host: ${{REDIS_HOST}}
            port: ${{REDIS_PORT:6379}}
    '''), encoding='utf-8')
    migration_dir = src_main_resources / 'db' / 'migration'
    migration_dir.mkdir(parents=True, exist_ok=True)
    (migration_dir / 'V1__init_schema.sql').write_text(textwrap.dedent(f'''\
        CREATE TABLE IF NOT EXISTS {artifact_id.replace('-', '_')}_domain (
            id BIGSERIAL PRIMARY KEY,
            external_id VARCHAR(255) NOT NULL,
            status VARCHAR(50) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    '''), encoding='utf-8')
    (module_dir / 'Dockerfile').write_text(textwrap.dedent(f'''\
        FROM eclipse-temurin:21-jre
        WORKDIR /app
        COPY target/{artifact_id}-*.jar app.jar
        EXPOSE 8080
        ENTRYPOINT ["java", "-jar", "app.jar"]
    '''), encoding='utf-8')
    (module_dir / 'README.md').write_text(f'# {artifact_id}\n\nPlaceholder microservice skeleton for the movie booking platform.\n', encoding='utf-8')

(root / 'README.md').write_text(textwrap.dedent('''\
    # Movie Booking Platform

    This workspace contains an enterprise-grade Maven multi-module scaffold for a distributed movie ticket booking platform.

    Modules:
    - common
    - gateway-service
    - auth-service
    - movie-service
    - theatre-service
    - show-service
    - booking-service
    - payment-service
    - notification-service
'''), encoding='utf-8')

print('scaffold generated')
