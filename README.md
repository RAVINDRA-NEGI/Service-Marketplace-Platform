# Service Marketplace Platform

A Spring Boot + Thymeleaf web app where clients browse professionals, book services, and leave reviews.

## Make this project public safely

Before publishing this repository or deploying to a public server:

1. **Do not commit secrets** (`.env`, API keys, DB passwords).
2. **Use non-root DB credentials** for the app account.
3. **Keep uploads out of git** and store them in a persistent volume/object storage.
4. **Run behind HTTPS** (Nginx/Caddy/reverse proxy).
5. **Set strong passwords** in your `.env` file.

## Quick Deploy with Docker (Recommended)

### Prerequisites
- Docker Desktop or Docker Engine + Compose plugin

### 1) Create environment file
```bash
cp .env.example .env
# then edit .env with strong passwords
```

### 2) Start everything
```bash
docker compose up -d --build
```

### 3) Open the app
- App: http://localhost:8080

### 4) Stop everything
```bash
docker compose down
```

If you also want to remove persisted DB and uploads data:
```bash
docker compose down -v
```

## Public VM deployment (Ubuntu example)

```bash
git clone <your-public-repo-url>
cd Service-Marketplace-Platform
cp .env.example .env
# edit .env

docker compose up -d --build
```

Then place Nginx in front of `localhost:8080` and enable TLS (Let's Encrypt).

## Manual Deploy (without Docker)

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
