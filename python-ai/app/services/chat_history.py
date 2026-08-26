from langchain_core.messages import BaseMessage, HumanMessage, AIMessage

from app.schemas.chat import ChatHistoryMessage


def to_langchain_messages(history: list[ChatHistoryMessage]) -> list[BaseMessage]:

    messages: list[BaseMessage] = []

    for message in history:

        if message.role == "USER":
            messages.append(
                HumanMessage(content=message.content)
            )

        elif message.role == "ASSISTANT":
            messages.append(
                AIMessage(content=message.content)
            )

    return messages