import asyncio
import shutil
import uuid
from pathlib import Path
from tempfile import TemporaryDirectory
from typing import Annotated

from fastapi import APIRouter, UploadFile, File, Form, HTTPException

from app.schemas.documents import DocumentIngestionResponse
from app.api.dependencies import IngestionServiceDep

router = APIRouter(
    prefix="/internal/documents",
    tags=["Documents"],
)

@router.post("/ingest", response_model=DocumentIngestionResponse)
async def ingest_document(file: Annotated[UploadFile, File()],
                          course_id: Annotated[uuid.UUID, Form()],
                          document_id: Annotated[uuid.UUID, Form()],
                          document_version_id: Annotated[uuid.UUID, Form()],
                          ingestion_service: IngestionServiceDep,
                          chapter_id: Annotated[uuid.UUID | None, Form()] = None,
                          topic_id: Annotated[uuid.UUID | None, Form()] = None):
    if not file.filename:
        raise HTTPException(
            status_code=400,
            detail="Uploaded file must have a filename",
        )
    suffix = Path(file.filename).suffix.lower()
    supported_extensions = {
        ".pdf",
        ".docx",
        ".txt",
    }
    if suffix not in supported_extensions:
        raise HTTPException(
            status_code=400,
            detail=(
                f"Unsupported document type: {suffix}"
            ),
        )

    with TemporaryDirectory() as temp_directory:
        temp_path = (Path(temp_directory) / f"upload{uuid.uuid4()}{suffix}")

        content = await file.read()
        temp_path.write_bytes(content)

        await file.seek(0)
        with temp_path.open("wb") as destination:
            await asyncio.to_thread(
                shutil.copyfileobj,
                file.file,
                destination,
            )

        result = await ingestion_service.ingest(
            file_path=temp_path,
            course_id=course_id,
            document_id=document_id,
            document_version_id=document_version_id,
            chapter_id=chapter_id,
            topic_id=topic_id,
        )

        return DocumentIngestionResponse(
            chunk_count=result.chunk_count,
            model_key=result.model_key,
        )