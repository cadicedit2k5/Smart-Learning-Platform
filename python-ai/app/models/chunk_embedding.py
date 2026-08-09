import uuid

from pgvector.sqlalchemy import VECTOR
from sqlalchemy import ForeignKey, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.configs.database import Base
from typing import TYPE_CHECKING

# Kiểm tra để tránh circle import
if TYPE_CHECKING:
    from app.models.document_chunk import DocumentChunk

class ChunkEmbedding(Base):
    __tablename__ = "chunk_embeddings"
    __table_args__ = {"schema": "ai_engine"}

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    chunk_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), 
                           ForeignKey("ai_engine.document_chunks.id",
                                      ondelete="CASCADE"),
                           primary_key=True)
    model_key: Mapped[str] = mapped_column(String(200), nullable=False)
    embedding: Mapped[list[float]] = mapped_column(VECTOR(), nullable=False)

    chunk: Mapped["DocumentChunk"] = relationship(
        back_populates="embeddings"
    )
