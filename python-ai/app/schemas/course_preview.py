from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from app.schemas.chat import ChatHistoryMessage


class _CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="ignore",
    )


class CoursePreviewTopic(_CamelModel):
    topic_id: UUID
    title: str
    order_index: int
    estimated_minutes: int | None = None


class CoursePreviewChapter(_CamelModel):
    chapter_id: UUID
    title: str
    description: str | None = None
    learning_objectives: str | None = None
    order_index: int
    topics: list[CoursePreviewTopic]


class CoursePreviewContext(_CamelModel):
    course_id: UUID
    title: str
    description: str | None = None
    level: str | None = None
    chapters: list[CoursePreviewChapter]


class CoursePreviewAnswerRequest(BaseModel):
    course: CoursePreviewContext

    history: list[ChatHistoryMessage] = Field(
        default_factory=list,
    )

    question: str = Field(
        min_length=1,
    )


class CoursePreviewOutputModel(BaseModel):
    answer: str = Field(
        min_length=1,
    )


class CoursePreviewAnswer(BaseModel):
    answer: str