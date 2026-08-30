import json
import logging

from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from app.messaging.events.topic import TopicKnowledgeIndexRequestedEvent
from app.messaging.topics import TOPIC_KNOWLEDGE_INDEX_REQUESTED
from app.services.topic_knowledge_handler import TopicKnowledgeHandler

logger = logging.getLogger(__name__)


class TopicKnowledgeConsumer:

    GROUP_ID = "ai-engine-topic-knowledge"

    def __init__(self, *, bootstrap_servers: str, handler: TopicKnowledgeHandler):
        self._consumer = AIOKafkaConsumer(
            TOPIC_KNOWLEDGE_INDEX_REQUESTED,
            bootstrap_servers=bootstrap_servers,
            group_id=self.GROUP_ID,
            enable_auto_commit=False,
            auto_offset_reset="earliest",
        )

        self._handler = handler

    async def start(self) -> None:
        await self._consumer.start()
        logger.info("Topic knowledge consumer started")

    async def stop(self) -> None:
        await self._consumer.stop()
        logger.info("Topic knowledge consumer stopped")

    async def consume(self) -> None:
        async for message in self._consumer:
            await self._handle_message(message.value)

    async def _handle_message(self, raw_value: bytes) -> None:
        try:
            payload = json.loads(raw_value.decode("utf-8"))
            event = TopicKnowledgeIndexRequestedEvent.model_validate(payload)
        except (UnicodeDecodeError, json.JSONDecodeError, ValidationError):
            logger.exception("Invalid topic knowledge event")
            raise

        try:
            await self._handler.handle(event)
        except Exception:
            logger.exception("Topic knowledge indexing failed: topic_id=%s", event.topic_id)
            raise

        await self._consumer.commit()