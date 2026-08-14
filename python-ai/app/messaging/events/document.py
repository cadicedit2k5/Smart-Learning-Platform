from datetime import datetime
from typing import Literal
from uuid import UUID

from app.messaging.event_model import EventModel


class DocumentIngestionRequestedEvent(EventModel):
    event_id: UUID
    schema_version: Literal[1]
    occurred_at: datetime

    processing_job_id: UUID

    course_id: UUID
    document_id: UUID
    document_version_id: UUID

    storage_bucket: str
    storage_key: str
    file_name: str
    mime_type: str