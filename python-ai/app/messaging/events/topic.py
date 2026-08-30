from datetime import datetime
from typing import Any, Literal
from uuid import UUID

from app.messaging.event_model import EventModel


class TopicKnowledgeIndexRequestedEvent(EventModel):
    event_id: UUID
    schema_version: Literal[1]
    occurred_at: datetime

    course_id: UUID
    chapter_id: UUID
    topic_id: UUID

    title: str
    description: str | None = None
    content: dict[str, Any] | None = None

    operation: Literal["UPSERT", "DELETE"]