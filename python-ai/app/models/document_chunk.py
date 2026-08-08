import uuid

from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.dialects.postgresql import UUID

from app.configs.database import Base

class DocumentChunk(Base):
    __tablename__ = "document_chunks"
    __table_args__ = {"schema": "ai_engine"}

    id = Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True),primary_key=True, default=uuid.uuid4)
    course_id = Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
    document_id = Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)