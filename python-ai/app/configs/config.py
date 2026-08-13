from pathlib import Path

from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache

BASE_DIR = Path(__file__).resolve().parents[2]

class Settings(BaseSettings):
    app_name: str = "Smart Learning AI Engine"
    database_url: str
    # chunk_size: int = 1000
    # chunk_overlap: int = 200

    google_api_key: SecretStr

    embedding_provider: str = "google"
    embedding_model: str = "gemini-embedding-2"
    embedding_dimensions: int = 768
    retrieve_top_k: int = 5

    llm_provider: str = "google"
    llm_model: str = "gemini-2.5-flash"

    llm_temperature: float = 0.2
    llm_timeout_seconds: float = 60.0
    llm_max_retries: int = 2

    model_config = SettingsConfigDict(
        env_file= BASE_DIR / ".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )


@lru_cache()
def get_settings() -> Settings:
    return Settings()