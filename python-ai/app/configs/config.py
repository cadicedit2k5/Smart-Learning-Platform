from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache

class Settings(BaseSettings):
    app_name: str = "Smart Learning AI Engine"
    database_url: str
    # chunk_size: int = 1000
    # chunk_overlap: int = 200

    google_api_key: SecretStr

    embedding_provider: str = "google"
    embedding_model: str = "gemini-embedding-2"
    embedding_dimensions: int = 768

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )


@lru_cache()
def get_settings() -> Settings:
    return Settings()