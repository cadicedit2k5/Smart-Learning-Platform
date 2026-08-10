import uuid

import pytest
from sqlalchemy import select

from app.configs.database import AsyncSessionLocal
from app.models import ChunkEmbedding, DocumentChunk
from app.repositories.chunk_repository import ChunkRepository

@pytest.mark.asyncio
async def test_replace_document_chunks():
    course_id = uuid.uuid4()
    document_id = uuid.uuid4()
    document_version_id = uuid.uuid4()

    async with AsyncSessionLocal() as session:
        repository = ChunkRepository(session)

        chunk = DocumentChunk(
            course_id=course_id,
            document_id=document_id,
            document_version_id=document_version_id,
            chunk_index=0,
            content="Dependency Injection trong Spring.",
            source_locator={
                "page": 1,
            },
        )

        chunk.embeddings.append(
            ChunkEmbedding(
                model_key="test:embedding:3",
                embedding=[
                    0.1,
                    0.2,
                    0.3,
                ],
            )
        )

        await repository.replace_document_chunks(
            course_id=course_id,
            document_id=document_id,
            document_version_id=document_version_id,
            chunks=[chunk],
        )

        await session.flush()
        old_chunk_id = chunk.id

        stored_chunk = await session.scalar(
            select(DocumentChunk)
            .where(
                DocumentChunk.course_id == course_id,
                DocumentChunk.document_id == document_id,
            )
        )

        embedding = await session.scalar(
            select(ChunkEmbedding)
            .where(
                ChunkEmbedding.chunk_id == chunk.id
            )
        )

        assert stored_chunk is not None
        assert stored_chunk.content == "Dependency Injection trong Spring."

        assert embedding is not None
        assert embedding.model_key == "test:embedding:3"

        replacement_chunk = DocumentChunk(
            course_id=course_id,
            document_id=document_id,
            document_version_id=document_version_id,
            chunk_index=0,
            content="Nội dung mới sau khi re-index.",
            source_locator={
                "page": 2,
            },
        )

        replacement_chunk.embeddings.append(
            ChunkEmbedding(
                model_key="test:embedding:3",
                embedding=[
                    0.4,
                    0.5,
                    0.6,
                ],
            )
        )

        await repository.replace_document_chunks(
            course_id=course_id,
            document_id=document_id,
            document_version_id=document_version_id,
            chunks=[replacement_chunk],
        )

        await session.flush()

        stored_chunks = (
            await session.scalars(
                select(DocumentChunk).where(
                    DocumentChunk.course_id == course_id,
                    DocumentChunk.document_id == document_id,
                )
            )
        ).all()

        assert len(stored_chunks) == 1
        assert (stored_chunks[0].content == "Nội dung mới sau khi re-index.")

        old_embedding = await session.scalar(
            select(ChunkEmbedding).where(
                ChunkEmbedding.chunk_id == old_chunk_id
            )
        )

        assert old_embedding is None

        new_embedding = await session.scalar(
            select(ChunkEmbedding).where(
                ChunkEmbedding.chunk_id
                == replacement_chunk.id
            )
        )

        assert new_embedding is not None
        assert new_embedding.model_key == "test:embedding:3"

        await session.rollback()