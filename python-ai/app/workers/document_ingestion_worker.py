import asyncio
import logging
import selectors

from app.configs.config import get_settings
from app.configs.database import AsyncSessionLocal
from app.infrastructure.ai.embeddings import create_embeddings
from app.infrastructure.documents.loader import DocumentLoader
from app.infrastructure.storage.minio_storage import MinioStorage
from app.messaging.consumers.document_ingestion_consumer import DocumentIngestionConsumer
from app.messaging.publisher.document_ingestion_result_publisher import DocumentIngestionResultPublisher
from app.services.document_ingestion_handler import DocumentIngestionHandler

logger = logging.getLogger(__name__)

async def main() -> None:

    settings = get_settings()

    storage = MinioStorage(settings)
    loader = DocumentLoader()
    embeddings = create_embeddings(settings)
    handler = DocumentIngestionHandler(
        session_factory=AsyncSessionLocal,
        storage=storage,
        loader=loader,
        embeddings=embeddings,
        settings=settings,
    )
    result_publisher = (
        DocumentIngestionResultPublisher(
            bootstrap_servers=(
                settings.kafka_bootstrap_servers
            )
        )
    )

    consumer = DocumentIngestionConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        handler=handler,
        result_publisher=result_publisher,
    )

    await result_publisher.start()
    await consumer.start()

    try:
        await consumer.consume()

    finally:
        await consumer.stop()
        await result_publisher.stop()

if __name__ == "__main__":

    logging.basicConfig(level=logging.INFO)

    try:
        asyncio.run(
            main(),
            loop_factory=lambda: asyncio.SelectorEventLoop(
                selectors.SelectSelector()
            ),
        )

    except KeyboardInterrupt:
        pass