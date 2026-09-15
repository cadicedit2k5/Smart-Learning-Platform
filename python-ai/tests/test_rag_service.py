from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

import pytest
from langchain_core.messages import AIMessage, HumanMessage
from pydantic import ValidationError

from app.schemas.chat import ChatHistoryMessage
from app.schemas.rag import RagOutputModel
from app.services.rag_service import RagService
from app.services.retrieval_service import RetrievedChunk


def make_chunk(source_type, content):
    source_id = uuid4()
    return RetrievedChunk(chunk_id=uuid4(), source_type=source_type, source_id=source_id,
                          document_id=source_id if source_type == "DOCUMENT" else None,
                          document_version_id=uuid4() if source_type == "DOCUMENT" else None,
                          topic_id=source_id if source_type == "TOPIC" else None,
                          content=content, source_locator={"heading": content}, distance=0.1)


@pytest.fixture
def dependencies():
    retrieval = MagicMock(retrieve=AsyncMock(return_value=[]))
    model = MagicMock()
    structured = model.with_structured_output.return_value
    structured.ainvoke = AsyncMock(return_value={"answer": "Explanation", "citation_labels": []})
    service = RagService(retrieval_service=retrieval, chat_model=model)
    model.with_structured_output.assert_called_once_with(RagOutputModel)
    return service, retrieval, structured


@pytest.mark.parametrize("as_model", [True, False])
async def test_rag_normalizes_citations_and_omits_topic_and_unknown_labels(dependencies, as_model):
    service, retrieval, model = dependencies
    chunks = [make_chunk("DOCUMENT", "Document context"), make_chunk("TOPIC", "Topic context")]
    retrieval.retrieve.return_value = chunks
    output = {"answer": "Explanation", "citation_labels": [" [s1] ", "S1", "S2", "S99", ""]}
    model.ainvoke.return_value = RagOutputModel(**output) if as_model else output
    course_id = uuid4()
    history = [ChatHistoryMessage(role="USER", content="What is DI?"),
               ChatHistoryMessage(role="ASSISTANT", content="Dependency Injection.")]
    result = await service.answer(course_id=course_id, question="  Example?  ", history=history)
    retrieval.retrieve.assert_awaited_once_with(course_id=course_id, question=(
        "USER: What is DI?\nASSISTANT: Dependency Injection.\nCURRENT_USER_QUESTION: Example?"))
    assert result.answer == "Explanation"
    assert len(result.citations) == 1
    citation = result.citations[0]
    assert citation.label == "S1"
    assert citation.chunk_id == chunks[0].chunk_id
    assert citation.document_id == chunks[0].document_id
    assert citation.document_version_id == chunks[0].document_version_id
    assert citation.locator == chunks[0].source_locator
    messages = model.ainvoke.call_args.args[0]
    assert isinstance(messages[1], HumanMessage)
    assert messages[1].content == history[0].content
    assert isinstance(messages[2], AIMessage)
    assert messages[2].content == history[1].content
    assert "[S1]\nDocument context" in messages[-1].content
    assert "[S2]\nTopic context" in messages[-1].content
    assert "<current_question>\nExample?\n</current_question>" in messages[-1].content


async def test_retrieval_uses_last_four_messages_with_500_character_limit(dependencies):
    service, retrieval, model = dependencies
    history = [ChatHistoryMessage(role="USER", content=f"message-{i} " + "x" * 600) for i in range(6)]
    await service.answer(question="Continue", history=history)
    expected = "\n".join([f"USER: {message.content[:500]}" for message in history[-4:]]
                         + ["CURRENT_USER_QUESTION: Continue"])
    retrieval.retrieve.assert_awaited_once_with(course_id=None, question=expected)
    # The model still receives complete conversation messages.
    assert [message.content for message in model.ainvoke.call_args.args[0][1:-1]] == [
        message.content for message in history]


async def test_no_sources_still_answers_without_citations(dependencies):
    service, _, model = dependencies
    model.ainvoke.return_value = {"answer": "General explanation", "citation_labels": ["S1"]}
    result = await service.answer(question="DI?", history=[])
    assert result.answer == "General explanation"
    assert result.citations == []


async def test_blank_question_does_not_call_retrieval_or_model(dependencies):
    service, retrieval, model = dependencies
    with pytest.raises(ValueError):
        await service.answer(question=" \n ", history=[])
    retrieval.retrieve.assert_not_awaited()
    model.ainvoke.assert_not_awaited()


async def test_invalid_structured_output_is_rejected(dependencies):
    service, _, model = dependencies
    model.ainvoke.return_value = {"answer": ""}
    with pytest.raises(ValidationError):
        await service.answer(question="DI?", history=[])
