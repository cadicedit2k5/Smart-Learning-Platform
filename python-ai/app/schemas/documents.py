from pydantic import BaseModel


class DocumentIngestionResponse(BaseModel):
    chunk_count: int
    model_key: str