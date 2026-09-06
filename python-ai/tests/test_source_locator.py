from langchain_core.documents import Document

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