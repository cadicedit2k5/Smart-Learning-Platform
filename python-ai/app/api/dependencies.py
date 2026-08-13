from functools import lru_cache
from typing import Annotated

from fastapi import Depends
from google.genai.live import AsyncSession
from langchain_core.embeddings import Embeddings
from langchain_core.language_models import BaseChatModel

from app.configs.config import Settings, get_settings
from app.configs.database import get_db_connection
from app.infrastructure.ai.chat_model import create_chat_model
from app.infrastructure.ai.embeddings import create_embeddings
from app.repositories.chunk_repository import ChunkRepository
from app.services.rag_service import RagService
from app.services.retrieval_service import RetrievalService

SettingsDep = Annotated[
    Settings,
    Depends(get_settings),
]

SessionDep = Annotated[
    AsyncSession,
    Depends(get_db_connection),
]

@lru_cache
def get_embeddings() -> Embeddings:
    return create_embeddings(get_settings())

EmbeddingsDep = Annotated[
    Embeddings,
    Depends(get_embeddings),
]

@lru_cache
def get_chat_model() -> BaseChatModel:
    return create_chat_model(get_settings())

ChatModelDep = Annotated[
    BaseChatModel,
    Depends(get_chat_model),
]

def get_chunk_repository(session: SessionDep) -> ChunkRepository:

    return ChunkRepository(session)

ChunkRepositoryDep = Annotated[
    ChunkRepository,
    Depends(get_chunk_repository),
]

def get_retrieval_service(
    embeddings: EmbeddingsDep,
    repository: ChunkRepositoryDep,
    settings: SettingsDep,
) -> RetrievalService:

    return RetrievalService(
        embeddings=embeddings,
        repository=repository,
        settings=settings,
    )

RetrievalServiceDep = Annotated[
    RetrievalService,
    Depends(get_retrieval_service),
]

def get_rag_service(
    retrieval_service: RetrievalServiceDep,
    chat_model: ChatModelDep,
) -> RagService:

    return RagService(
        retrieval_service=retrieval_service,
        chat_model=chat_model,
    )

RagServiceDep = Annotated[
    RagService,
    Depends(get_rag_service),
]