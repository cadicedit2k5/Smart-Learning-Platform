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

## Run

Configure environment variables using the provided `.env.example`
and `application.properties.example` files.

Start infrastructure:

docker compose up -d

Then start backend services, Python AI Engine and frontend.