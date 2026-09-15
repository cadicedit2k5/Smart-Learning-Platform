from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

import pytest
from langchain_core.messages import HumanMessage
from pydantic import ValidationError

from app.schemas.chat import ChatHistoryMessage
from app.schemas.course_preview import CoursePreviewContext, CoursePreviewOutputModel
from app.services.course_preview_service import CoursePreviewService


@pytest.mark.parametrize("as_model", [True, False])
async def test_course_preview_uses_course_structure_and_history(as_model):
    course = CoursePreviewContext.model_validate({
        "courseId": str(uuid4()), "title": "Spring Basics", "level": "BEGINNER",
        "description": "Learn Spring",
        "chapters": [{"chapterId": str(uuid4()), "title": "IoC", "orderIndex": 1,
                      "description": "Containers", "learningObjectives": "Understand injection",
                      "topics": [{"topicId": str(uuid4()), "title": "DI", "orderIndex": 1}]}],
    })
    model = MagicMock()
    output = {"answer": "This course introduces DI."}
    structured = model.with_structured_output.return_value
    structured.ainvoke = AsyncMock(return_value=CoursePreviewOutputModel(**output) if as_model else output)
    service = CoursePreviewService(chat_model=model)
    result = await service.answer(course=course, question="  Is this for beginners?  ",
                                  history=[ChatHistoryMessage(role="USER", content="I am new to Spring.")])
    assert result.answer == output["answer"]
    model.with_structured_output.assert_called_once_with(CoursePreviewOutputModel)
    messages = structured.ainvoke.call_args.args[0]
    assert isinstance(messages[1], HumanMessage)
    assert messages[1].content == "I am new to Spring."
    prompt = "\n".join(message.content for message in messages)
    for content in ("Spring Basics", "BEGINNER", "Learn Spring", "1. IoC", "Containers", "Understand injection", "- DI", "Is this for beginners?"):
        assert content in prompt


async def test_preview_handles_missing_optional_course_details():
    model = MagicMock()
    model.with_structured_output.return_value.ainvoke = AsyncMock(return_value={"answer": "Preview"})
    service = CoursePreviewService(chat_model=model)
    course = CoursePreviewContext(course_id=uuid4(), title="Spring", chapters=[])
    assert (await service.answer(course=course, question="Overview?", history=[])).answer == "Preview"
    messages = model.with_structured_output.return_value.ainvoke.call_args.args[0]
    prompt = "\n".join(message.content for message in messages)
    assert "Level: N/A" in prompt
    assert "Description: N/A" in prompt


async def test_preview_rejects_invalid_model_output():
    model = MagicMock()
    model.with_structured_output.return_value.ainvoke = AsyncMock(return_value={"answer": ""})
    service = CoursePreviewService(chat_model=model)
    with pytest.raises(ValidationError):
        await service.answer(course=CoursePreviewContext(course_id=uuid4(), title="Spring", chapters=[]),
                             question="Overview?", history=[])
