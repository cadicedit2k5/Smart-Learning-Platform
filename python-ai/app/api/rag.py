from fastapi import APIRouter

from app.schemas.rag import RagAnswer, RagAnswerRequest
from app.api.dependencies import RagServiceDep

router = APIRouter(
    prefix="/internal/rag",
    tags=["RAG"],
)

@router.post("/answer", response_model=RagAnswer)
async def answer_question(request: RagAnswerRequest, rag_service: RagServiceDep) -> RagAnswer:
    return await rag_service.answer(course_id=request.course_id, question=request.question, history=request.history)
