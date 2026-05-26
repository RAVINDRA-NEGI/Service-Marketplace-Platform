# Service Marketplace Platform

A Spring Boot + Thymeleaf web app where clients browse professionals, book services, and leave reviews.

## Quick Deploy with Docker (Recommended)

### Prerequisites
- Docker Desktop or Docker Engine + Compose plugin

### 1) Start everything
```bash
docker compose up -d --build
```

### 2) Open the app
- App: http://localhost:8080
- MySQL: localhost:3306 (user: `root`, password: `rootpass`)

### 3) Stop everything
```bash
docker compose down
```

If you also want to remove persisted DB and uploads data:
```bash
docker compose down -v
```

## Manual Deploy (VM/Linux)

### Prerequisites
- Java 21
- Maven 3.9+
- MySQL 8+

### Build and run
```bash
mvn -DskipTests package
java -jar target/Service-Marketplace-Platform-0.0.1-SNAPSHOT.jar
```

Set DB connection via environment variables in production:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- optional: `APP_UPLOAD_DIR`

## Production notes
- Use a non-root DB user in production.
- Put the app behind Nginx/Caddy for TLS.
- Back up both DB data and uploads volume/path.
