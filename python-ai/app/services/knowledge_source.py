import uuid
from dataclasses import dataclass
from enum import StrEnum


class KnowledgeSourceType(StrEnum):
    DOCUMENT = "DOCUMENT"
    TOPIC = "TOPIC"


@dataclass(frozen=True, slots=True)
class KnowledgeSource:
    course_id: uuid.UUID
    source_type: KnowledgeSourceType
    source_id: uuid.UUID
    source_version_id: uuid.UUID | None = None

    document_id: uuid.UUID | None = None
    document_version_id: uuid.UUID | None = None

    chapter_id: uuid.UUID | None = None
    topic_id: uuid.UUID | None = None