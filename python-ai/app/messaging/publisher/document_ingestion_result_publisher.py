from aiokafka import AIOKafkaProducer

from app.messaging.events.document import DocumentIngestionCompletedEvent, DocumentIngestionFailedEvent
from app.messaging.topics import DOCUMENT_INGESTION_COMPLETED, DOCUMENT_INGESTION_FAILED


class DocumentIngestionResultPublisher:

    def __init__(self, bootstrap_servers: str) -> None:
        self._producer = AIOKafkaProducer(bootstrap_servers=bootstrap_servers)

    async def start(self) -> None:
        await self._producer.start()

    async def stop(self) -> None:
        await self._producer.stop()

    async def publish_completed(self,
            event: DocumentIngestionCompletedEvent) -> None:
        payload = event.model_dump_json(by_alias=True).encode("utf-8")

        await self._producer.send_and_wait(
            DOCUMENT_INGESTION_COMPLETED,
            value=payload,
            key=str(
                event.document_version_id
            ).encode("utf-8"),
        )

    async def publish_failed(self,
            event: DocumentIngestionFailedEvent) -> None:
        payload = event.model_dump_json(by_alias=True).encode("utf-8")

        await self._producer.send_and_wait(
            DOCUMENT_INGESTION_FAILED,
            value=payload,
            key=str(
                event.document_version_id
            ).encode("utf-8"),
        )