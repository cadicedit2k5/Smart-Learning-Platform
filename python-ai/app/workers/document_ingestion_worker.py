import asyncio
import logging
import selectors

from app.configs.config import get_settings
from app.configs.database import AsyncSessionLocal
from app.infrastructure.ai.embeddings import create_embeddings
from app.infrastructure.documents.loader import DocumentLoader
from app.infrastructure.documents.topic_content_loader import TopicContentLoader
from app.infrastructure.storage.minio_storage import MinioStorage
from app.messaging.consumers.document_deletion_consumer import DocumentDeletionConsumer
from app.messaging.consumers.document_ingestion_consumer import DocumentIngestionConsumer
from app.messaging.consumers.topic_knowledge_consumer import TopicKnowledgeConsumer
from app.messaging.publisher.document_ingestion_result_publisher import DocumentIngestionResultPublisher
from app.services.document_deletion_handler import DocumentDeletionHandler
from app.services.document_ingestion_handler import DocumentIngestionHandler
from app.services.topic_knowledge_handler import TopicKnowledgeHandler

logger = logging.getLogger(__name__)

async def main() -> None:

    settings = get_settings()

    storage = MinioStorage(settings)
    loader = DocumentLoader()
    embeddings = create_embeddings(settings)

    topic_loader = TopicContentLoader()

    topic_handler = TopicKnowledgeHandler(
        session_factory=AsyncSessionLocal,
        embeddings=embeddings,
        loader=topic_loader,
        settings=settings,
    )

    topic_consumer = TopicKnowledgeConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        handler=topic_handler,
    )

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

    deletion_handler = DocumentDeletionHandler(
        session_factory=AsyncSessionLocal,
    )

    deletion_consumer = DocumentDeletionConsumer(
        bootstrap_servers=settings.kafka_bootstrap_servers,
        handler=deletion_handler,
    )

    await result_publisher.start()
    consumers = [
        consumer,
        deletion_consumer,
        topic_consumer,
    ]

    for item in consumers:
        await item.start()

    try:
        await asyncio.gather(*(item.consume() for item in consumers))

    finally:
        for item in reversed(consumers):
            await item.stop()

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