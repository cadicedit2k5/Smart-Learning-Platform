from langchain_core.embeddings import Embeddings
from langchain_google_genai import GoogleGenerativeAIEmbeddings

from app.configs.config import Settings


def create_embeddings(settings: Settings) -> Embeddings:
    if settings.embedding_provider == "google":
        return GoogleGenerativeAIEmbeddings(
            model=settings.embedding_model,
            google_api_key=settings.google_api_key,
            output_dimensionality=settings.embedding_dimensions
        )

    raise ValueError(
        f"Unsupported embedding provider: "
        f"{settings.embedding_provider}"
    )

def get_embedding_model_key(settings: Settings) -> str:
    # Cau truc tra model_key se co dang provider:model:dimensions
    return f"{settings.embedding_provider}:{settings.embedding_model}:{settings.embedding_dimensions}:"