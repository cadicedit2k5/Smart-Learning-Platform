# Smart Learning Platform

Smart Learning Platform là nền tảng hỗ trợ quản lý khóa học,
tài liệu học tập và tương tác với AI Tutor dựa trên RAG.

## Technologies

### Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Kafka
- MinIO

### AI Engine
- Python
- FastAPI
- LangChain
- Vector Database / Embedding
- RAG

### Frontend
- Vue 3
- TypeScript
- Vite
- Pinia

## Main Modules

- System Service: authentication and user management
- Core Service: course, document and learning management
- AI Service: AI conversation management
- Python AI Engine: document processing and RAG
- Gateway: API Gateway
- Storage: MinIO integration

## Requirements

- Java 21
- Node.js 22+
- Python 3.x
- Docker / Docker Compose

## Run Project

### Requirements

- Git
- Docker Desktop / Docker Engine
- Google Gemini API Key

### 1. Clone Project

```bash
git clone -b refactor/document-course-only --single-branch https://github.com/cadicedit2k5/Smart-Learning-Platform.git
cd Smart-Learning-Platform
```

### 2. Configure Environment

Create `.env` from `.env.example`.

**Windows PowerShell**

```powershell
Copy-Item .env.example .env
```

**Linux / macOS**

```bash
cp .env.example .env
```

Open `.env` and provide your Gemini API Key:

```env
GOOGLE_API_KEY=YOUR_GOOGLE_GEMINI_API_KEY
```

### 3. Start the Application

```bash
docker compose up --build -d
```

Check container status:

```bash
docker compose ps -a
```

Docker Compose will automatically start the backend services, Python AI Engine, frontend, PostgreSQL, Kafka, MinIO, and load demo data.

### 4. Access the Application

| Service | URL |
| --- | --- |
| Frontend | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| MinIO Console | http://localhost:9001 |

### Demo Accounts

| Role | Email | Password |
| --- | --- | --- |
| Admin | admin@gmail.com | Admin@123456 |
| Lecturer | lecturer01@example.com | 12345678 |
| Student | student01@example.com | 12345678 |

### Stop the Application

```bash
docker compose down
```

To reset all data and initialize the project again:

```bash
docker compose down -v
docker compose up --build -d
```

> `docker compose down -v` removes all Docker volumes, including PostgreSQL and MinIO data.