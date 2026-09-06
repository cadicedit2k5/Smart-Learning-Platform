from typing import Literal
from pydantic import BaseModel, Field


class ChatHistoryMessage(BaseModel):
    role: Literal["USER", "ASSISTANT"]

    content: str = Field(min_length=1)