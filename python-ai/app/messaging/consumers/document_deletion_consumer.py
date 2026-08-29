import json
import logging

from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from app.messaging.events.document import DocumentDeletionRequestedEvent
from app.messaging.topics import DOCUMENT_DELETION_REQUESTED
from app.services.document_deletion_handler import DocumentDeletionHandler

logger = logging.getLogger(__name__)


class DocumentDeletionConsumer:

    GROUP_ID = "ai-engine-document-deletion"

    def __init__(self,*,
        bootstrap_servers: str,
        handler: DocumentDeletionHandler) -> None:
        self._consumer = AIOKafkaConsumer(
            DOCUMENT_DELETION_REQUESTED,
            bootstrap_servers=bootstrap_servers,
            group_id=self.GROUP_ID,
            enable_auto_commit=False,
            auto_offset_reset="earliest",
        )
        self._handler = handler

    async def start(self) -> None:
        await self._consumer.start()
        logger.info("Document deletion consumer started")

    async def stop(self) -> None:
        await self._consumer.stop()
        logger.info("Document deletion consumer stopped")

    async def consume(self) -> None:
        async for message in self._consumer:
            await self._handle_message(message.value)

    async def _handle_message(self, raw_value: bytes) -> None:
        try:
            payload = json.loads(raw_value.decode("utf-8"))
            event = DocumentDeletionRequestedEvent.model_validate(payload)
        except (UnicodeDecodeError, json.JSONDecodeError, ValidationError):
            logger.exception("Invalid document deletion event")
            raise

        try:
            await self._handler.handle(event)
        except Exception:
            logger.exception(
                "Document deletion failed: event_id=%s, document_id=%s",
                event.event_id,
                event.document_id,
            )
            raise

        logger.info(
            "Document chunks deleted: event_id=%s, document_id=%s",
            event.event_id,
            event.document_id,
        )

        await self._consumer.commit()