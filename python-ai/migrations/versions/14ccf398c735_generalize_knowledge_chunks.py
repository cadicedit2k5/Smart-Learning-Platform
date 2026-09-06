"""generalize knowledge chunks

Revision ID: 14ccf398c735
Revises: 8db4f95f78d4
Create Date: 2026-08-30 17:32:40.849113

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '14ccf398c735'
down_revision: Union[str, Sequence[str], None] = '8db4f95f78d4'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column('document_chunks', sa.Column('source_type', sa.String(length=30), nullable=True), schema='ai_engine')
    op.add_column('document_chunks', sa.Column('source_id', sa.UUID(), nullable=True), schema='ai_engine')
    op.add_column('document_chunks', sa.Column('source_version_id', sa.UUID(), nullable=True), schema='ai_engine')

    op.execute("""
        UPDATE ai_engine.document_chunks
        SET source_type = 'DOCUMENT',
            source_id = document_id,
            source_version_id = document_version_id
    """)

    op.alter_column('document_chunks', 'source_type', existing_type=sa.String(length=30), nullable=False, schema='ai_engine')
    op.alter_column('document_chunks', 'source_id', existing_type=sa.UUID(), nullable=False, schema='ai_engine')

    op.alter_column('document_chunks', 'document_id', existing_type=sa.UUID(), nullable=True, schema='ai_engine')
    op.alter_column('document_chunks', 'document_version_id', existing_type=sa.UUID(), nullable=True, schema='ai_engine')

    op.create_index('ix_document_chunks_source', 'document_chunks', ['course_id', 'source_type', 'source_id'], unique=False, schema='ai_engine')


def downgrade() -> None:
    op.execute("DELETE FROM ai_engine.document_chunks WHERE source_type <> 'DOCUMENT'")

    op.drop_index('ix_document_chunks_source', table_name='document_chunks', schema='ai_engine')

    op.alter_column('document_chunks', 'document_id', existing_type=sa.UUID(), nullable=False, schema='ai_engine')
    op.alter_column('document_chunks', 'document_version_id', existing_type=sa.UUID(), nullable=False, schema='ai_engine')

    op.drop_column('document_chunks', 'source_version_id', schema='ai_engine')
    op.drop_column('document_chunks', 'source_id', schema='ai_engine')
    op.drop_column('document_chunks', 'source_type', schema='ai_engine')
