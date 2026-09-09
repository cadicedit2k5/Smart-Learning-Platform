from uuid import uuid4

import pytest
from sqlalchemy import select

from app.models import ChunkEmbedding, DocumentChunk
from app.repositories.chunk_repository import ChunkRepository


def make_chunk(course_id, source_id, source_type="DOCUMENT", vector=None, model_key="test:embedding:3"):
    chunk = DocumentChunk(course_id=course_id, source_type=source_type, source_id=source_id,
                          document_id=source_id if source_type == "DOCUMENT" else None,
                          topic_id=source_id if source_type == "TOPIC" else None,
                          chunk_index=0, content=f"{source_type} content")
    chunk.embeddings.append(ChunkEmbedding(model_key=model_key, embedding=vector or [1., 0., 0.]))
    return chunk


@pytest.mark.parametrize("source_type", ["DOCUMENT", "TOPIC"])
async def test_replace_and_delete_only_matching_source(db_session, source_type):
    repository = ChunkRepository(db_session)
    course_id, source_id = uuid4(), uuid4()
    old = make_chunk(course_id, source_id, source_type)
    unrelated = [
        make_chunk(uuid4(), source_id, source_type),
        make_chunk(course_id, uuid4(), source_type),
        make_chunk(course_id, source_id, "TOPIC" if source_type == "DOCUMENT" else "DOCUMENT"),
    ]
    db_session.add_all([old, *unrelated])
    await db_session.flush()
    old_id = old.id
    replacement = make_chunk(course_id, source_id, source_type)
    replacement.content = "Replacement"
    await repository.replace_source_chunks(course_id=course_id, source_type=source_type,
                                            source_id=source_id, chunks=[replacement])
    await db_session.flush()
    ids = [old_id, replacement.id, *(chunk.id for chunk in unrelated)]
    stored = (await db_session.scalars(select(DocumentChunk).where(DocumentChunk.id.in_(ids)))).all()
    assert {chunk.id for chunk in stored} == {replacement.id, *(chunk.id for chunk in unrelated)}
    assert await db_session.scalar(select(ChunkEmbedding).where(ChunkEmbedding.chunk_id == old_id)) is None
    assert await db_session.scalar(select(ChunkEmbedding).where(ChunkEmbedding.chunk_id == replacement.id)) is not None

    await repository.delete_source_chunks(course_id=course_id, source_type=source_type, source_id=source_id)
    await db_session.flush()
    stored = (await db_session.scalars(select(DocumentChunk).where(DocumentChunk.id.in_(ids)))).all()
    assert {chunk.id for chunk in stored} == {chunk.id for chunk in unrelated}
    assert await db_session.scalar(select(ChunkEmbedding).where(ChunkEmbedding.chunk_id == replacement.id)) is None


@pytest.mark.parametrize("scoped", [True, False])
async def test_similarity_search_filters_model_course_and_orders_distance(db_session, scoped):
    repository = ChunkRepository(db_session)
    course_id = uuid4()
    model_key = f"test:{uuid4()}:3"
    relevant = make_chunk(course_id, uuid4(), vector=[0.9, 0.1, 0.], model_key=model_key)
    other = make_chunk(course_id, uuid4(), "TOPIC", vector=[0., 1., 0.], model_key=model_key)
    foreign = make_chunk(uuid4(), uuid4(), vector=[1., 0., 0.], model_key=model_key)
    wrong_model = make_chunk(course_id, uuid4(), model_key="wrong-model")
    db_session.add_all([relevant, other, foreign, wrong_model])
    await db_session.flush()
    results = await repository.similarity_search(course_id=course_id if scoped else None,
                                                  model_key=model_key, query_vector=[1., 0., 0.], top_k=2)
    assert [result.chunk.id for result in results] == (
        [relevant.id, other.id] if scoped else [foreign.id, relevant.id])
    assert results[0].distance <= results[1].distance
