# Judge API Service

> **Archived.** This was the My Online Judge monolith. It was split into services between 2026-09-27 and
> 2026-10-04; what remained of it — submissions, judging, languages, judge servers — continues, with this
> repository's full history, in [oj-submission-service](https://github.com/My-Online-Judge/oj-submission-service).
> Users and tokens moved to oj-identity-service, problems to oj-problem-service; judge-deployment runs them all.

Backend API service for the Online Judge system. This service manages submissions, languages and judge servers, and integrates with the judging engine. Problems and their test cases live in oj-problem-service, which it calls over gRPC (`ProblemCatalog`); users and tokens live in oj-identity-service.

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
