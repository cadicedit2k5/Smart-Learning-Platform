import uuid

from langchain_core.language_models import BaseChatModel

from app.prompts.rag import RAG_PROMPT
from app.schemas.rag import RagOutputModel, RagAnswer, RagCitation
from app.services.rag_context import build_rag_context
from app.services.retrieval_service import RetrievalService


class RagService:
    def __init__(self, *, retrieval_service: RetrievalService, chat_model: BaseChatModel):
        self._retrieval_service = retrieval_service
        self._structured_model = chat_model.with_structured_output(RagOutputModel)

    async def answer(self, *, course_id: uuid.UUID, question: str):
        question = question.strip()
        if not question:
            raise ValueError("Vui lòng cung cấp câu hỏi")

        retrieval_chunks = await self._retrieval_service.retrieve(course_id=course_id, question=question)
        if not retrieval_chunks:
            return RagAnswer(
                answer=(
                    "Không tìm thấy thông tin phù hợp "
                    "trong tài liệu của khóa học."
                ),
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








