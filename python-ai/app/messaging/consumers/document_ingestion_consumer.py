import json
import logging
from datetime import datetime, timezone
from uuid import uuid4

from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from app.messaging.events.document import DocumentIngestionRequestedEvent, DocumentIngestionCompletedEvent, \
    DocumentIngestionFailedEvent
from app.messaging.publisher.document_ingestion_result_publisher import DocumentIngestionResultPublisher
from app.messaging.topics import DOCUMENT_INGESTION_REQUESTED
from app.services.document_ingestion_handler import DocumentIngestionHandler

logger = logging.getLogger(__name__)

class DocumentIngestionConsumer:

    GROUP_ID = "ai-engine-document-ingestion"

    def __init__(self,bootstrap_servers: str,
                 handler: DocumentIngestionHandler,
                 result_publisher: DocumentIngestionResultPublisher) -> None:
        self._consumer = AIOKafkaConsumer(
            DOCUMENT_INGESTION_REQUESTED,
            bootstrap_servers=bootstrap_servers,
            group_id=self.GROUP_ID,
            enable_auto_commit=False,
            auto_offset_reset="earliest"
        )
        self._handler = handler
        self._result_publisher = result_publisher

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

        try:
            result = await self._handler.handle(event)

        except Exception as exception:
            await self._handle_failure(
                event,
                exception)
            return
        logger.info(
            (
                "Document ingestion completed: "
                "event_id=%s, "
                "document_version_id=%s, "
                "chunk_count=%s, "
                "model_key=%s"
            ),
            event.event_id,
            event.document_version_id,
            result.chunk_count,
            result.model_key,
        )

        completed_event = (
            DocumentIngestionCompletedEvent(
                event_id=uuid4(),
                schema_version=1,
                occurred_at=datetime.now(timezone.utc),

                request_event_id=event.event_id,
                processing_job_id=(event.processing_job_id),
                document_version_id=(event.document_version_id),

                chunk_count=result.chunk_count,
                model_key=result.model_key,
            )
        )
        await self._result_publisher.publish_completed(completed_event)

        await self._consumer.commit()

    async def _handle_failure(
            self,
            event: DocumentIngestionRequestedEvent,
            exception: Exception,
    ) -> None:

        logger.exception(
            (
                "Document ingestion failed: "
                "event_id=%s, "
                "document_version_id=%s"
            ),
            event.event_id,
            event.document_version_id,
        )

        failed_event = (
            DocumentIngestionFailedEvent(
                event_id=uuid4(),
                schema_version=1,
                occurred_at=datetime.now(timezone.utc),

                request_event_id=event.event_id,
                processing_job_id=(event.processing_job_id),
                document_version_id=(event.document_version_id),

                error_type=type(exception).__name__,

                error_message=(str(exception)[:2000] or "Unknown ingestion error"),
            )
        )

        await self._result_publisher.publish_failed(failed_event)

        await self._consumer.commit()