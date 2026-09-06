from typing import Any
from uuid import UUID

from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter


BLOCK_TYPES = {
    "doc",
    "paragraph",
    "heading",
    "blockquote",
    "listItem",
    "bulletList",
    "orderedList",
    "codeBlock",
}


def _extract_text(node: object) -> str:
    if isinstance(node, list):
        return "\n".join(filter(None, (_extract_text(item) for item in node)))

    if not isinstance(node, dict):
        return ""

    node_type = node.get("type")

    if node_type == "text":
        return str(node.get("text", ""))

    if node_type == "hardBreak":
        return "\n"

    children = node.get("content")
    if not isinstance(children, list):
        return ""

    parts = [text for child in children if (text := _extract_text(child)).strip()]
    return ("\n" if node_type in BLOCK_TYPES else " ").join(parts)


class TopicContentLoader:

    def __init__(self) -> None:
        self._splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=150)

    def load(self, *, topic_id: UUID, chapter_id: UUID, title: str, description: str | None, content: dict[str, Any] | None) -> list[Document]:
        body = _extract_text(content or {}).strip()

        parts = [f"Chủ đề: {title.strip()}"]

        if description and description.strip():
            parts.append(f"Mô tả: {description.strip()}")

        if body:
            parts.append(body)

        text = "\n\n".join(parts).strip()

        if not text:
            return []

        locator = {
            "type": "topic",
            "topicId": str(topic_id),
            "chapterId": str(chapter_id),
            "title": title,
        }

        return [
            Document(page_content=chunk, metadata={"source_locator": locator})
            for chunk in self._splitter.split_text(text)
        ]