import asyncio
import json
import logging
import random

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

        max_retries = 5

        for attempt in range(max_retries):

            try:
                await self._handler.handle(event)

                await self._consumer.commit()

                return

            except Exception as exc:

                message = str(exc)

                retryable = (
                        "429" in message
                        or "RESOURCE_EXHAUSTED" in message
                        or "503" in message
                        or "UNAVAILABLE" in message
                )

                if not retryable:
                    logger.exception(
                        "Topic knowledge indexing failed: "
                        "topic_id=%s",
                        event.topic_id,
                    )
                    raise

                if attempt == max_retries - 1:
                    logger.exception(
                        "Topic knowledge indexing failed "
                        "after retries: topic_id=%s",
                        event.topic_id,
                    )
                    raise

                delay = min(
                    2 ** attempt + random.uniform(0, 1),
                    60,
                )

                logger.warning(
                    "Temporary embedding error. "
                    "topic_id=%s retry=%s/%s "
                    "waiting=%.1fs",
                    event.topic_id,
                    attempt + 1,
                    max_retries,
                    delay,
                )

                await asyncio.sleep(delay)