from langchain_core.documents import Document
import pytest

from app.infrastructure.documents.source_locator import extract_source_locator


def test_extract_source_locator():

    document = Document(
        page_content="Dependency Injection...",
        metadata={
            "dl_meta": {
                "headings": [
                    "Spring Framework",
                    "Dependency Injection",
                ],
                "doc_items": [
                    {
                        "prov": [
                            {
                                "page_no": 12,
                            }
                        ]
                    },
                    {
                        "prov": [
                            {
                                "page_no": 13,
                            }
                        ]
                    },
                ],
            }
        },
    )

    locator = extract_source_locator(document)

    assert locator == {
        "pages": [12, 13],
        "heading": "Dependency Injection",
    }


@pytest.mark.parametrize("metadata", [{}, {"dl_meta": None}, {"dl_meta": "invalid"}, {"dl_meta": {}}])
def test_absent_or_invalid_metadata_has_no_locator(metadata):
    assert extract_source_locator(Document(page_content="Content", metadata=metadata)) is None


def test_pages_are_sorted_deduplicated_and_invalid_provenance_is_ignored():
    document = Document(page_content="Content", metadata={"dl_meta": {
        "doc_items": [None, {"prov": [None, {"page_no": "3"}, {"page_no": 3}, {"page_no": 1}, {"page_no": 3}]}]
    }})
    assert extract_source_locator(document) == {"pages": [1, 3]}
