import asyncio
from pathlib import Path

from docling.datamodel.base_models import InputFormat
from docling.datamodel.object_detection_engine_options import TransformersObjectDetectionEngineOptions
from docling.datamodel.pipeline_options import PdfPipelineOptions, LayoutObjectDetectionOptions
from docling.document_converter import DocumentConverter, PdfFormatOption
from langchain_core.documents import Document
from langchain_docling import DoclingLoader
from langchain_docling.loader import ExportType

class DocumentLoader:

    def __init__(self):
        # Disable compile model to compatible with the env.
        layout_options = LayoutObjectDetectionOptions(
            engine_options=TransformersObjectDetectionEngineOptions(
                compile_model=False,
            ),
        )

        pdf_options = PdfPipelineOptions(
            layout_options=layout_options,
        )

        self.converter = DocumentConverter(
            format_options={
                InputFormat.PDF: PdfFormatOption(
                    pipeline_options=pdf_options,
                ),
            },
        )
        # Không cho quá nhiều PDF chạy đồng thời trong cùng process.
        self._docling_slots = asyncio.Semaphore(1)

    async def load(self, file_path: Path) -> list[Document]:
        if not file_path.is_file():
            raise FileNotFoundError(
                f"Document not found: {file_path}"
            )
        
        # File txt về bản chất đã là text rồi nên không cần xử lý
        suffix = file_path.suffix.lower()
        if suffix == ".txt":
            return self._load_text(file_path)

        if suffix in {".pdf", ".docx"}:
            return await asyncio.to_thread(
                self._load_with_docling,
                file_path,
            )

        raise ValueError(
            f"Unsupported document type: {suffix}"
        )

    def _load_with_docling(
        self,
        file_path: Path,
    ) -> list[Document]:
        loader = DoclingLoader(
            file_path=str(file_path),
            converter=self.converter,
            export_type=ExportType.MARKDOWN,
        )

        return loader.load()

    def _load_text(self, file_path: Path) -> list[Document]:
        content = file_path.read_text(
            encoding="utf-8"
        )

        return [
            Document(
                page_content=content,
                metadata={
                    "source": str(file_path),
                },
            )
        ]
