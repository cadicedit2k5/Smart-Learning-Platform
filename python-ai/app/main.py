import logging

from fastapi import FastAPI

from app.api.rag import router as rag_router
from app.api.document import router as document_router
from app.api.course_preview import router as course_preview_router
from app.configs.config import get_settings

logging.basicConfig(level=logging.INFO)

settings = get_settings()

app = FastAPI(
    title=settings.app_name
)
app.include_router(rag_router)
app.include_router(document_router)
app.include_router(course_preview_router)
@app.get("/health")
def health():
    return {"status": "UP"}