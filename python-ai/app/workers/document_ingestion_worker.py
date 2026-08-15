import asyncio
import logging

from app.configs.config import get_settings
from app.messaging.consumers.document_ingestion_consumer import DocumentIngestionConsumer

logger = logging.getLogger(__name__)

async def main() -> None:

    settings = get_settings()

    consumer = DocumentIngestionConsumer(bootstrap_servers=(
            settings.kafka_bootstrap_servers)
    )

    await consumer.start()

    try:
        await consumer.consume()

    finally:
        await consumer.stop()

if __name__ == "__main__":

    logging.basicConfig(level=logging.INFO)

    try:
        asyncio.run(main())

    except KeyboardInterrupt:
        pass