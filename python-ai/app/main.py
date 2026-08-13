from fastapi import FastAPI

from app.api.rag import router as rag_router
from app.configs.config import get_settings

settings = get_settings()

app = FastAPI(
    title=settings.app_name
)
app.include_router(rag_router)
@app.get("/health")
def health():
    return {"status": "UP"}