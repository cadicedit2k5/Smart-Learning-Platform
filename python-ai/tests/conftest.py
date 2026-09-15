import asyncio
import os
import selectors
from pathlib import Path
from tempfile import TemporaryDirectory

import pytest


def pytest_addoption(parser):
    parser.addoption("--run-integration", action="store_true", help="Run PostgreSQL/Google API tests")


def pytest_configure(config):
    if config.option.basetemp is None:
        # Windows runs under different accounts/sandboxes can leave pytest's
        # shared system-temp directory inaccessible. Give each run its own root.
        temporary_directory = TemporaryDirectory(prefix=".pytest-tmp-", dir=config.rootpath)
        config.add_cleanup(temporary_directory.cleanup)
        config.option.basetemp = str(Path(temporary_directory.name) / "tmp")

    if config.getoption("--run-integration"):
        if not os.environ.get("TEST_DATABASE_URL"):
            raise pytest.UsageError("--run-integration requires TEST_DATABASE_URL pointing to a migrated test database")
        os.environ["DATABASE_URL"] = os.environ["TEST_DATABASE_URL"]
    else:
        # Models create an engine on import; unit tests must not use application credentials.
        os.environ.update(
            DATABASE_URL="postgresql+psycopg://test:test@localhost:5432/test",
            GOOGLE_API_KEY="unit-test-key", MINIO_ACCESS_KEY="unit-test", MINIO_SECRET_KEY="unit-test",
        )


def pytest_collection_modifyitems(config, items):
    for item in items:
        if "integration" in item.path.parts:
            item.add_marker(pytest.mark.integration)
            if not config.getoption("--run-integration"):
                item.add_marker(pytest.mark.skip(reason="Use --run-integration with TEST_DATABASE_URL"))


def pytest_asyncio_loop_factories(config, item):
    return {"selector": lambda: asyncio.SelectorEventLoop(selectors.SelectSelector())}


@pytest.fixture
def settings():
    from app.configs.config import Settings

    return Settings(_env_file=None, database_url="postgresql+psycopg://test:test@localhost/test",
                    minio_access_key="test", minio_secret_key="test", google_api_key="test",
                    embedding_provider="google", embedding_model="test-embedding",
                    embedding_dimensions=3, retrieve_top_k=5)
