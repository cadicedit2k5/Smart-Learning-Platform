import pytest

from app.configs.config import get_settings
from app.infrastructure.ai.embeddings import create_embeddings


@pytest.mark.asyncio
async def test_embed_query():
    settings = get_settings()

    embeddings = create_embeddings(settings)

    vector = await embeddings.aembed_query(
        "Dependency Injection trong Spring là gì?"
    )

    assert len(vector) == settings.embedding_dimensions

@pytest.mark.asyncio
async def test_embed_documents():
    settings = get_settings()

    embeddings = create_embeddings(settings)

    vectors = await embeddings.aembed_documents(
        [
            "Dependency Injection giúp giảm coupling.",
            "Spring IoC Container quản lý dependency.",
        ]
    )

    assert len(vectors) == 2

    assert all(
        len(vector) == settings.embedding_dimensions
        for vector in vectors
    )