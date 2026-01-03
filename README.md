# Judge API Service

Backend API service for the Online Judge system. This service manages problems, submissions, and integrates with the judging engine.

## 🛠 Tech Stack

- **Java**: 17
- **Framework**: Spring Boot 3.4.1
- **Database**: PostgreSQL
- **Build Tool**: Maven 3.5+
- **Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Utilities**: Lombok, MapStruct

## 🚀 Prerequisites

Ensure you have the following installed:

- [JDK 17+](https://www.oracle.com/java/technologies/downloads/)
- [Maven 3.5+](https://maven.apache.org/download.cgi)
- [Docker & Docker Compose](https://docs.docker.com/get-docker/) (Optional, for running dependencies or full stack)

## ⚙️ Build & Run

### 1. Using Maven Wrapper (Recommended)

Run the application with the default `dev` profile:

```bash
./mvnw spring-boot:run
```

### 2. Using Java Jar

Package the application first:

```bash
./mvnw clean package -P dev
```

Run the generated jar:

```bash
java -jar target/backend-service.jar
```

### 3. Using Docker

Build the image:

```bash
docker build -t backend-service .
```

Run the container:

```bash
docker run -d -p 8080:8080 backend-service:latest
```

## 📚 API Documentation

Once the application is running, API documentation is available at:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

## 🧪 Testing

Run unit and integration tests:

```bash
./mvnw clean test
```
