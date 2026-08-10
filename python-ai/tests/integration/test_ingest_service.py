import uuid
from pathlib import Path

from google.genai.live import AsyncSession
from sqlalchemy import select, func

from app.configs.config import get_settings
from app.configs.database import AsyncSessionLocal
from app.infrastructure.ai.embeddings import create_embeddings, get_embedding_model_key
from app.infrastructure.documents.loader import DocumentLoader
from app.models import DocumentChunk, ChunkEmbedding
from app.repositories.chunk_repository import ChunkRepository
from app.services.ingestion_service import IngestionService


async def test_ingest_document():
    course_id = uuid.uuid4()
    document_id = uuid.uuid4()
    document_version_id = uuid.uuid4()

    settings = get_settings()
    loader = DocumentLoader()
    embeddings = create_embeddings(settings)

    async with AsyncSessionLocal() as session:
        service = IngestionService(
            session=session,
            loader=loader,
            embeddings=embeddings,
            settings=settings,
        )

        res = await service.ingest(file_path=Path("tests/resources/sample.txt"),
                                   course_id=course_id, document_id=document_id,
                                   document_version_id=document_version_id)

        assert res.chunk_count > 0
        assert res.model_key == get_embedding_model_key(settings)

        chunks = (
            await session.scalars(
                select(DocumentChunk)
                .where(DocumentChunk.course_id == course_id,
                    DocumentChunk.document_id == document_id)
                .order_by(DocumentChunk.chunk_index))).all()

        assert len(chunks) == res.chunk_count
        assert all(chunk.content.strip() for chunk in chunks)

        embedding_count = await session.scalar(select(func.count())
            .select_from(ChunkEmbedding)
            .join(DocumentChunk)
            .where(
                DocumentChunk.course_id == course_id,
                DocumentChunk.document_id == document_id))

        assert embedding_count == res.chunk_count

        repository = ChunkRepository(session)
        await repository.delete_document_chunks(
            course_id=course_id,
            document_id=document_id)
