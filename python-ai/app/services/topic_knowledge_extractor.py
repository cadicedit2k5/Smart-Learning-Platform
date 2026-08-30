from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.infrastructure.documents.tiptap_text import extract_tiptap_text
from app.messaging.events.topic import TopicKnowledgeIndexRequestedEvent


class TopicKnowledgeExtractor:

    def __init__(self) -> None:
        self._splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=150)

    def extract(self, event: TopicKnowledgeIndexRequestedEvent) -> list[Document]:
        body = extract_tiptap_text(event.content or {}).strip()

        if not body:
            return []

        body_chunks = self._splitter.split_text(body)

        prefix = f"Chủ đề: {event.title.strip()}"

        if event.description and event.description.strip():
            prefix += f"\nMô tả: {event.description.strip()}"

        locator = {
            "type": "topic",
            "topicId": str(event.topic_id),
            "chapterId": str(event.chapter_id),
            "title": event.title,
        }

        return [
            Document(
                page_content=f"{prefix}\n\n{chunk}",
                metadata={"source_locator": locator},
            )
            for chunk in body_chunks
        ]