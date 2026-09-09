from unittest.mock import Mock

import pytest
from langchain_core.documents import Document

from app.infrastructure.documents import loader as loader_module


@pytest.fixture
def loader(monkeypatch):
    monkeypatch.setattr(loader_module, "DocumentConverter", Mock())
    return loader_module.DocumentLoader()


@pytest.mark.parametrize("suffix", [".txt", ".pdf", ".docx", ".PDF"])
async def test_load_returns_docling_chunks(tmp_path, monkeypatch, loader, suffix):
    path = tmp_path / f"sample{suffix}"
    path.write_bytes(b"fixture content; parsing is delegated to Docling")
    documents = [Document(page_content="Dependency Injection", metadata={"dl_meta": {}})]
    docling = Mock()
    docling.return_value.load.return_value = documents
    monkeypatch.setattr(loader_module, "DoclingLoader", docling)

    assert await loader.load(path) == documents
    docling.assert_called_once_with(file_path=str(path), converter=loader.converter,
                                    export_type=loader_module.ExportType.DOC_CHUNKS)
    docling.return_value.load.assert_called_once_with()


async def test_missing_document(tmp_path, loader):
    with pytest.raises(FileNotFoundError, match="Document not found"):
        await loader.load(tmp_path / "missing.pdf")


async def test_unsupported_document(tmp_path, loader):
    path = tmp_path / "sample.csv"
    path.touch()
    with pytest.raises(ValueError, match="Unsupported document type"):
        await loader.load(path)


async def test_docling_error_propagates(tmp_path, loader, monkeypatch):
    path = tmp_path / "broken.pdf"
    path.touch()
    docling = Mock()
    docling.return_value.load.side_effect = RuntimeError("Conversion failed")
    monkeypatch.setattr(loader_module, "DoclingLoader", docling)
    with pytest.raises(RuntimeError, match="Conversion failed"):
        await loader.load(path)
