from uuid import uuid4

import pytest

from app.infrastructure.documents.topic_content_loader import TopicContentLoader


def test_topic_content_and_locator_survive_splitting():
    topic_id, chapter_id = uuid4(), uuid4()
    documents = TopicContentLoader().load(
        topic_id=topic_id, chapter_id=chapter_id, title=" Spring ", description=" IoC basics ",
        content={"type": "doc", "content": [
            {"type": "heading", "content": [{"type": "text", "text": "Dependency Injection"}]},
            {"type": "paragraph", "content": [{"type": "text", "text": "Spring manages dependencies. " * 100}]},
        ]},
    )
    assert len(documents) > 1
    assert "Spring" in documents[0].page_content
    assert "IoC basics" in documents[0].page_content
    assert any("Dependency Injection" in doc.page_content for doc in documents)
    assert all(0 < len(doc.page_content) <= 1000 for doc in documents)
    assert all(doc.metadata["source_locator"] == {
        "type": "topic", "topicId": str(topic_id), "chapterId": str(chapter_id), "title": " Spring "
    } for doc in documents)


@pytest.mark.parametrize("content", [None, {}, {"type": "doc", "content": [None, "invalid"]}])
def test_topic_without_body_keeps_title(content):
    documents = TopicContentLoader().load(topic_id=uuid4(), chapter_id=uuid4(), title="Spring",
                                         description="  ", content=content)
    assert len(documents) == 1
    assert documents[0].page_content == "Chủ đề: Spring"
