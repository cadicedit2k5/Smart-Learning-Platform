sequenceDiagram
    actor Lecturer
    participant FE as VueJS Frontend
    participant GW as Spring Cloud Gateway
    participant CORE as Core Service
    participant SYSDB as PostgreSQL system schema
    participant COREDB as PostgreSQL core schema
    participant MINIO as MinIO
    participant KAFKA as Kafka
    participant PY as Python AI Engine
    participant AIDB as PostgreSQL ai_engine schema

    Lecturer->>FE: Upload document
    FE->>GW: POST /api/core/courses/{courseId}/documents + JWT
    GW->>GW: Verify JWT
    GW->>CORE: Forward request

    CORE->>CORE: Extract userId, roles, permissions from JWT
    CORE->>CORE: Check DOCUMENT_UPLOAD permission
    CORE->>COREDB: Check lecturer belongs to course
    COREDB-->>CORE: Allowed

    CORE->>MINIO: Store uploaded file
    MINIO-->>CORE: Return storage path

    CORE->>COREDB: Save document metadata status=PENDING
    CORE->>KAFKA: Publish document.uploaded
    CORE-->>FE: Return documentId + status=PENDING

    KAFKA-->>PY: Consume document.uploaded
    PY->>MINIO: Read file
    PY->>PY: Extract text
    PY->>PY: Chunking
    PY->>PY: Generate embeddings
    PY->>AIDB: Save chunks and vectors

    alt Processing success
        PY->>KAFKA: Publish document.indexed
        KAFKA-->>CORE: Consume document.indexed
        CORE->>COREDB: Update status=COMPLETED
    else Processing failed
        PY->>KAFKA: Publish document.index_failed
        KAFKA-->>CORE: Consume document.index_failed
        CORE->>COREDB: Update status=FAILED
    end