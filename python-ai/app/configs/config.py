from pydantic_settings import BaseSettings, SettingsConfigDict 
from functools import lru_cache

class Settings(BaseSettings):
    app_name: str = "Smart Learning AI Engine"
    database_url: str

    model_config = SettingsConfigDict(
        env_file=".env",
        extra="ignore"
    )


@lru_cache()
def get_settings() -> Settings:
    return Settings()