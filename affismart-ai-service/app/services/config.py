from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "AffiSmart AI Service"
    app_env: str = "dev"
    app_host: str = "0.0.0.0"
    app_port: int = 8000
    database_url: str = Field(
        default="postgresql+psycopg://postgres:postgres@localhost:5432/affismart_mall",
        alias="AI_DATABASE_URL",
    )
    model_refresh_seconds: int = Field(default=300, alias="AI_MODEL_REFRESH_SECONDS")
    event_lookback_days: int = Field(default=90, alias="AI_EVENT_LOOKBACK_DAYS")
    default_limit: int = Field(default=12, alias="AI_DEFAULT_LIMIT")
    max_limit: int = Field(default=24, alias="AI_MAX_LIMIT")
    popularity_fallback_limit: int = Field(default=100, alias="AI_POPULARITY_FALLBACK_LIMIT")

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        populate_by_name=True,
        extra="ignore",
    )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
