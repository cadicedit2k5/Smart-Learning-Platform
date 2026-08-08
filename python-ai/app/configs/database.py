from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase

from app.configs.config import get_settings

settings = get_settings()

engine = create_async_engine(settings.database_url, pool_pre_ping=True)

class Base(DeclarativeBase):
    pass

AsyncSessionLocal = async_sessionmaker(bind=engine,
                                       class_=AsyncSession,
                                       expire_on_commit=False)

async def get_db_connection():
    async with AsyncSessionLocal() as session:
        yield session