import uuid

from pydantic import BaseModel, Field, field_validator

from app.schemas.chat import ChatHistoryMessage


# Output của LLM
class RagOutputModel(BaseModel):
    answer: str = Field(
        min_length=1,
        description=(
            "Câu trả lời cho người học dựa trên context được cung cấp."
        ),
    )

    citation_labels: list[str] = Field(
        default_factory=list,
        description=(
            "Các source label thực sự hỗ trợ câu trả lời."
        ),
    )

# Output của app
class RagCitation(BaseModel):
    label: str

    chunk_id: uuid.UUID
    document_id: uuid.UUID
    document_version_id: uuid.UUID | None

    locator: dict | None = None

class RagAnswer(BaseModel):
    answer: str
    citations: list[RagCitation]

# Dành cho fast api
class RagAnswerRequest(BaseModel):
    course_id: uuid.UUID | None = None

    history: list[ChatHistoryMessage] = Field(
        default_factory=list,
    )

    question: str = Field(min_length=1)

    @field_validator("question")
    @classmethod
    def validate_question(
            cls,
            value: str,
    ) -> str:
        value = value.strip()

        if not value:
            raise ValueError("question must not be blank" )

        return value