import uuid

from app.configs.config import get_settings
from app.configs.database import AsyncSessionLocal
from app.infrastructure.ai.embeddings import create_embeddings
from app.repositories.chunk_repository import ChunkRepository
from app.services.retrieval_service import RetrievalService


async def test_real_retrieval():

    course_id = uuid.UUID(
        "965c545d-dcd8-48a6-88fe-d684b60e8ddc"
    )

    settings = get_settings()

    async with AsyncSessionLocal() as session:

        repository = ChunkRepository(
            session
        )

        embeddings = create_embeddings(
            settings
        )

        service = RetrievalService(
            embeddings=embeddings,
            repository=repository,
            settings=settings,
        )

        results = await service.retrieve(
            course_id=course_id,
            question=(
                "Dependency Injection là gì?"
            ),
        )

        for result in results:
            print()
            print(
                "distance:",
                result.distance,
            )
            print(
                "document:",
                result.document_id,
            )
            print(
                "locator:",
                result.source_locator,
            )
            print(
                result.content[:500],
            )