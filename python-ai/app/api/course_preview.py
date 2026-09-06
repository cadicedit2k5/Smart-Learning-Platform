from fastapi import APIRouter

from app.api.dependencies import CoursePreviewServiceDep
from app.schemas.course_preview import CoursePreviewAnswer, CoursePreviewAnswerRequest

router = APIRouter(
    prefix="/internal/course-preview",
    tags=["Course Preview"],
)


@router.post("/answer", response_model=CoursePreviewAnswer)
async def answer_course_preview(
    request: CoursePreviewAnswerRequest,
    service: CoursePreviewServiceDep
) -> CoursePreviewAnswer:

    return await service.answer(
        course=request.course,
        history=request.history,
        question=request.question)