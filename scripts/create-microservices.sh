#!/usr/bin/env bash
set -euo pipefail

BOOT_VERSION="3.5.8"
CLOUD_VERSION="2025.0.3"

create_service() {
  local service="$1"
  local package="$2"
  local port="$3"
  local artifact="$4"
  local gateway="${5:-false}"
  local dir="backend/$service"
  local package_path="${package//./\/}"

  mkdir -p "$dir/src/main/java/$package_path"
  mkdir -p "$dir/src/main/resources"

  if [[ "$gateway" == "true" ]]; then
    cat > "$dir/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.neo4flix</groupId>
  <artifactId>$artifact</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>$BOOT_VERSION</version>
    <relativePath/>
  </parent>

  <properties>
    <java.version>21</java.version>
    <spring-cloud.version>$CLOUD_VERSION</spring-cloud.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>\${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
EOF
  else
    cat > "$dir/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.neo4flix</groupId>
  <artifactId>$artifact</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>$BOOT_VERSION</version>
    <relativePath/>
  </parent>

  <properties>
    <java.version>21</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
EOF
  fi

  local class_name
  case "$service" in
    user-service) class_name="UserServiceApplication" ;;
    movie-service) class_name="MovieServiceApplication" ;;
    rating-service) class_name="RatingServiceApplication" ;;
    recommendation-service) class_name="RecommendationServiceApplication" ;;
    api-gateway) class_name="ApiGatewayApplication" ;;
    *) echo "Unknown service: $service"; exit 1 ;;
  esac

  cat > "$dir/src/main/java/$package_path/$class_name.java" <<EOF
package $package;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class $class_name {
    public static void main(String[] args) {
        SpringApplication.run($class_name.class, args);
    }
}
EOF

  cat > "$dir/src/main/resources/application.yml" <<EOF
spring:
  application:
    name: $service
  profiles:
    default: dev

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
EOF

  cat > "$dir/src/main/resources/application-dev.yml" <<EOF
server:
  port: $port

logging:
  level:
    root: INFO
    com.neo4flix: DEBUG
EOF

  cat > "$dir/src/main/resources/application-prod.yml" <<EOF
server:
  port: \${SERVER_PORT:8080}

logging:
  level:
    root: WARN
    com.neo4flix: INFO
EOF

  echo "Created $service"
}

create_service "user-service" "com.neo4flix.userservice" 8081 "user-service"
create_service "movie-service" "com.neo4flix.movieservice" 8082 "movie-service"
create_service "rating-service" "com.neo4flix.ratingservice" 8083 "rating-service"
create_service "recommendation-service" "com.neo4flix.recommendationservice" 8084 "recommendation-service"
create_service "api-gateway" "com.neo4flix.gateway" 8080 "api-gateway" true

echo "All five applications created."
