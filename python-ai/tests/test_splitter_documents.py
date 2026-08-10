# import pytest
# from langchain_core.documents import Document
#
# from app.infrastructure.documents.splitter import DocumentsSplitter
#
# @pytest.mark.asyncio
# def test_splitter_documents():
#     document = Document(
#         page_content=("Dependency Injection trong Spring. " * 100),
#         metadata={
#             "source": "spring.pdf"
#         },
#     )
#
#     splitter = DocumentsSplitter(
#         chunk_size=200,
#         chunk_overlap=50,
#     )
#
#     chunks = splitter.split([document])
#
#     assert len(chunks) > 1
#
#     assert all(
#         chunk.metadata["source"] == "spring.pdf"
#         for chunk in chunks
#     )
#
#     assert "start_index" in chunks[0].metadata