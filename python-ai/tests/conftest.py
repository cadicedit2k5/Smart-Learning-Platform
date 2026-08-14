import asyncio
import selectors


def pytest_asyncio_loop_factories(config, item):
    return {
        "selector": lambda: asyncio.SelectorEventLoop(
            selectors.SelectSelector()
        )
    }