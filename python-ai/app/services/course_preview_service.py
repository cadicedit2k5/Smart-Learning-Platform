from langchain_core.language_models import BaseChatModel

from app.prompts.course_preview import COURSE_PREVIEW_PROMPT
from app.schemas.chat import ChatHistoryMessage
from app.schemas.course_preview import CoursePreviewOutputModel, CoursePreviewContext, CoursePreviewAnswer
from app.services.chat_history import to_langchain_messages


class CoursePreviewService:

    def __init__(self, *, chat_model: BaseChatModel):
        self._structured_model = chat_model.with_structured_output(CoursePreviewOutputModel)

    async def answer(self, *,
        course: CoursePreviewContext,
        question: str,
        history: list[ChatHistoryMessage]
    ) -> CoursePreviewAnswer:

        context = self._build_context(course)

        messages = COURSE_PREVIEW_PROMPT.format_messages(
                course_context=context,
                history=to_langchain_messages(history),
                question=question.strip(),
        )

        raw_output = await self._structured_model.ainvoke(messages)

        if isinstance(raw_output, CoursePreviewOutputModel):
            output = raw_output
        else:
            output = CoursePreviewOutputModel.model_validate(raw_output)

        return CoursePreviewAnswer(answer=output.answer)

    def _build_context(self, course: CoursePreviewContext) -> str:

        lines = [
            f"Course: {course.title}",
            f"Level: {course.level or 'N/A'}",
            f"Description: {course.description or 'N/A'}",
            "",
            "COURSE STRUCTURE:",
        ]

        for chapter in course.chapters:

            lines.append(f"{chapter.order_index}. {chapter.title}")

            if chapter.description:
                lines.append(f"   Description: {chapter.description}")

            if chapter.learning_objectives:
                lines.append(f"   Learning objectives: {chapter.learning_objectives}")

            for topic in chapter.topics:
                lines.append(f"   - {topic.title}")

        return "\n".join(lines)