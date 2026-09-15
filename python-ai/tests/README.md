# Python AI tests

Run the local suite from the python-ai directory:

```powershell
.\.venv\Scripts\Activate.ps1
pytest tests
```

Each run uses a unique temporary directory under the project and removes it on
exit. This avoids Windows permission conflicts in the shared
`%TEMP%\pytest-of-<user>` directory. An explicit `--basetemp` is still respected.
If an old `.pytest_cache` directory is inaccessible, run with `-p no:cacheprovider`
to disable pytest's cache; this is separate from the temporary test files.

The local suite mocks external services and Docling conversion; it does not require
sample documents, PostgreSQL, Kafka, MinIO, or Google API access.
Integration tests are collected but skipped unless explicitly enabled.

Run database integration tests against a dedicated PostgreSQL database with pgvector
and the current Alembic migrations applied:

```powershell
$env:TEST_DATABASE_URL = "postgresql+psycopg://test:test@localhost:5432/smart_learning_test"
.\.venv\Scripts\python.exe -m pytest tests/integration --run-integration --ignore=tests/integration/test_embeddings.py -q -p no:cacheprovider
```

Database tests create their own records and roll back the outer transaction, including
commits performed by the ingestion service. Retrieval/RAG tests use deterministic
embedding and chat doubles while exercising the real repository.

To include live Google embedding tests, configure Google credentials/model settings
in the environment or .env and omit the --ignore option. These two tests call the
configured embedding API; they are not run by the default local suite.
