import json
import logging

from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from app.messaging.events.document import DocumentIngestionRequestedEvent
from app.messaging.topics import DOCUMENT_INGESTION_REQUESTED

logger = logging.getLogger(__name__)

class DocumentIngestionConsumer:

    GROUP_ID = "ai-engine-document-ingestion"

    def __init__(self,bootstrap_servers: str) -> None:
        self._consumer = AIOKafkaConsumer(
            DOCUMENT_INGESTION_REQUESTED,
            bootstrap_servers=bootstrap_servers,
            group_id=self.GROUP_ID,
            enable_auto_commit=False,
            auto_offset_reset="earliest"
        )

    async def start(self) -> None:
        await self._consumer.start()

        logger.info("Document ingestion consumer started")

    async def stop(self) -> None:
        await self._consumer.stop()

        logger.info("Document ingestion consumer stopped")

    async def consume(self) -> None:
        async for message in self._consumer:
            await self._handle_message(message.value)

    async def _handle_message(self,raw_value: bytes) -> None:

        try:
            payload = json.loads(raw_value.decode("utf-8"))

            event = (DocumentIngestionRequestedEvent.model_validate(payload))

        except (UnicodeDecodeError,
                json.JSONDecodeError,
                ValidationError):
            logger.exception("Invalid document ingestion event")

            raise

        logger.info(
            (
                "Received document ingestion event: "
                "event_id=%s, "
                "course_id=%s, "
                "document_id=%s, "
                "document_version_id=%s, "
                "file_name=%s"
            ),
            event.event_id,
            event.course_id,
            event.document_id,
            event.document_version_id,
            event.file_name,
        )

        await self._consumer.commit()