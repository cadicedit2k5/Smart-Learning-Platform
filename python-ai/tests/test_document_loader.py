from pathlib import Path

import pytest

from app.infrastructure.documents.loader import DocumentLoader

@pytest.mark.asyncio
async def test_load_txt():
    loader = DocumentLoader()

    documents = await loader.load(
        Path("tests/resources/sample.txt")
    )

    assert len(documents) == 1
    assert "Dependency Injection" in documents[0].page_content

@pytest.mark.asyncio
async def test_load_pdf():
    loader = DocumentLoader()

    documents = await loader.load(
        Path("tests/resources/sample.pdf")
    )

    print(documents[0].page_content[:2000])
    print(documents[0].metadata)

    assert len(documents) > 0
    assert documents[0].page_content.strip()


@pytest.mark.asyncio
async def test_load_docx():
    loader = DocumentLoader()

    documents = await loader.load(
        Path("tests/resources/sample.docx")
    )

    assert len(documents) > 0
    assert documents[0].page_content.strip()