import uuid

from langchain_core.language_models import BaseChatModel

from app.prompts.rag import RAG_PROMPT
from app.schemas.chat import ChatHistoryMessage
from app.schemas.rag import RagOutputModel, RagAnswer, RagCitation
from app.services.rag_context import build_rag_context
from app.services.retrieval_service import RetrievalService


class RagService:
    def __init__(self, *, retrieval_service: RetrievalService, chat_model: BaseChatModel):
        self._retrieval_service = retrieval_service
        self._structured_model = chat_model.with_structured_output(RagOutputModel)

    async def answer(self, *, course_id: uuid.UUID | None = None, question: str,
                     history: list[ChatHistoryMessage]):
        question = question.strip()
        if not question:
            raise ValueError("Vui lòng cung cấp câu hỏi")

        retrieval_query = self._build_retrieval_query(history=history, question=question)

        retrieval_chunks = await self._retrieval_service.retrieve(course_id=course_id, question=retrieval_query)
        if not retrieval_chunks:
            search_scope = (
                "trong tài liệu của khóa học."
                if course_id is not None
                else "trong kho tài liệu."
            )
            return RagAnswer(
                answer=f"Không tìm thấy thông tin phù hợp {search_scope}",
                citations=[],
            )

        rag_context = build_rag_context(chunks=retrieval_chunks)
        messages = RAG_PROMPT.format_messages(question=question, context=rag_context.text)
        raw_output = await self._structured_model.ainvoke(messages)

        if isinstance(raw_output, RagOutputModel):
            model_ouput = raw_output
        else:
            model_ouput = RagOutputModel.model_validate(raw_output)

        citations: list[RagCitation] = []
        used_labels: set[str] = set()

        for raw_label in model_ouput.citation_labels:
            label = raw_label.strip().upper().removeprefix("[").removesuffix("]")
            if label in used_labels:
                continue

            source = rag_context.sources.get(label)
            if source is None:
                continue

            citations.append(RagCitation(
                    label=label,
                    chunk_id=source.chunk_id,
                    document_id=source.document_id,
                    document_version_id=source.document_version_id,
                    locator=source.source_locator))
            used_labels.add(label)

        return RagAnswer(answer=model_ouput.answer, citations=citations)

    def _build_retrieval_query(self, *,
            history: list[ChatHistoryMessage], question: str) -> str:

        recent_history = history[-4:]

        lines: list[str] = []

        for message in recent_history:
            content = message.content[:500]

            lines.append(f"{message.role}: {content}")

        lines.append(f"CURRENT_USER_QUESTION: {question}")

        return "\n".join(lines)








