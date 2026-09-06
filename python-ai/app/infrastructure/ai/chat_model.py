from langchain_core.language_models import BaseChatModel
from langchain_google_genai import ChatGoogleGenerativeAI

from app.configs.config import Settings


def create_chat_model(settings: Settings) -> BaseChatModel:
    if settings.llm_provider == "google":
        return ChatGoogleGenerativeAI(
            model=settings.llm_model,
            api_key=(settings.google_api_key.get_secret_value()),
            temperature=settings.llm_temperature,
            timeout=settings.llm_timeout_seconds,
            max_retries=settings.llm_max_retries,
        )

    raise ValueError(f"Unsupported LLM provider: {settings.llm_provider}")