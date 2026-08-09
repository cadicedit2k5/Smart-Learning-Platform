import uuid

from sqlalchemy import Integer, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.postgresql import UUID, JSONB

from app.configs.database import Base

from typing import TYPE_CHECKING
if TYPE_CHECKING:
    from app.models.chunk_embedding import ChunkEmbedding

class DocumentChunk(Base):
    __tablename__ = "document_chunks"
    __table_args__ = {"schema": "ai_engine"}

    id : Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True),primary_key=True, default=uuid.uuid4)

    course_id : Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
    document_id : Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
    document_version_id : Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
    chapter_id : Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)
    topic_id : Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False)

    chunk_index : Mapped[int] = mapped_column(Integer, nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    source_locator: Mapped[dict] = mapped_column(JSONB, nullable=True)

    embeddings: Mapped[list["ChunkEmbedding"]] = relationship("ChunkEmbedding", 
                backref="chunk", cascade="all, delete-orphan")
