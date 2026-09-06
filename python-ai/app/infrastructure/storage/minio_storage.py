import asyncio
from pathlib import Path

from minio import Minio

from app.configs.config import Settings


class MinioStorage:

    def __init__(self,settings: Settings) -> None:
        self._client = Minio(
            endpoint=settings.minio_endpoint,
            access_key=settings.minio_access_key,
            secret_key=(settings.minio_secret_key.get_secret_value()),
            secure=settings.minio_secure)

    async def download_to_file(self, *, bucket: str, object_name: str, destination: Path) -> None:
        await asyncio.to_thread(
            self._client.fget_object,
            bucket,
            object_name,
            str(destination),
        )