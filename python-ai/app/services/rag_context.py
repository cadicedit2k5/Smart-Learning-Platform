from dataclasses import dataclass

from app.services.retrieval_service import RetrievedChunk


@dataclass(frozen=True, slots=True)
class RagContext:
    text: str
    sources: dict[str, RetrievedChunk]


def build_rag_context(chunks: list[RetrievedChunk]) -> RagContext:
    parts: list[str] = []
    sources: dict[str, RetrievedChunk] = {}

    for index, chunk in enumerate(chunks, start=1):
        label = f"S{index}"

        sources[label] = chunk

        parts.append(f"[{label}]\n"
                     f"{chunk.content}")

    return RagContext(text="\n\n".join(parts), sources=sources)